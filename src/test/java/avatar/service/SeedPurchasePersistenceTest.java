package avatar.service;

import avatar.db.DbManager;
import avatar.model.User;
import avatar.item.PartManager;
import avatar.Farm.farmItem;
import com.zaxxer.hikari.HikariDataSource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import org.junit.Test;
import static org.junit.Assert.*;

public class SeedPurchasePersistenceTest {
    @Test public void purchaseCommitsBothRowsAndRejectsStaleOrFailedWrites() throws Exception {
        Field field = DbManager.class.getDeclaredField("hikariDataSource"); field.setAccessible(true);
        DbManager db = DbManager.getInstance(); assertNull(field.get(db));
        HikariDataSource pool = new HikariDataSource();
        String url = System.getProperty("avatar.test.seedPurchaseUrl");
        if (url != null && !url.matches("jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/avatar2d_seed_purchase_test"))
            throw new IllegalArgumentException("Only an isolated loopback seed test database is allowed");
        pool.setJdbcUrl(url == null ? "jdbc:h2:mem:seed_purchase;MODE=MySQL" : url);
        if (url != null) pool.setUsername("root");
        field.set(db, pool);
        farmItem seed = new farmItem(5, "fixture", 5, 10, 20);
        PartManager.getInstance().getFarmItems().add(seed);
        try {
            try (Connection c = db.getConnection()) {
                c.createStatement().execute("CREATE TABLE players (user_id INT PRIMARY KEY,xu BIGINT,luong INT,persistence_revision BIGINT)");
                c.createStatement().execute("INSERT INTO players VALUES (7,100,50,0)");
                c.createStatement().execute("CREATE TABLE farm_data (user_id INT PRIMARY KEY,land_data LONGTEXT,animal_data LONGTEXT,hatgiong LONGTEXT,phanbon LONGTEXT,nongsan LONGTEXT,nongsandacbiet LONGTEXT,starfruit_quantity INT,starfruit_last_harvest_ms BIGINT,cooking_recipe INT,cooking_ready_ms BIGINT,cooking_output_id INT,persistence_revision BIGINT)");
            }
            User user = new User(); user.setId(7); user.setXu(100); user.setLuong(50);
            Field revision = User.class.getDeclaredField("playerRevision"); revision.setAccessible(true); revision.setLong(user, 0);
            user.saveFarmData(7);
            GameplayFeedbackTest.withSession(session -> {
                session.user = user; user.setSession(session);
                avatar.network.Message request = new avatar.network.Message(avatar.constants.Cmd.BUY_ITEM);
                request.writer().writeShort(5); request.writer().writeByte(2); request.writer().writeByte(1);
                new avatar.message.FarmMsgHandler(session).onMessage(new avatar.network.Message(avatar.constants.Cmd.BUY_ITEM, request.getData()));
                avatar.network.Message response = session.messages.stream()
                        .filter(m -> m.getCommand() == avatar.constants.Cmd.BUY_ITEM).findFirst().orElseThrow(AssertionError::new);
                var decoded = new avatar.network.Message(response.getCommand(), response.getData()).reader();
                assertEquals(5, decoded.readShort()); assertEquals(2, decoded.readByte());
                assertEquals(100, decoded.readInt()); assertEquals(1, decoded.readByte());
                assertEquals(60, decoded.readInt()); assertEquals(50, decoded.readInt());
                assertEquals(0, decoded.readInt()); assertEquals(0, decoded.available());
            });
            assertEquals(60, user.getXu()); assertEquals(50, user.getLuong());
            user.buyFarmSeed(5, 1, 0);
            assertEquals(30, user.getLuong()); assertEquals(3, user.hatgiong.get(0).getSoluong());
            User loaded = new User(); loaded.setId(7); loaded.loadFarmData(7);
            assertEquals(3, loaded.hatgiong.get(0).getSoluong());
            try (Connection c = db.getConnection(); var r = c.createStatement().executeQuery("SELECT xu,luong FROM players WHERE user_id=7")) {
                assertTrue(r.next()); assertEquals(60,r.getLong(1)); assertEquals(30,r.getInt(2));
            }
            for (int quantity : new int[]{-1, 0, 100}) {
                try { user.buyFarmSeed(5, quantity, 1); fail(); } catch (IllegalArgumentException expected) { }
            }
            try { user.buyFarmSeed(5, 4, 1); fail(); } catch (IllegalArgumentException expected) { }
            user.landItems.add(new avatar.Farm.LandItem(0, -1, 0, 0, false, false, false, null));
            user.saveFarmData(7);
            user.plantCrop(0, 5);
            user.landItems.get(0).setPlantedTime(java.time.LocalDateTime.now().minusMinutes(10));
            user.saveFarmData(7);
            User mature = new User(); mature.setId(7); mature.loadFarmData(7);
            assertEquals(10, mature.harvestCrop(0));
            User harvested = new User(); harvested.setId(7); harvested.loadFarmData(7);
            assertEquals(2, harvested.hatgiong.get(0).getSoluong());
            assertEquals(10, harvested.NongSan.get(0).getSoluong());
            // Use the current farm revision to isolate the deliberately induced failure below.
            user.loadFarmData(7);
            try { user.buyFarmSeed(99, 1, 1); fail(); } catch (IllegalArgumentException expected) { }
            try { user.buyFarmSeed(5, 1, 2); fail(); } catch (IllegalArgumentException expected) { }
            user.hatgiong.get(0).setSoluong(99);
            try { user.buyFarmSeed(5, 1, 1); fail(); } catch (IllegalArgumentException expected) { }
            user.hatgiong.get(0).setSoluong(2);
            user.hatgiong.add(new avatar.Farm.HatGiong(5, 1));
            try { user.buyFarmSeed(5, 1, 1); fail(); } catch (IllegalArgumentException expected) { }
            user.hatgiong.remove(1);
            java.util.concurrent.ExecutorService workers = java.util.concurrent.Executors.newFixedThreadPool(2);
            try {
                java.util.concurrent.Callable<Boolean> buy = () -> {
                    try { user.buyFarmSeed(5, 2, 1); return true; }
                    catch (IllegalArgumentException insufficient) { return false; }
                };
                var first = workers.submit(buy); var second = workers.submit(buy);
                assertTrue(first.get() ^ second.get());
                assertEquals(20, user.getXu());
                assertEquals(4, user.hatgiong.get(0).getSoluong());
            } finally { workers.shutdownNow(); }
            // Force the farm CAS to fail after the money UPDATE, proving transaction rollback.
            try (Connection c = db.getConnection()) { c.createStatement().execute("UPDATE farm_data SET persistence_revision=persistence_revision+1 WHERE user_id=7"); }
            try { user.buyFarmSeed(5, 1, 1); fail(); } catch (SQLException expected) { }
            assertEquals(20,user.getXu()); assertEquals(4,user.hatgiong.get(0).getSoluong());
            try (Connection c = db.getConnection(); var r = c.createStatement().executeQuery("SELECT xu FROM players WHERE user_id=7")) {
                assertTrue(r.next()); assertEquals(20,r.getLong(1));
            }
        } finally { PartManager.getInstance().getFarmItems().remove(seed); pool.close(); field.set(db,null); }
    }
}
