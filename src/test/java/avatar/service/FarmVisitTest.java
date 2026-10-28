package avatar.service;

import avatar.Farm.LandItem;
import avatar.constants.Cmd;
import avatar.network.Message;
import avatar.server.UserManager;
import org.junit.Test;
import static org.junit.Assert.*;

public class FarmVisitTest {
    @Test public void parkEntryClearsWeatherInsteadOfCreatingEmptyTimedEffect() throws Exception {
        GameplayFeedbackTest.withSession(session -> {
            avatar.model.User player = new avatar.model.User() {
                @Override public AvatarService getAvatarService() {
                    return new AvatarService(session) {
                        @Override public void enter(avatar.play.Zone zone) { }
                    };
                }
            };
            player.setId(71); player.setUsername("stage71"); player.setSession(session); session.user = player;
            avatar.play.Zone zone = new avatar.play.Zone(null, 0);
            zone.enter(player, (short) 90, (short) 65);
            Message weather = session.messages.stream().filter(m -> m.getCommand() == Cmd.WEATHER)
                    .findFirst().orElseThrow(AssertionError::new);
            // Native ir.a(-1) clears effects; ir.a(2) creates ba(2,0), which removes from an empty vector.
            assertArrayEquals(new byte[]{-1}, weather.getData());
            assertTrue(zone.getPlayers().contains(player));
        });
    }

    @Test public void farmContextLeavesParkBeforeOverlappingResourceCommands() throws Exception {
        GameplayFeedbackTest.withSession(visitor -> {
            avatar.play.Zone park = new avatar.play.Zone(null, 0);
            park.add(visitor.user); visitor.user.setZone(park);
            Message parkChat = new Message(Cmd.CHAT_PARK);
            parkChat.writer().write(java.util.HexFormat.of().parseHex(
                    "773594b9001748c3a379207468616d20676961206e676179206ec3a06f"));
            park.getService().sendMessage(parkChat);
            assertEquals(Cmd.GET_IMAGE_FARM, visitor.messages.get(0).getCommand());
            // Native gb.d treats park sender bytes as an image count, then reads past the payload.
            java.io.DataInputStream images = new java.io.DataInputStream(
                    new java.io.ByteArrayInputStream(visitor.messages.get(0).getData()));
            int count = images.readShort();
            try {
                for (int i = 0; i < count; i++) {
                    images.readShort(); images.readShort();
                    images.readByte(); images.readByte(); images.readByte(); images.readByte();
                }
                fail("Park chat must reproduce the incompatible farm image decoder EOF");
            } catch (java.io.EOFException expected) { }
            visitor.messages.clear();
            visitor.getHandler(new Message(Cmd.GET_HANDLER, new byte[]{10}));
            assertNull("Farm client must leave park broadcast recipients", visitor.user.getZone());
            assertFalse(park.getPlayers().contains(visitor.user));
            visitor.messages.clear();
            Message chat = new Message(Cmd.CHAT_PARK);
            chat.writer().writeInt(2000000001); chat.writer().writeUTF("park bot");
            park.getService().sendMessage(chat);
            assertTrue("Park chat 55 must not reach the farm image decoder", visitor.messages.isEmpty());
        });
    }

    @Test public void visitShowsOwnersFarmAndCannotHarvestVisitorsFruit() throws Exception {
        GameplayFeedbackTest.withSession(visitor -> GameplayFeedbackTest.withSession(owner -> {
            visitor.user.setId(71); owner.user.setId(92);
            visitor.user.landItems.add(new LandItem(0, -1, 0, 0, false, false, false, null));
            owner.user.setStarfruitQuantity(12);
            UserManager.getInstance().add(owner.user);
            try {
                FarmService service = new FarmService(visitor);
                Message request = new Message(Cmd.JOIN); request.writer().writeInt(92);
                service.joinFarm(new Message(Cmd.JOIN, request.getData()));
                Message response = new Message(Cmd.JOIN, visitor.messages.get(0).getData());
                assertEquals(92, response.reader().readInt());
                assertEquals(0, response.reader().readUnsignedByte());
                assertEquals(0, response.reader().readUnsignedByte());
                response.reader().skipBytes(8);
                assertEquals(12, response.reader().readShort());
                service.harvestStarfruit();
                assertEquals(180, visitor.user.getStarfruitQuantity());
                assertTrue(visitor.user.NongSanDacBiet.isEmpty());
                assertEquals(Cmd.SET_MONEY_ERROR, visitor.messages.get(1).getCommand());
            } finally { UserManager.getInstance().remove(owner.user); }
        }));
    }
}
