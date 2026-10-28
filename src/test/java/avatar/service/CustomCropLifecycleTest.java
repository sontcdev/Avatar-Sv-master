package avatar.service;

import avatar.Farm.HatGiong;
import avatar.Farm.LandItem;
import avatar.constants.Cmd;
import avatar.message.FarmMsgHandler;
import avatar.model.User;
import avatar.network.Message;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.Test;
import static org.junit.Assert.*;

public class CustomCropLifecycleTest {
    @Test public void handlersPlantOnceAndHarvestAtDeadline() throws Exception {
        GameplayFeedbackTest.withSession(session -> {
            StarfruitHarvestTest.SavedUser user = new StarfruitHarvestTest.SavedUser();
            user.setId(7); user.setSession(session); session.user = user;
            user.landItems.add(new LandItem(0, -1, 0, 0, false, false, false, null));
            user.hatgiong.add(new HatGiong(2, 2));
            java.util.Properties p = CustomCropPolicyTest.testConfig();
            CustomCropPolicy policy = CustomCropPolicy.load(p);
            java.lang.reflect.Field field = avatar.network.Session.class.getDeclaredField("farmService");
            field.setAccessible(true);
            FarmService farm = new FarmService(session, policy, Clock.fixed(Instant.ofEpochMilli(1_000_000), ZoneOffset.UTC));
            field.set(session, farm);
            FarmMsgHandler handler = new FarmMsgHandler(session);
            Message join = new Message(Cmd.JOIN); join.writer().writeInt(7);
            handler.onMessage(new Message(Cmd.JOIN, join.getData()));
            Message plant = new Message(Cmd.PLANT_SEED); plant.writer().writeInt(7); plant.writer().writeByte(0); plant.writer().writeByte(2);
            handler.onMessage(new Message(Cmd.PLANT_SEED, plant.getData()));
            handler.onMessage(new Message(Cmd.PLANT_SEED, plant.getData()));
            assertEquals(1, user.hatgiong.get(0).getSoluong());
            assertEquals(0, user.landItems.get(0).getType());
            Message harvest = new Message(Cmd.TREE_HARVEST); harvest.writer().writeInt(7); harvest.writer().writeByte(0);
            handler.onMessage(new Message(Cmd.TREE_HARVEST, harvest.getData()));
            assertTrue(user.NongSan.isEmpty());
            farm = new FarmService(session, policy, Clock.fixed(Instant.ofEpochMilli(15_400_000), ZoneOffset.UTC));
            field.set(session, farm); handler = new FarmMsgHandler(session);
            handler.onMessage(new Message(Cmd.JOIN, join.getData()));
            handler.onMessage(new Message(Cmd.TREE_HARVEST, harvest.getData()));
            handler.onMessage(new Message(Cmd.TREE_HARVEST, harvest.getData()));
            assertEquals(2, user.NongSan.get(0).getSoluong());
            assertEquals(-1, user.landItems.get(0).getType());
        });
    }
    @Test public void rejectsForeignUnsupportedEmptyAndMalformedRequests() throws Exception {
        GameplayFeedbackTest.withSession(session -> {
            StarfruitHarvestTest.SavedUser user = new StarfruitHarvestTest.SavedUser();
            user.setId(7); user.setXu(123); user.setScores(45); user.setExpMain(67); user.setSession(session); session.user = user;
            user.landItems.add(new LandItem(0, -1, 0, 0, false, false, false, null));
            user.hatgiong.add(new HatGiong(2, 1));
            java.lang.reflect.Field field = avatar.network.Session.class.getDeclaredField("farmService"); field.setAccessible(true);
            field.set(session, new FarmService(session, CustomCropPolicy.load(CustomCropPolicyTest.testConfig()), Clock.fixed(Instant.ofEpochMilli(1_000_000), ZoneOffset.UTC)));
            FarmMsgHandler handler = new FarmMsgHandler(session);
            Message join = new Message(Cmd.JOIN); join.writer().writeInt(7); handler.onMessage(new Message(Cmd.JOIN, join.getData()));
            for (int[] values : new int[][] {{8, 0, 2}, {7, 0, 3}, {7, 1, 2}}) {
                Message request = new Message(Cmd.PLANT_SEED); request.writer().writeInt(values[0]); request.writer().writeByte(values[1]); request.writer().writeByte(values[2]);
                handler.onMessage(new Message(Cmd.PLANT_SEED, request.getData()));
            }
            assertEquals(1, user.hatgiong.get(0).getSoluong()); assertEquals(-1, user.landItems.get(0).getType());
            user.hatgiong.get(0).setSoluong(0);
            Message plant = new Message(Cmd.PLANT_SEED); plant.writer().writeInt(7); plant.writer().writeByte(0); plant.writer().writeByte(2);
            handler.onMessage(new Message(Cmd.PLANT_SEED, plant.getData()));
            assertEquals(-1, user.landItems.get(0).getType());
            user.hatgiong.get(0).setSoluong(1); handler.onMessage(new Message(Cmd.PLANT_SEED, plant.getData()));
            Message malformed = new Message(Cmd.TREE_HARVEST); malformed.writer().writeInt(7); malformed.writer().writeByte(0); malformed.writer().writeByte(0);
            handler.onMessage(new Message(Cmd.TREE_HARVEST, malformed.getData()));
            assertEquals(0, user.landItems.get(0).getType()); assertTrue(user.NongSan.isEmpty());
            assertEquals(123, user.getXu()); assertEquals(45, user.getScores()); assertEquals(67, user.getExpMain());
        });
    }
}
