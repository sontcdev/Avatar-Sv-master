package avatar.service;

import avatar.Farm.NongSan;
import avatar.Farm.FarmCookingCatalog;
import avatar.db.DbManager;
import avatar.model.User;
import java.lang.reflect.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import org.junit.*;
import static org.junit.Assert.*;

/** Opt-in integration checks against the disposable sales database only. */
public class CookingPersistenceTest {
    // Native farm_info.dat recipe 3: 1,440 rice, thirty minutes, one cooked product.
    private static final FarmCookingCatalog.Recipe RICE =
            new FarmCookingCatalog.Recipe(3,"Cơm",207,30,Map.of(6,1440));
    private static final long START = 1000;
    private static final long READY = 1801000;
    private DbManager db;
    private final Set<Path> existingConflicts = new HashSet<>();

    @Before public void setup() throws Exception {
        Assume.assumeTrue(Boolean.getBoolean("avatar.test.isolatedSalesDb"));
        Path conflicts = Path.of("logs/persistence-conflicts");
        if (Files.isDirectory(conflicts)) try (java.util.stream.Stream<Path> paths=Files.list(conflicts)) {
            paths.forEach(existingConflicts::add);
        }
        db=DbManager.getInstance(); db.start();
        try (Connection c=db.getConnection(); Statement sql=c.createStatement()) {
            assertEquals("avatar2d_sales_test",c.getCatalog());
            sql.execute("DROP TABLE IF EXISTS farm_data");
            sql.execute("DROP TABLE IF EXISTS players");
            sql.execute("CREATE TABLE farm_data (user_id INT PRIMARY KEY, land_data LONGTEXT, animal_data LONGTEXT, hatgiong LONGTEXT, phanbon LONGTEXT, nongsan LONGTEXT, nongsandacbiet LONGTEXT, starfruit_quantity INT DEFAULT 180, starfruit_last_harvest_ms BIGINT DEFAULT 0, cooking_recipe INT DEFAULT 0, cooking_ready_ms BIGINT DEFAULT 0, cooking_output_id INT DEFAULT 0, persistence_revision BIGINT NOT NULL DEFAULT 0) ENGINE=InnoDB");
            List<String> columns=new ArrayList<>();
            columns.add("user_id INT PRIMARY KEY");
            columns.add("xu BIGINT DEFAULT 100");
            columns.add("persistence_revision BIGINT NOT NULL DEFAULT 0");
            for (String name:new String[]{"gender","friendly","crazy","stylish","happy","hunger","chest_slot","chest_home_slot","luong","luong_khoa","xeng","level_main","exp_main","scores","xu_from_boss","TopPhaoLuong","TopPhaoXu"})
                columns.add(name+" INT DEFAULT 0");
            for (String name:new String[]{"chests","wearing","chests_home"}) columns.add(name+" LONGTEXT DEFAULT '[]'");
            sql.execute("CREATE TABLE players ("+String.join(",",columns)+") ENGINE=InnoDB");
            sql.execute("INSERT INTO players (user_id) VALUES (9001)");
        }
    }

    @After public void cleanup() throws Exception {
        if (db==null) return;
        db.shutdown();
        Path conflicts=Path.of("logs/persistence-conflicts");
        if (Files.isDirectory(conflicts)) try (java.util.stream.Stream<Path> paths=Files.list(conflicts)) {
            for (Path path:(Iterable<Path>)paths::iterator)
                if (!existingConflicts.contains(path) && path.getFileName().toString().startsWith("9001-")) Files.delete(path);
        }
    }

    private User stock() throws Exception {
        User user=new User(); user.setId(9001); user.setXu(100); user.setPlayerRevision(0);
        user.NongSan.add(new NongSan(6,2880)); user.saveFarmData(9001);
        return user;
    }
    private User reload() throws Exception {
        User user=new User(); user.setId(9001); user.loadFarmData(9001); return user;
    }
    private void logout(User user) throws Exception {
        Method save=User.class.getDeclaredMethod("saveData"); save.setAccessible(true);
        try { save.invoke(user); }
        catch (InvocationTargetException failure) { throw (Exception)failure.getCause(); }
    }
    private void assertActive(User user) {
        assertEquals(3,user.getCookingRecipe()); assertEquals(READY,user.getCookingReadyMs());
        assertEquals(207,user.getCookingOutputId()); assertEquals(1440,user.NongSan.get(0).getSoluong());
    }

    @Test public void reloadPreservesDeadlineAndCollectionCreditsExactlyOnce() throws Exception {
        User user=stock(); user.startCooking(RICE,START);
        User loaded=reload(); assertActive(loaded);
        try { loaded.collectCooking(READY-1); fail("Recipe is not ready"); }
        catch (IllegalArgumentException expected) { }
        assertTrue(reload().NongSanDacBiet.isEmpty());
        assertEquals(207,loaded.collectCooking(READY));
        User collected=reload();
        assertEquals(0,collected.getCookingRecipe()); assertEquals(0,collected.getCookingReadyMs());
        assertEquals(0,collected.getCookingOutputId());
        assertEquals(207,collected.NongSanDacBiet.get(0).getId());
        assertEquals(1,collected.NongSanDacBiet.get(0).getSoluong());
        try { collected.collectCooking(READY+1); fail("Collected cooking must not credit twice"); }
        catch (IllegalArgumentException expected) { }
        assertEquals(1,reload().NongSanDacBiet.get(0).getSoluong());
    }

