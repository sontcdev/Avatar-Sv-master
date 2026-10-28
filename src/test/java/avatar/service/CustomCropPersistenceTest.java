package avatar.service;

import avatar.Farm.HatGiong;
import avatar.Farm.LandItem;
import avatar.constants.Cmd;
import avatar.db.DbManager;
import avatar.message.FarmMsgHandler;
import avatar.model.User;
import avatar.network.Message;
import com.zaxxer.hikari.HikariDataSource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class CustomCropPersistenceTest {
    @Test public void handlersPersistCropAndHarvestWithCas() throws Exception {
        DbManager db = DbManager.getInstance();
        Field poolField = DbManager.class.getDeclaredField("hikariDataSource"); poolField.setAccessible(true);
        assertNull("Never replace an active database pool", poolField.get(db));
        boolean maria = Boolean.getBoolean("avatar.test.customCropMariaDb");
        HikariDataSource pool = null;
        if (maria) db.start();
        else {
            pool = new HikariDataSource();
            pool.setJdbcUrl("jdbc:h2:mem:custom_crop_test;MODE=MySQL;DB_CLOSE_DELAY=-1");
            poolField.set(db, pool);
        }
        try {
            try (Connection c = db.getConnection()) {
                if (maria) assertEquals("avatar2d_customcrop_test", c.getCatalog());
                c.createStatement().execute("CREATE TABLE farm_data (user_id INT PRIMARY KEY, land_data LONGTEXT, animal_data LONGTEXT, hatgiong LONGTEXT, phanbon LONGTEXT, nongsan LONGTEXT, nongsandacbiet LONGTEXT, starfruit_quantity INT, starfruit_last_harvest_ms BIGINT, cooking_recipe INT, cooking_ready_ms BIGINT, cooking_output_id INT, persistence_revision BIGINT)");
            }
            GameplayFeedbackTest.withSession(session -> {
                CustomCropPolicy policy = CustomCropPolicy.load(CustomCropPolicyTest.testConfig());
                User first = new User(); first.setId(7); first.setSession(session); session.user = first;
                first.landItems.add(new LandItem(0, -1, 0, 0, false, false, false, null));
                first.hatgiong.add(new HatGiong(2, 2)); first.saveFarmData(7);
                User stale = new User(); stale.setId(7); stale.loadFarmData(7);
                Field serviceField = avatar.network.Session.class.getDeclaredField("farmService"); serviceField.setAccessible(true);
                serviceField.set(session, new FarmService(session, policy, fixed(1_000_000)));
                FarmMsgHandler handler = new FarmMsgHandler(session);
                Message join = new Message(Cmd.JOIN); join.writer().writeInt(7);
                handler.onMessage(new Message(Cmd.JOIN, join.getData()));
                Message plant = new Message(Cmd.PLANT_SEED); plant.writer().writeInt(7); plant.writer().writeByte(0); plant.writer().writeByte(2);
                Message trailing = new Message(Cmd.PLANT_SEED); trailing.writer().write(plant.getData()); trailing.writer().writeByte(0);
                handler.onMessage(new Message(Cmd.PLANT_SEED, trailing.getData()));
                Message foreign = new Message(Cmd.PLANT_SEED); foreign.writer().writeInt(8); foreign.writer().writeByte(0); foreign.writer().writeByte(2);
                handler.onMessage(new Message(Cmd.PLANT_SEED, foreign.getData()));
                assertEquals(2, first.hatgiong.get(0).getSoluong());
                handler.onMessage(new Message(Cmd.PLANT_SEED, plant.getData()));
                handler.onMessage(new Message(Cmd.PLANT_SEED, plant.getData()));
                assertEquals(1, first.hatgiong.get(0).getSoluong());
                try { stale.plantCustomCrop(0, 2, policy, 1_000_000); fail("stale revision"); } catch (SQLException expected) { }
                assertEquals(2, stale.hatgiong.get(0).getSoluong());
                User reloaded = new User(); reloaded.setId(7); reloaded.loadFarmData(7);
                assertEquals(15_400_000, reloaded.landItems.get(0).getCustomDeadlineMs());
                assertEquals(CustomCropPolicy.VERSION, reloaded.landItems.get(0).getCustomPolicy());
                assertEquals(1, reloaded.hatgiong.get(0).getSoluong());
                reloaded.setSession(session); session.user = reloaded;
                serviceField.set(session, new FarmService(session, policy, fixed(15_399_999)));
                handler = new FarmMsgHandler(session); handler.onMessage(new Message(Cmd.JOIN, join.getData()));
                Message harvest = new Message(Cmd.TREE_HARVEST); harvest.writer().writeInt(7); harvest.writer().writeByte(0);
                handler.onMessage(new Message(Cmd.TREE_HARVEST, harvest.getData()));
                assertTrue(reloaded.NongSan.isEmpty());
                serviceField.set(session, new FarmService(session, policy, fixed(15_400_000)));
                handler = new FarmMsgHandler(session); handler.onMessage(new Message(Cmd.JOIN, join.getData()));
                handler.onMessage(new Message(Cmd.TREE_HARVEST, harvest.getData()));
                handler.onMessage(new Message(Cmd.TREE_HARVEST, harvest.getData()));
                User after = new User(); after.setId(7); after.loadFarmData(7);
                assertEquals(-1, after.landItems.get(0).getType());
                assertEquals(2, after.NongSan.get(0).getSoluong());
                assertEquals(3, after.NongSan.get(0).getId());
                after.plantCustomCrop(0, 2, policy, 20_000_000);
                User left = new User(); left.setId(7); left.loadFarmData(7);
                User right = new User(); right.setId(7); right.loadFarmData(7);
                ExecutorService executor = Executors.newFixedThreadPool(2);
                try {
                    Callable<Boolean> a = () -> { try { left.harvestCustomCrop(0, policy, 34_400_000); return true; } catch (SQLException expected) { return false; } };
                    Callable<Boolean> b = () -> { try { right.harvestCustomCrop(0, policy, 34_400_000); return true; } catch (SQLException expected) { return false; } };
                    Future<Boolean> x = executor.submit(a), y = executor.submit(b);
                    assertNotEquals(x.get(), y.get());
                } finally { executor.shutdownNow(); }
                User finalState = new User(); finalState.setId(7); finalState.loadFarmData(7);
                assertEquals(4, finalState.NongSan.get(0).getSoluong());
                assertEquals(-1, finalState.landItems.get(0).getType());
                finalState.hatgiong.get(0).setSoluong(1); finalState.saveFarmData(7);
                try (Connection c = db.getConnection()) { c.createStatement().execute("ALTER TABLE farm_data ADD CONSTRAINT no_custom CHECK (land_data NOT LIKE '%custom-crop-v1%')"); }
                try { finalState.plantCustomCrop(0, 2, policy, 40_000_000); fail("SQL failure"); }
                catch (SQLException expected) { }
                assertEquals(1, finalState.hatgiong.get(0).getSoluong());
                assertEquals(-1, finalState.landItems.get(0).getType());
                User afterFailure = new User(); afterFailure.setId(7); afterFailure.loadFarmData(7);
                assertEquals(1, afterFailure.hatgiong.get(0).getSoluong());
                assertEquals(-1, afterFailure.landItems.get(0).getType());
            });
        } finally { if (maria) db.shutdown(); else { poolField.set(db, null); pool.close(); } }
    }
    private static Clock fixed(long millis) { return Clock.fixed(Instant.ofEpochMilli(millis), ZoneOffset.UTC); }
}
