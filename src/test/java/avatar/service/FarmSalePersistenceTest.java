package avatar.service;

import avatar.constants.Cmd;
import avatar.db.DbManager;
import avatar.model.User;
import avatar.network.Message;
import avatar.message.FarmMsgHandler;
import avatar.Farm.NongSanDacBiet;
import java.nio.file.*;
import java.sql.*;
import java.util.concurrent.*;
import org.junit.*;
import static org.junit.Assert.*;

/** All prices here are synthetic test-only values, never production prices. */
public class FarmSalePersistenceTest {
    private Path catalog;
    private DbManager db;
    @Before public void setup() throws Exception {
        Assume.assumeTrue(Boolean.getBoolean("avatar.test.isolatedSalesDb"));
        db = DbManager.getInstance(); db.start();
        try (Connection c = db.getConnection()) {
            assertEquals("avatar2d_sales_test", c.getCatalog());
            c.createStatement().execute("DROP TABLE IF EXISTS farm_data");
            c.createStatement().execute("DROP TABLE IF EXISTS players");
            c.createStatement().execute("CREATE TABLE players (user_id INT PRIMARY KEY, xu BIGINT NOT NULL CHECK (xu < 1000000)) ENGINE=InnoDB");
            c.createStatement().execute("CREATE TABLE farm_data (user_id INT PRIMARY KEY, land_data LONGTEXT, animal_data LONGTEXT, hatgiong LONGTEXT, phanbon LONGTEXT, nongsan LONGTEXT, nongsandacbiet LONGTEXT, starfruit_quantity INT DEFAULT 180, starfruit_last_harvest_ms BIGINT DEFAULT 0) ENGINE=InnoDB");
            c.createStatement().execute("INSERT INTO players VALUES (9001,100)");
        }
        catalog = Files.createTempFile("synthetic-farm-prices", ".properties");
        Files.writeString(catalog, "enabled=true\napproval=SYNTHETIC_TEST_ONLY\nprice.215=7\n");
        System.setProperty("avatar.farm.sales.catalog", catalog.toString());
    }
    @After public void cleanup() throws Exception {
        if (catalog != null) { System.clearProperty("avatar.farm.sales.catalog"); Files.deleteIfExists(catalog); }
        if (db != null) db.shutdown();
    }
    private void stock(User user) throws Exception {
        user.setId(9001); user.setXu(100);
        user.NongSanDacBiet.add(new NongSanDacBiet(215,20)); user.saveFarmData(9001);
    }
    private Message sale(int product) throws Exception {
        Message m = new Message(Cmd.SELL_ITEM); m.writer().writeShort(product);
        return new Message(Cmd.SELL_ITEM,m.getData());
    }
    private void balance(int expected) throws Exception {
        try (Connection c = db.getConnection(); ResultSet r = c.createStatement().executeQuery("SELECT xu FROM players WHERE user_id=9001")) {
            assertTrue(r.next()); assertEquals(expected,r.getLong(1));
        }
    }
    @Test public void nativeSaleCommitsInventoryAndMoneyAndRejectsReplayAfterReload() throws Exception {
        GameplayFeedbackTest.withSession(s -> {
            stock(s.user); FarmMsgHandler handler = new FarmMsgHandler(s);
            handler.onMessage(sale(215));
            assertEquals(Cmd.SELL_ITEM,s.messages.get(0).getCommand());
            Message response = new Message(Cmd.SELL_ITEM,s.messages.get(0).getData());
            assertEquals(140,response.reader().readInt()); assertEquals(240,response.reader().readInt());
            assertEquals(215,response.reader().readShort()); assertEquals(0,response.reader().available());
            assertEquals(240,s.user.getXu()); balance(240);
            User reloaded = new User(); reloaded.setId(9001); reloaded.loadFarmData(9001);
            assertEquals(0,reloaded.NongSanDacBiet.get(0).getSoluong());
            reloaded.setXu(240); s.user = reloaded; reloaded.setSession(s);
            s.messages.clear(); handler.onMessage(sale(215));
            assertEquals(Cmd.SET_MONEY_ERROR,s.messages.get(0).getCommand()); balance(240);
        });
    }
    @Test public void concurrentDuplicatePacketsCreditOnlyOnce() throws Exception {
        GameplayFeedbackTest.withSession(s -> {
            stock(s.user);
            ExecutorService workers = Executors.newFixedThreadPool(5);
            try {
                java.util.List<Future<?>> futures = new java.util.ArrayList<>();
                for (int i=0;i<10;i++) futures.add(workers.submit(() -> {
                    try { new FarmService(s).sellFarmitm(s.user,sale(215)); }
                    catch (Exception e) { throw new RuntimeException(e); }
                }));
                for (Future<?> f : futures) f.get(10,TimeUnit.SECONDS);
                balance(240); assertEquals(240,s.user.getXu());
                assertEquals(0,s.user.NongSanDacBiet.get(0).getSoluong());
            } finally { workers.shutdownNow(); }
        });
    }
    @Test public void databaseFailureRollsBackInventoryAndLeavesMemoryUnchanged() throws Exception {
        GameplayFeedbackTest.withSession(s -> {
            stock(s.user); Files.writeString(catalog,"enabled=true\napproval=SYNTHETIC_TEST_ONLY\nprice.215=50000\n");
            new FarmMsgHandler(s).onMessage(sale(215));
            assertEquals(Cmd.SET_MONEY_ERROR,s.messages.get(0).getCommand()); balance(100);
            assertEquals(100,s.user.getXu()); assertEquals(20,s.user.NongSanDacBiet.get(0).getSoluong());
            User reloaded = new User(); reloaded.setId(9001); reloaded.loadFarmData(9001);
            assertEquals(20,reloaded.NongSanDacBiet.get(0).getSoluong());
        });
    }
    @Test public void overflowMissingPriceAndUnownedProductCannotChangeState() throws Exception {
        GameplayFeedbackTest.withSession(s -> {
            stock(s.user);
            for (int product : new int[]{7,216,-1}) {
                new FarmMsgHandler(s).onMessage(sale(product)); balance(100);
            }
            Files.writeString(catalog,"enabled=true\napproval=SYNTHETIC_TEST_ONLY\nprice.215=2147483647\n");
            new FarmMsgHandler(s).onMessage(sale(215)); balance(100);
            assertEquals(20,s.user.NongSanDacBiet.get(0).getSoluong());
        });
    }
    @Test public void malformedPersistedQuantityCannotBeSold() throws Exception {
        GameplayFeedbackTest.withSession(s -> {
            stock(s.user);
            try (Connection c = db.getConnection()) {
                c.createStatement().executeUpdate("UPDATE farm_data SET nongsandacbiet='[{\"id\":215,\"soluong\":20.5}]' WHERE user_id=9001");
            }
            new FarmMsgHandler(s).onMessage(sale(215));
            balance(100); assertEquals(20,s.user.NongSanDacBiet.get(0).getSoluong());
        });
    }