    @Test public void staleStartRollsBackIngredientsAndCannotReplaceCommittedCooking() throws Exception {
        User user=stock(), stale=reload(); user.startCooking(RICE,START);
        try { stale.startCooking(RICE,START+100); fail("Stale farm revision must conflict"); }
        catch (SQLException expected) { }
        assertEquals(2880,stale.NongSan.get(0).getSoluong()); assertEquals(0,stale.getCookingRecipe());
        assertActive(reload());
    }

    @Test public void staleCollectionRestoresMemoryWithoutDuplicatingCommittedReward() throws Exception {
        User user=stock(); user.startCooking(RICE,START); User stale=reload();
        user.collectCooking(READY);
        try { stale.collectCooking(READY); fail("Stale collection must conflict"); }
        catch (SQLException expected) { }
        assertActive(stale); assertTrue(stale.NongSanDacBiet.isEmpty());
        assertEquals(1,reload().NongSanDacBiet.get(0).getSoluong());
    }

    @Test public void logoutPreservesCookingAndStaleLogoutRollsBackCurrency() throws Exception {
        User user=stock(), stale=reload(); stale.setXu(999); stale.setPlayerRevision(0);
        user.startCooking(RICE,START);
        try { logout(stale); fail("Stale logout must conflict"); }
        catch (IllegalStateException expected) { }
        try (Connection c=db.getConnection(); ResultSet r=c.createStatement().executeQuery("SELECT xu FROM players WHERE user_id=9001")) {
            assertTrue(r.next()); assertEquals(100,r.getLong(1));
        }
        logout(user); assertActive(reload());
        user.collectCooking(READY); logout(user);
        User loaded=reload(); assertEquals(0,loaded.getCookingRecipe());
        assertEquals(1,loaded.NongSanDacBiet.get(0).getSoluong());
    }

    @Test public void failedStartCommitRestoresIngredientsAndPersistedIdleState() throws Exception {
        User user=stock();
        try (CommitFault fault=new CommitFault("before")) {
            try { user.startCooking(RICE,START); fail("Synthetic precommit failure"); }
            catch (SQLException expected) { }
        }
        assertEquals(2880,user.NongSan.get(0).getSoluong()); assertEquals(0,user.getCookingRecipe());
        User loaded=reload(); assertEquals(2880,loaded.NongSan.get(0).getSoluong());
        assertEquals(0,loaded.getCookingRecipe());
    }

    @Test public void ambiguousCollectionCommitBlocksOldWritesAndReloadCannotCreditAgain() throws Exception {
        User user=stock(); user.startCooking(RICE,START);
        try (CommitFault fault=new CommitFault("ack")) {
            try { user.collectCooking(READY); fail("Synthetic lost acknowledgement"); }
            catch (SQLException expected) { }
        }
        assertTrue(user.isFarmSaleUncertain()); assertActive(user); assertTrue(user.NongSanDacBiet.isEmpty());
        try { user.saveFarmData(9001); fail("Uncertain old snapshot cannot overwrite reward"); }
        catch (SQLException expected) { }
        User loaded=reload(); assertEquals(0,loaded.getCookingRecipe());
        assertEquals(1,loaded.NongSanDacBiet.get(0).getSoluong());
        try { loaded.collectCooking(READY+1); fail("Committed reward must not be collected again"); }
        catch (IllegalArgumentException expected) { }
    }

    /** Real isolated DB transactions with failure injected only at the commit boundary. */
    private final class CommitFault implements AutoCloseable {
        private final Field field;
        private final com.zaxxer.hikari.HikariDataSource original;
        CommitFault(String mode) throws Exception {
            field=DbManager.class.getDeclaredField("hikariDataSource"); field.setAccessible(true);
            original=(com.zaxxer.hikari.HikariDataSource)field.get(db);
            field.set(db,new com.zaxxer.hikari.HikariDataSource() {
                @Override public Connection getConnection() throws SQLException {
                    Connection real=original.getConnection();
                    return (Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class<?>[]{Connection.class},(proxy,method,args) -> {
                        if (method.getName().equals("commit") && mode.equals("before")) throw new SQLException("Synthetic precommit failure");
                        try {
                            Object result=method.invoke(real,args);
                            if (method.getName().equals("commit") && mode.equals("ack")) throw new SQLException("Synthetic lost acknowledgement");
                            return result;
                        } catch (InvocationTargetException failure) { throw failure.getCause(); }
                    });
                }
            });
        }
        @Override public void close() throws Exception { field.set(db,original); }
    }
}
