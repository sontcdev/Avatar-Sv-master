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
    private final java.util.Set<Path> existingConflicts = new java.util.HashSet<>();
    @Before public void setup() throws Exception {
        Assume.assumeTrue(Boolean.getBoolean("avatar.test.isolatedSalesDb"));
        Path conflictDir=Path.of("logs/persistence-conflicts");
        if (Files.isDirectory(conflictDir)) try (java.util.stream.Stream<Path> paths=Files.list(conflictDir)) { paths.forEach(existingConflicts::add); }
        db = DbManager.getInstance(); db.start();
        try (Connection c = db.getConnection()) {
            assertEquals("avatar2d_sales_test", c.getCatalog());
            c.createStatement().execute("DROP TABLE IF EXISTS farm_data");
            c.createStatement().execute("DROP TABLE IF EXISTS players");
            c.createStatement().execute("CREATE TABLE players (user_id INT PRIMARY KEY, xu BIGINT NOT NULL CHECK (xu < 1000000), persistence_revision BIGINT NOT NULL DEFAULT 0) ENGINE=InnoDB");
            c.createStatement().execute("CREATE TABLE farm_data (user_id INT PRIMARY KEY, land_data LONGTEXT, animal_data LONGTEXT, hatgiong LONGTEXT, phanbon LONGTEXT, nongsan LONGTEXT, nongsandacbiet LONGTEXT, cooking_recipe INT DEFAULT 0, cooking_ready_ms BIGINT DEFAULT 0, cooking_output_id INT DEFAULT 0, starfruit_quantity INT DEFAULT 180, starfruit_last_harvest_ms BIGINT DEFAULT 0, persistence_revision BIGINT NOT NULL DEFAULT 0) ENGINE=InnoDB");
            c.createStatement().execute("INSERT INTO players (user_id,xu) VALUES (9001,100)");
        }
        catalog = Files.createTempFile("synthetic-farm-prices", ".properties");
        Files.writeString(catalog, "enabled=true\napproval=SYNTHETIC_TEST_ONLY\nprice.215=7\n");
        System.setProperty("avatar.farm.sales.catalog", catalog.toString());
    }
    @After public void cleanup() throws Exception {
        if (catalog != null) { System.clearProperty("avatar.farm.sales.catalog"); Files.deleteIfExists(catalog); }
        if (db != null) db.shutdown();
        Path conflictDir=Path.of("logs/persistence-conflicts");
        if (Files.isDirectory(conflictDir)) try (java.util.stream.Stream<Path> paths=Files.list(conflictDir)) {
            for (Path path : (Iterable<Path>)paths::iterator) if (!existingConflicts.contains(path) && path.getFileName().toString().startsWith("9001-")) Files.delete(path);
        }
    }
    private void stock(User user) throws Exception {
        user.setId(9001); user.setXu(100); user.setPlayerRevision(0);
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
            reloaded.setXu(240); reloaded.setPlayerRevision(1); s.user = reloaded; reloaded.setSession(s);
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
            User second = new User(); second.setId(9001); second.setXu(100); second.setPlayerRevision(0); second.loadFarmData(9001);
            ExecutorService workers = Executors.newFixedThreadPool(2);
            try {
                java.util.List<Future<Boolean>> futures = new java.util.ArrayList<>();
                for (User user : new User[]{s.user,second}) futures.add(workers.submit(() -> {
                    try { user.sellFarmProduct(215,7); return true; }
                    catch (IllegalArgumentException | java.sql.SQLTransactionRollbackException stale) { return false; }
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

    @Test public void legacyPacketReplayAfterLegitimateHarvestSellsNewStockKnownBlocker() throws Exception {
        GameplayFeedbackTest.withSession(s -> {
            stock(s.user);
            byte[] oldPacket = new byte[]{0, (byte) 215};
            FarmMsgHandler handler = new FarmMsgHandler(s);
            handler.onMessage(new Message(Cmd.SELL_ITEM,oldPacket.clone()));
            balance(240);
            s.user.setStarfruitQuantity(20); s.user.setStarfruitLastHarvestMs(0);
            assertEquals(20,s.user.harvestStarfruit());
            User loaded = new User(); loaded.setId(9001); loaded.loadFarmData(9001);
            assertEquals(20,loaded.NongSanDacBiet.get(0).getSoluong());
            handler.onMessage(new Message(Cmd.SELL_ITEM,oldPacket.clone()));
            // Characterization of the unresolved protocol ambiguity, not a replay-safety assertion.
            balance(380); assertEquals(0,s.user.NongSanDacBiet.get(0).getSoluong());
        });
    }

    @Test public void lostCommitAcknowledgementCannotBeOverwrittenByUncertainSession() throws Exception {
        GameplayFeedbackTest.withSession(s -> {
            stock(s.user);
            try (JdbcFault fault = new JdbcFault(db,"ack")) {
                try { s.user.sellFarmProduct(215,7); fail("Expected lost commit acknowledgement"); }
                catch (SQLException expected) { assertTrue(s.user.isFarmSaleUncertain()); }
            }
            balance(240); assertEquals(100,s.user.getXu());
            try { s.user.saveFarmData(9001); fail("Uncertain snapshot must not persist"); }
            catch (SQLException expected) { }
            java.lang.reflect.Method save = User.class.getDeclaredMethod("saveData");
            save.setAccessible(true); save.invoke(s.user); balance(240);
            User loaded = new User(); loaded.setId(9001); loaded.loadFarmData(9001);
            assertEquals(0,loaded.NongSanDacBiet.get(0).getSoluong());
        });
    }

    @Test public void failedCommitBeforeDatabaseCommitLeavesBothRowsUnchanged() throws Exception {
        GameplayFeedbackTest.withSession(s -> {
            stock(s.user);
            try (JdbcFault fault = new JdbcFault(db,"before")) {
                try { s.user.sellFarmProduct(215,7); fail("Expected precommit failure"); }
                catch (SQLException expected) { assertTrue(s.user.isFarmSaleUncertain()); }
            }
            balance(100);
            User loaded = new User(); loaded.setId(9001); loaded.loadFarmData(9001);
            assertEquals(20,loaded.NongSanDacBiet.get(0).getSoluong());
        });
    }

    @Test public void rollbackFailureMarksSessionUncertainAndBlocksItsFarmWrites() throws Exception {
        GameplayFeedbackTest.withSession(s -> {
            stock(s.user);
            try (JdbcFault fault = new JdbcFault(db,"rollback")) {
                try { s.user.sellFarmProduct(215,50000); fail("Expected constraint failure"); }
                catch (SQLException expected) {
                    assertEquals(1,expected.getSuppressed().length);
                    assertTrue(s.user.isFarmSaleUncertain());
                }
            }
            try { s.user.saveFarmData(9001); fail("Unsafe rollback snapshot"); }
            catch (SQLException expected) { }
            balance(100);
            User loaded = new User(); loaded.setId(9001); loaded.loadFarmData(9001);
            assertEquals(20,loaded.NongSanDacBiet.get(0).getSoluong());
        });
    }

    @Test public void staleFarmSnapshotCannotRestoreSoldStock() throws Exception {
        GameplayFeedbackTest.withSession(s -> {
            stock(s.user);
            User stale = new User(); stale.setId(9001); stale.loadFarmData(9001);
            s.user.sellFarmProduct(215,7); balance(240);
            try { stale.saveFarmData(9001); fail("Stale farm write must fail"); }
            catch (SQLException expected) { }
            User loaded = new User(); loaded.setId(9001); loaded.loadFarmData(9001);
            assertEquals(0,loaded.NongSanDacBiet.get(0).getSoluong());
            // Revision conflict preserves the committed inventory; protocol replay is still blocked.
        });
    }

    @Test public void staleAutosaveCannotRestoreStockAfterAnotherSessionHarvests() throws Exception {
        GameplayFeedbackTest.withSession(s -> {
            stock(s.user);
            User stale = new User(); stale.setId(9001); stale.loadFarmData(9001);
            s.user.setStarfruitQuantity(20); s.user.setStarfruitLastHarvestMs(0);
            assertEquals(20,s.user.harvestStarfruit());
            try { stale.saveFarmData(9001); fail("Stale autosave must report a conflict"); }
            catch (SQLException expected) { }
            User loaded = new User(); loaded.setId(9001); loaded.loadFarmData(9001);
            assertEquals(40,loaded.NongSanDacBiet.get(0).getSoluong());
        });
    }

    private void logoutSchema() throws Exception {
        try (Connection c = db.getConnection()) {
            java.util.List<String> columns = new java.util.ArrayList<>();
            for (String name : new String[]{"gender","friendly","crazy","stylish","happy","hunger","chest_slot","chest_home_slot","luong","luong_khoa","xeng","level_main","exp_main","scores","xu_from_boss","TopPhaoLuong","TopPhaoXu","is_online","client_id"})
                columns.add("ADD COLUMN " + name + " INT DEFAULT 0");
            for (String name : new String[]{"chests","wearing","chests_home"})
                columns.add("ADD COLUMN " + name + " LONGTEXT DEFAULT '[]'");
            columns.add("ADD COLUMN attendance_date DATE NULL"); columns.add("ADD COLUMN last_online TIMESTAMP NULL");
            c.createStatement().execute("ALTER TABLE players " + String.join(",",columns));
        }
    }
    private void saveLogout(User user) throws Exception {
        java.lang.reflect.Method save = User.class.getDeclaredMethod("saveData"); save.setAccessible(true);
        try { save.invoke(user); }
        catch (java.lang.reflect.InvocationTargetException failure) { throw (Exception) failure.getCause(); }
    }
    @Test public void staleLogoutCannotOverwriteAnotherCommittedRewardWithSalesOff() throws Exception {
        GameplayFeedbackTest.withSession(s -> {
            logoutSchema(); stock(s.user); System.clearProperty("avatar.farm.sales.catalog");
            User other = new User(); other.setId(9001); other.setPlayerRevision(0); other.setChestSlot((byte)10);
            avatar.item.Part ticket = avatar.item.Part.builder().id(593).type((short)-2).build();
            avatar.item.PartManager.getInstance().getParts().add(ticket);
            try {
                assertTrue(other.claimDailyAttendance());
                try { saveLogout(s.user); fail("Stale logout must report conflict"); }
                catch (IllegalStateException expected) { }
                try (Connection c=db.getConnection(); ResultSet r=c.createStatement().executeQuery("SELECT scores,chests FROM players WHERE user_id=9001")) {
                    assertTrue(r.next()); assertEquals(20,r.getInt(1)); assertTrue(r.getString(2).contains("593"));
                }
                assertEquals(0,s.user.getScores()); assertTrue(s.user.chests.isEmpty());
            } finally { avatar.item.PartManager.getInstance().getParts().remove(ticket); }
        });
    }
    @Test public void farmConflictRollsBackEntireLogoutIncludingCurrency() throws Exception {
        GameplayFeedbackTest.withSession(s -> {
            logoutSchema(); stock(s.user);
            User other=new User(); other.setId(9001); other.loadFarmData(9001);
            other.NongSanDacBiet.get(0).setSoluong(3); other.saveFarmData(9001);
            s.user.setXu(200);
            try { saveLogout(s.user); fail("Farm conflict must roll back currency too"); }
            catch (IllegalStateException expected) { }
            balance(100); assertEquals(200,s.user.getXu());
            Path conflicts=Path.of("logs/persistence-conflicts");
            assertTrue("Rejected logout must retain a reconciliation snapshot",Files.isDirectory(conflicts));
            try (java.util.stream.Stream<Path> files=Files.list(conflicts)) {
                Path snapshot=files.filter(f -> !existingConflicts.contains(f) && f.getFileName().toString().startsWith("9001-")).findFirst().orElseThrow(AssertionError::new);
                assertTrue(Files.readString(snapshot).contains("200")); Files.delete(snapshot);
            }
            User loaded=new User(); loaded.setId(9001); loaded.loadFarmData(9001);
            assertEquals(3,loaded.NongSanDacBiet.get(0).getSoluong());
        });
    }
    @Test public void currentLogoutCommitsFarmAndCurrencyAndAllowsNextSave() throws Exception {
        GameplayFeedbackTest.withSession(s -> {
            logoutSchema(); stock(s.user); s.user.setXu(200);
            s.user.NongSanDacBiet.get(0).setSoluong(3); saveLogout(s.user); balance(200);
            s.user.setXu(300); saveLogout(s.user); balance(300);
            User loaded=new User(); loaded.setId(9001); loaded.loadFarmData(9001);
            assertEquals(3,loaded.NongSanDacBiet.get(0).getSoluong());
        });
    }
    @Test public void staleHarvestRollsBackMemoryWithoutOverwritingCommittedFarm() throws Exception {
        GameplayFeedbackTest.withSession(s -> {
            stock(s.user); User stale=new User(); stale.setId(9001); stale.loadFarmData(9001);
            s.user.NongSanDacBiet.get(0).setSoluong(3); s.user.saveFarmData(9001);
            try { stale.harvestStarfruit(); fail("Stale harvest must conflict"); }
            catch (SQLException expected) { }
            assertEquals(20,stale.NongSanDacBiet.get(0).getSoluong());
            assertEquals(180,stale.getStarfruitQuantity());
            User loaded=new User(); loaded.setId(9001); loaded.loadFarmData(9001);
            assertEquals(3,loaded.NongSanDacBiet.get(0).getSoluong());
        });
    }
    @Test public void failedLogoutCommitCannotOverwriteCommittedStateOnRetry() throws Exception {
        GameplayFeedbackTest.withSession(s -> {
            logoutSchema(); stock(s.user); s.user.setXu(200);
            try (JdbcFault fault=new JdbcFault(db,"ack")) {
                try { saveLogout(s.user); fail("Expected ambiguous commit"); }
                catch (IllegalStateException expected) { }
            }
            balance(200); assertTrue(s.user.isFarmSaleUncertain());
            s.user.setXu(999); saveLogout(s.user); balance(200);
        });
    }

    @Test public void duplicateLoginCannotUnlockOrDisconnectCurrentOwner() throws Exception {
        GameplayFeedbackTest.withSession(s -> {
            try (Connection c=db.getConnection()) {
                c.createStatement().execute("CREATE TABLE IF NOT EXISTS users (id INT PRIMARY KEY, username VARCHAR(40))");
                c.createStatement().execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS password VARCHAR(40), ADD COLUMN IF NOT EXISTS role INT DEFAULT 0, ADD COLUMN IF NOT EXISTS active INT DEFAULT 1, ADD COLUMN IF NOT EXISTS login_lock INT DEFAULT 0, ADD COLUMN IF NOT EXISTS ban LONGTEXT");
                c.createStatement().execute("REPLACE INTO users (id,username,password,login_lock) VALUES (9001,'fixture', '"+avatar.server.Utils.md5("test-only")+"',1)");
            }
            boolean active=avatar.server.ServerManager.active; avatar.server.ServerManager.active=true;
            s.user.setUsername("fixture"); s.user.setPassword("test-only");
            try {
                assertFalse(s.user.login());
                try (Connection c=db.getConnection(); ResultSet r=c.createStatement().executeQuery("SELECT login_lock FROM users WHERE id=9001")) {
                    assertTrue(r.next()); assertEquals(1,r.getInt(1));
                }
                try (Connection c=db.getConnection()) { c.createStatement().execute("UPDATE users SET login_lock=0 WHERE id=9001"); }
                avatar.server.UserManager.getInstance().add(s.user);
                int messages=s.messages.size();
                assertFalse(s.user.login());
                assertEquals("Duplicate login must only notify the incoming session",messages+1,s.messages.size());
                assertSame(s.user,avatar.server.UserManager.getInstance().find(9001));
            } finally {
                avatar.server.UserManager.getInstance().remove(s.user);
                avatar.server.ServerManager.active=active;
                try (Connection c=db.getConnection()) { c.createStatement().execute("DROP TABLE users"); }
            }
        });
    }

    @Test public void directLegacyCounterWriteAlsoInvalidatesStaleLogout() throws Exception {
        GameplayFeedbackTest.withSession(s -> {
            logoutSchema(); stock(s.user);
            User other=new User(); other.setId(9001); other.setXu(100); other.setPlayerRevision(0); other.updateTopPhaoXu(5);
            try { saveLogout(s.user); fail("Direct legacy write must invalidate old snapshot"); }
            catch (IllegalStateException expected) { }
            try (Connection c=db.getConnection(); ResultSet r=c.createStatement().executeQuery("SELECT TopPhaoXu FROM players WHERE user_id=9001")) {
                assertTrue(r.next()); assertEquals(1,r.getInt(1));
            }
        });
    }

    @Test public void concurrentFarmWritersHaveOneWinnerAndKeepRejectedProposal() throws Exception {
        GameplayFeedbackTest.withSession(s -> {
            stock(s.user); User other=new User(); other.setId(9001); other.loadFarmData(9001);
            s.user.NongSanDacBiet.get(0).setSoluong(3); other.NongSanDacBiet.get(0).setSoluong(4);
            ExecutorService workers=Executors.newFixedThreadPool(2);
            try {
                java.util.List<Future<Boolean>> results=new java.util.ArrayList<>();
                for (User user : new User[]{s.user,other}) results.add(workers.submit(() -> {
                    try { user.saveFarmData(9001); return true; } catch (SQLTransactionRollbackException conflict) { return false; }
                }));
                assertTrue(results.get(0).get(10,TimeUnit.SECONDS) ^ results.get(1).get(10,TimeUnit.SECONDS));
                assertEquals(3,s.user.NongSanDacBiet.get(0).getSoluong()); assertEquals(4,other.NongSanDacBiet.get(0).getSoluong());
                User loaded=new User(); loaded.setId(9001); loaded.loadFarmData(9001);
                int quantity=loaded.NongSanDacBiet.get(0).getSoluong(); assertTrue(quantity==3 || quantity==4);
            } finally { workers.shutdownNow(); }
        });
    }
    @Test public void ambiguousHarvestCommitCannotBeCreditedAgainAfterReload() throws Exception {
        GameplayFeedbackTest.withSession(s -> {
            stock(s.user);
            try (JdbcFault fault=new JdbcFault(db,"ack")) {
                try { s.user.harvestStarfruit(); fail("Expected lost acknowledgement"); } catch (SQLException expected) { }
            }
            assertTrue(s.user.isFarmSaleUncertain()); assertEquals(20,s.user.NongSanDacBiet.get(0).getSoluong());
            try { s.user.saveFarmData(9001); fail("Ambiguous old snapshot must be blocked"); } catch (SQLException expected) { }
            User loaded=new User(); loaded.setId(9001); loaded.loadFarmData(9001);
            assertEquals(200,loaded.NongSanDacBiet.get(0).getSoluong()); assertEquals(0,loaded.harvestStarfruit());
        });
    }

    @Test public void attendanceResourceCloseFailureCannotEraseCommittedReward() throws Exception {
        GameplayFeedbackTest.withSession(s -> {
            logoutSchema(); stock(s.user); s.user.setChestSlot((byte)10);
            avatar.item.Part ticket=avatar.item.Part.builder().id(593).type((short)-2).build();
            avatar.item.PartManager.getInstance().getParts().add(ticket);
            try {
                try (JdbcFault fault=new JdbcFault(db,"close")) {
                    try { s.user.claimDailyAttendance(); fail("Expected close failure"); } catch (SQLException expected) { }
                }
                assertTrue("Close failure must block stale logout",s.user.isFarmSaleUncertain());
                saveLogout(s.user);
                try (Connection c=db.getConnection(); ResultSet r=c.createStatement().executeQuery("SELECT scores,chests FROM players WHERE user_id=9001")) {
                    assertTrue(r.next()); assertEquals(20,r.getInt(1)); assertTrue(r.getString(2).contains("593"));
                }
            } finally { avatar.item.PartManager.getInstance().getParts().remove(ticket); }
        });
    }
    @Test public void counterResourceCloseFailureCannotEraseCommittedCounter() throws Exception {
        GameplayFeedbackTest.withSession(s -> {
            logoutSchema(); stock(s.user);
            try (JdbcFault fault=new JdbcFault(db,"close")) {
                try { s.user.updateTopPhaoXu(5); fail("Expected close failure"); } catch (IllegalStateException expected) { }
            }
            assertTrue("Close failure must block stale logout",s.user.isFarmSaleUncertain());
            saveLogout(s.user);
            try (Connection c=db.getConnection(); ResultSet r=c.createStatement().executeQuery("SELECT TopPhaoXu FROM players WHERE user_id=9001")) {
                assertTrue(r.next()); assertEquals(1,r.getInt(1));
            }
        });
    }

    /** Inject only JDBC boundary failures; all queries/locks/commits use the real isolated DB. */
    private static final class JdbcFault implements AutoCloseable {
        private final DbManager db;
        private final java.lang.reflect.Field field;
        private final com.zaxxer.hikari.HikariDataSource original;
        JdbcFault(DbManager db, String mode) throws Exception {
            this.db = db;
            field = DbManager.class.getDeclaredField("hikariDataSource"); field.setAccessible(true);
            original = (com.zaxxer.hikari.HikariDataSource) field.get(db);
            field.set(db,new com.zaxxer.hikari.HikariDataSource() {
                @Override public Connection getConnection() throws SQLException {
                    Connection real = original.getConnection();
                    return (Connection) java.lang.reflect.Proxy.newProxyInstance(Connection.class.getClassLoader(),
                            new Class<?>[]{Connection.class}, (proxy,method,args) -> {
                                if (method.getName().equals("commit") && mode.equals("before"))
                                    throw new SQLException("Synthetic precommit failure");
                                if (method.getName().equals("rollback") && mode.equals("rollback"))
                                    throw new SQLException("Synthetic rollback failure");
                                try {
                                    Object result = method.invoke(real,args);
                                    if (method.getName().equals("close") && mode.equals("close"))
                                        throw new SQLException("Synthetic resource close failure");
                                    if (method.getName().equals("commit") && mode.equals("ack"))
                                        throw new SQLException("Synthetic lost commit acknowledgement");
                                    return result;
                                } catch (java.lang.reflect.InvocationTargetException e) { throw e.getCause(); }
                            });
                }
            });
        }
        @Override public void close() throws Exception { field.set(db,original); }
    }

}