    @Test public void separateStaleUsersCannotBothCreditTheSameInventory() throws Exception {
        GameplayFeedbackTest.withSession(s -> {
            stock(s.user);
            User second = new User(); second.setId(9001); second.setXu(100); second.loadFarmData(9001);
            ExecutorService workers = Executors.newFixedThreadPool(2);
            try {
                java.util.List<Future<Boolean>> futures = new java.util.ArrayList<>();
                for (User user : new User[]{s.user,second}) futures.add(workers.submit(() -> {
                    try { user.sellFarmProduct(215,7); return true; }
                    catch (IllegalArgumentException stale) { return false; }
                }));
                int winners = 0;
                for (Future<Boolean> future : futures) if (future.get(10,TimeUnit.SECONDS)) winners++;
                assertEquals(1,winners); balance(240);
            } finally { workers.shutdownNow(); }
        });
    }

    @Test public void ordinaryProductUsesOnlyItsOwnedStock() throws Exception {
        GameplayFeedbackTest.withSession(s -> {
            stock(s.user); s.user.NongSan.add(new avatar.Farm.NongSan(7,3)); s.user.saveFarmData(9001);
            Files.writeString(catalog,"enabled=true\napproval=SYNTHETIC_TEST_ONLY\nprice.7=2\n");
            new FarmMsgHandler(s).onMessage(sale(7)); balance(106);
            assertEquals(0,s.user.NongSan.get(0).getSoluong());
            assertEquals(20,s.user.NongSanDacBiet.get(0).getSoluong());
            User reloaded = new User(); reloaded.setId(9001); reloaded.loadFarmData(9001);
            assertEquals(0,reloaded.NongSan.get(0).getSoluong());
            assertEquals(20,reloaded.NongSanDacBiet.get(0).getSoluong());
        });
    }

}
