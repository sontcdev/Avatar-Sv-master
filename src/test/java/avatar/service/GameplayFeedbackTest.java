package avatar.service;

import avatar.constants.Cmd;
import avatar.Farm.*;
import avatar.item.PartManager;
import java.time.LocalDateTime;
import avatar.handler.GlobalHandler;
import avatar.item.Item;
import avatar.message.*;
import avatar.model.*;
import avatar.network.*;
import org.junit.Test;
import java.net.*;
import java.util.*;
import static org.junit.Assert.*;

public class GameplayFeedbackTest {
    @Test public void initialCityHandlerWorksBeforeJoiningAnyZone() throws Exception {
        withSession(session -> {
            assertNull(session.user.getZone());
            session.getHandler(new Message(Cmd.GET_HANDLER,new byte[]{8}));
            assertEquals(Cmd.GET_HANDLER,session.messages.get(0).getCommand());
            assertArrayEquals(new byte[]{8},session.messages.get(0).getData());
        });
    }

    @Test public void incompleteFarmActionsRespondInsteadOfWaiting() throws Exception {
        withSession(session -> {
            FarmMsgHandler handler = new FarmMsgHandler(session);
            for (byte cmd : new byte[]{Cmd.COOKING, Cmd.HARVEST_COOK, Cmd.NAU_NHANH,
                    Cmd.USING_ITEM, Cmd.SELL_ANIMAL, Cmd.HARVEST_ANIMAL, Cmd.UPDATE_STARFRUIL,
                    Cmd.UPDATE_STARTFRUIT_BYMONEY, Cmd.UPDATE_STORE, Cmd.STEAL}) {
                session.messages.clear();
                handler.onMessage(new Message(cmd, new byte[0]));
                assertTrue("No feedback for command " + cmd, session.messages.stream().anyMatch(m -> m.getCommand() == Cmd.SET_MONEY_ERROR));
            }
        });
    }
    @Test public void menuRejectsNegativeSelectionAndReportsUnimplementedLeaf() throws Exception {
        withSession(session -> {
            session.user.setMenus(List.of(Menu.builder().name("Chức năng thử").build()));
            GlobalHandler handler = new GlobalHandler(session.user);
            for (int select : new int[]{-1, 0}) {
                session.messages.clear();
                Message request = new Message(Cmd.MENU_OPTION);
                request.writer().writeInt(2000000001); request.writer().writeByte(0); request.writer().writeByte(select);
                handler.handleOptionMenu(new Message(Cmd.MENU_OPTION, request.getData()));
                assertFalse(session.messages.isEmpty());
            }
        });
    }
    @Test public void homeChestCountMatchesFilteredEntries() throws Exception {
        withSession(session -> {
            session.user.chests.add(new Item(40));
            session.user.chests.add(new Item(41));
            new HomeService(session).onCustomChest(null);
            Message wire = session.messages.get(0);
            Message decoded = new Message(wire.getCommand(), wire.getData());
            assertEquals(1, decoded.reader().readShort());
            assertEquals(41, decoded.reader().readShort());
            assertEquals(0, decoded.reader().readByte()); assertEquals("", decoded.reader().readUTF());
            assertEquals(0, decoded.reader().readInt()); assertEquals(1, decoded.reader().readByte());
            assertEquals(0, decoded.reader().readShort()); assertEquals(0, decoded.reader().available());
        });
    }
    @Test public void fishingWithoutBaitDoesNotBlockFollowingCommands() throws Exception {
        withSession(session -> {
            long started = System.nanoTime();
            new ParkMsgHandler(session).onMessage(new Message(Cmd.QUANG_CAU, new byte[0]));
            assertTrue("Packet collector blocked by fishing delay", System.nanoTime() - started < 1_000_000_000L);
            assertTrue(session.messages.stream().noneMatch(m -> m.getCommand() == Cmd.CAN_CAU));
        });
    }
    @Test public void validFishingCastReturnsImmediatelyAndDoesNotScheduleTwice() throws Exception {
        withSession(session -> {
            session.user.chests.add(new Item(448, -1, 1));
            ParkMsgHandler handler = new ParkMsgHandler(session);
            long started = System.nanoTime();
            try {
                handler.onMessage(new Message(Cmd.QUANG_CAU, new byte[0]));
                handler.onMessage(new Message(Cmd.QUANG_CAU, new byte[0]));
                assertTrue(System.nanoTime() - started < 1_000_000_000L);
                assertEquals(1, session.messages.stream().filter(m -> m.getCommand() == Cmd.QUANG_CAU).count());
            } finally { session.getParkService().cancelFishing(); }
        });
    }
    @Test public void invalidSeedAndMissingShopDataReturnFeedback() throws Exception {
        withSession(session -> {
            FarmMsgHandler handler = new FarmMsgHandler(session);
            Message join = new Message(Cmd.JOIN); join.writer().writeInt(7);
            handler.onMessage(new Message(Cmd.JOIN, join.getData()));
            session.messages.clear();
            Message plant = new Message(Cmd.PLANT_SEED);
            plant.writer().writeInt(7); plant.writer().writeByte(0); plant.writer().writeByte(1);
            handler.onMessage(new Message(Cmd.PLANT_SEED, plant.getData()));
            assertEquals(Cmd.SET_MONEY_ERROR, session.messages.get(0).getCommand());
            session.messages.clear();
            Message buy = new Message(Cmd.BUY_ITEM);
            buy.writer().writeShort(123); buy.writer().writeByte(1); buy.writer().writeByte(1);
            handler.onMessage(new Message(Cmd.BUY_ITEM, buy.getData()));
            assertEquals(Cmd.SET_MONEY_ERROR, session.messages.get(0).getCommand());
        });
    }
    @Test public void cropHarvestCreditsInventoryOnceInsteadOfJustClearingTheLand() throws Exception {
        withSession(session -> {
            StarfruitHarvestTest.SavedUser user = new StarfruitHarvestTest.SavedUser();
            user.setId(7); user.setSession(session); session.user = user;
            user.landItems.add(new LandItem(0, 5, 100, 0, false, false, true, LocalDateTime.now().minusMinutes(10)));
            farmItem crop = new farmItem(5, "Test crop", 5, 120, 20);
            PartManager.getInstance().getFarmItems().add(crop);
            try {
                FarmMsgHandler handler = new FarmMsgHandler(session);
                Message join = new Message(Cmd.JOIN); join.writer().writeInt(7);
                handler.onMessage(new Message(Cmd.JOIN, join.getData()));
                Message harvest = new Message(Cmd.TREE_HARVEST); harvest.writer().writeInt(7); harvest.writer().writeByte(0);
                handler.onMessage(new Message(Cmd.TREE_HARVEST, harvest.getData()));
                handler.onMessage(new Message(Cmd.TREE_HARVEST, harvest.getData()));
                assertEquals(120, user.NongSan.get(0).getSoluong());
                assertEquals(-1, user.landItems.get(0).getType());
                assertEquals(1, user.saves);
            } finally { PartManager.getInstance().getFarmItems().remove(crop); }
        });
    }
    @Test public void houseRejectsUnaffordablePurchaseAndMissingChestItem() throws Exception {
        withSession(session -> {
            HomeMsgHandler handler = new HomeMsgHandler(session);
            Message buy = new Message(Cmd.BUY_ITEM_HOUSE);
            buy.writer().writeShort(1); buy.writer().writeByte(0); buy.writer().writeByte(0); buy.writer().writeByte(0);
            handler.onMessage(new Message(Cmd.BUY_ITEM_HOUSE, buy.getData()));
            assertEquals(0, session.user.getXu());
            assertEquals(Cmd.SET_MONEY_ERROR, session.messages.get(0).getCommand());
            session.messages.clear();
            Message transfer = new Message(Cmd.TRANS_PART_CHEST);
            transfer.writer().writeByte(1); transfer.writer().writeShort(0); transfer.writer().writeShort(1);
            handler.onMessage(new Message(Cmd.TRANS_PART_CHEST, transfer.getData()));
            assertEquals(Cmd.SET_MONEY_ERROR, session.messages.get(0).getCommand());
            assertFalse(session.user.chests.contains(null));
        });
    }
    @Test public void malformedActionReportsFailureAndNextRequestStillWorks() throws Exception {
        withSession(session -> {
            FarmMsgHandler handler = new FarmMsgHandler(session);
            handler.onMessage(new Message(Cmd.PLANT_SEED, new byte[0]));
            assertEquals(Cmd.SET_MONEY_ERROR, session.messages.get(0).getCommand());
            handler.onMessage(new Message(Cmd.INVENTORY, new byte[0]));
            assertTrue(session.messages.stream().anyMatch(m -> m.getCommand() == Cmd.INVENTORY));
        });
    }
    @Test public void unsupportedNewGameReportsInsteadOfWaiting() throws Exception {
        withSession(session -> {
            session.user = null;
            new MessageHandler(session).onMessage(new Message(Cmd.LOGIN_NEW_GAME, new byte[0]));
            assertEquals(Cmd.SET_MONEY_ERROR, session.messages.get(0).getCommand());
        });
    }
    @Test public void farmPurchaseUsesSelectedCurrencyAndMergesSeedStock() throws Exception {
        withSession(session -> {
            session.user.setXu(1000); session.user.setLuong(100);
            session.user.hatgiong.add(new HatGiong(5, 2));
            farmItem seed = new farmItem(5, "Test seed", 5, 10, 20);
            PartManager.getInstance().getFarmItems().add(seed);
            try {
                Message purchase = new Message(Cmd.BUY_ITEM);
                purchase.writer().writeShort(5); purchase.writer().writeByte(2); purchase.writer().writeByte(0);
                new FarmMsgHandler(session).onMessage(new Message(Cmd.BUY_ITEM, purchase.getData()));
                assertEquals(1000, session.user.getXu()); assertEquals(60, session.user.getLuong());
                assertEquals(1, session.user.hatgiong.size()); assertEquals(4, session.user.hatgiong.get(0).getSoluong());
            } finally { PartManager.getInstance().getFarmItems().remove(seed); }
        });
    }
    @Test public void failedCropSaveRestoresLandAndStock() throws Exception {
        StarfruitHarvestTest.SavedUser user = new StarfruitHarvestTest.SavedUser(); user.fail = true;
        user.landItems.add(new LandItem(0, 5, 100, 0, false, false, true, LocalDateTime.now().minusMinutes(10)));
        farmItem crop = new farmItem(5, "Test crop", 5, 10, 20);
        PartManager.getInstance().getFarmItems().add(crop);
        try {
            try { user.harvestCrop(0); fail("Expected database failure"); }
            catch (java.sql.SQLException expected) { }
            assertEquals(5, user.landItems.get(0).getType()); assertTrue(user.NongSan.isEmpty());
        } finally { PartManager.getInstance().getFarmItems().remove(crop); }
    }
    @Test public void missingAndUnimplementedNpcsReturnFeedback() throws Exception {
        withSession(session -> {
            avatar.handler.NpcHandler.handlerCommunicate(Npc.ID_ADD + 999, session.user);
            assertEquals(Cmd.SET_MONEY_ERROR, session.messages.get(0).getCommand());
            session.messages.clear();
            avatar.play.Zone zone = new avatar.play.Zone(null, 0);
            session.user.setZone(zone);
            User npc = new User(); npc.setId(Npc.ID_ADD + 999); zone.add(npc);
            avatar.handler.NpcHandler.handlerCommunicate(npc.getId(), session.user);
            assertEquals(Cmd.SET_MONEY_ERROR, session.messages.get(0).getCommand());
        });
    }
    @Test public void openingLegacyMenuClearsPreviousNpcActions() throws Exception {
        withSession(session -> {
            int[] calls = {0};
            session.user.setMenus(List.of(Menu.builder().name("old action").action(() -> calls[0]++).build()));
            session.getAvatarService().openMenuOption(1000, 1, "a", "b", "c");
            Message selection = new Message(Cmd.MENU_OPTION);
            selection.writer().writeInt(1000); selection.writer().writeByte(1); selection.writer().writeByte(2);
            new GlobalHandler(session.user).handleOptionMenu(new Message(Cmd.MENU_OPTION, selection.getData()));
            assertEquals(0, calls[0]);
            assertTrue(session.messages.stream().anyMatch(m -> m.getCommand() == Cmd.TEXT_BOX));
        });
    }
    @Test public void openingActionMenuRegistersCurrentActions() throws Exception {
        withSession(session -> {
            int[] calls = {0};
            session.getAvatarService().openMenuOption(2000000001, 0,
                    List.of(Menu.builder().name("action").action(() -> calls[0]++).build()));
            Message selection = new Message(Cmd.MENU_OPTION);
            selection.writer().writeInt(2000000001); selection.writer().writeByte(0); selection.writer().writeByte(0);
            new GlobalHandler(session.user).handleOptionMenu(new Message(Cmd.MENU_OPTION, selection.getData()));
            assertEquals(1, calls[0]);
        });
    }
    interface Check { void run(StarfruitHarvestTest.CaptureSession session) throws Exception; }
    static void withSession(Check check) throws Exception {
        try (ServerSocket listener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
             Socket peer = new Socket(listener.getInetAddress(), listener.getLocalPort()); Socket accepted = listener.accept()) {
            StarfruitHarvestTest.CaptureSession session = new StarfruitHarvestTest.CaptureSession(accepted);
            User user = new User(); user.setId(7); user.setSession(session); session.user = user;
            check.run(session);
        }
    }
}
