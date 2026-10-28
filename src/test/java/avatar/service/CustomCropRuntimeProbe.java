package avatar.service;

import avatar.Farm.HatGiong;
import avatar.Farm.LandItem;
import avatar.constants.Cmd;
import avatar.db.DbManager;
import avatar.message.FarmMsgHandler;
import avatar.model.User;
import avatar.network.Message;
import java.sql.Connection;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/** Executed only against the dedicated disposable MariaDB fixture. */
public class CustomCropRuntimeProbe {
    public static void main(String[] args) throws Exception {
        DbManager db = DbManager.getInstance(); db.start();
        try {
            try (Connection c = db.getConnection()) {
                if (!"avatar2d_customcrop_test".equals(c.getCatalog())) throw new IllegalStateException("Wrong test catalog");
                if (args[0].equals("plant")) c.createStatement().execute("CREATE TABLE farm_data (user_id INT PRIMARY KEY, land_data LONGTEXT, animal_data LONGTEXT, hatgiong LONGTEXT, phanbon LONGTEXT, nongsan LONGTEXT, nongsandacbiet LONGTEXT, starfruit_quantity INT, starfruit_last_harvest_ms BIGINT, cooking_recipe INT, cooking_ready_ms BIGINT, cooking_output_id INT, persistence_revision BIGINT)");
            }
            GameplayFeedbackTest.withSession(session -> {
                User user = new User(); user.setId(7); user.setSession(session); session.user = user;
                if (args[0].equals("plant")) {
                    user.landItems.add(new LandItem(0, -1, 0, 0, false, false, false, null));
                    user.hatgiong.add(new HatGiong(0, 1)); user.saveFarmData(7);
                } else user.loadFarmData(7);
                FarmMsgHandler handler = new FarmMsgHandler(session);
                Message join = new Message(Cmd.JOIN); join.writer().writeInt(7);
                handler.onMessage(new Message(Cmd.JOIN, join.getData()));
                if (args[0].equals("plant")) {
                    Message plant = new Message(Cmd.PLANT_SEED); plant.writer().writeInt(7); plant.writer().writeByte(0); plant.writer().writeByte(0);
                    handler.onMessage(new Message(Cmd.PLANT_SEED, plant.getData()));
                    if (user.hatgiong.get(0).getSoluong() != 0 || user.landItems.get(0).getCustomPolicy() == null) throw new AssertionError("plant failed");
                    Message early = new Message(Cmd.TREE_HARVEST); early.writer().writeInt(7); early.writer().writeByte(0);
                    handler.onMessage(new Message(Cmd.TREE_HARVEST, early.getData()));
                    if (!user.NongSan.isEmpty()) throw new AssertionError("early harvest succeeded");
                } else {
                    long deadline = user.landItems.get(0).getCustomDeadlineMs();
                    if (deadline <= 0 || user.hatgiong.get(0).getSoluong() != 0) throw new AssertionError("restart lost crop");
                    java.lang.reflect.Field field = avatar.network.Session.class.getDeclaredField("farmService"); field.setAccessible(true);
                    field.set(session, new FarmService(session, CustomCropPolicy.runtime(), Clock.fixed(Instant.ofEpochMilli(deadline), ZoneOffset.UTC)));
                    handler = new FarmMsgHandler(session); handler.onMessage(new Message(Cmd.JOIN, join.getData()));
                    Message harvest = new Message(Cmd.TREE_HARVEST); harvest.writer().writeInt(7); harvest.writer().writeByte(0);
                    handler.onMessage(new Message(Cmd.TREE_HARVEST, harvest.getData()));
                    handler.onMessage(new Message(Cmd.TREE_HARVEST, harvest.getData()));
                    User verify = new User(); verify.setId(7); verify.loadFarmData(7);
                    if (verify.landItems.get(0).getType() != -1 || verify.NongSan.get(0).getSoluong() != 2) throw new AssertionError("harvest failed");
                }
            });
            System.out.println("CUSTOM_CROP_RUNTIME_PROBE_PASS " + args[0]);
        } finally { db.shutdown(); }
    }
}
