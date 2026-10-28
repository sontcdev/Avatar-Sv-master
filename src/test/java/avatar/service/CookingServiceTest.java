package avatar.service;

import avatar.Farm.FarmCookingCatalog;
import avatar.Farm.NongSan;
import avatar.constants.Cmd;
import avatar.message.FarmMsgHandler;
import avatar.network.Message;
import avatar.server.UserManager;
import java.io.DataInputStream;
import java.lang.reflect.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class CookingServiceTest {
    @Test public void inventoryTailMatchesNativeDecoderWithNonemptyProductsAndFertilizer() throws Exception {
        GameplayFeedbackTest.withSession(session -> {
            StarfruitHarvestTest.SavedUser user=install(session);
            user.PhanBon.add(new avatar.Farm.PhanBon(5, 7));
            user.NongSanDacBiet.add(new avatar.Farm.NongSanDacBiet(207, 2));
            session.getFarmService().getInventory(null);
            DataInputStream in=new Message(Cmd.INVENTORY,session.messages.get(0).getData()).reader();
            assertEquals(0,in.readUnsignedByte());
            assertEquals(1,in.readUnsignedByte()); assertEquals(6,in.readByte()); assertEquals(2880,in.readShort());
            in.readInt(); in.readByte(); in.readByte();
            assertEquals(1,in.readUnsignedByte()); assertEquals(5,in.readShort()); assertEquals(7,in.readShort());
            assertEquals(1,in.readUnsignedByte()); assertEquals(207,in.readShort()); assertEquals(2,in.readShort());
            in.readByte(); in.readInt(); in.readBoolean(); in.readShort(); in.readByte();
            assertEquals(1,in.readUnsignedByte()); assertEquals(6,in.readShort()); assertEquals(2880,in.readInt());
            assertEquals(1,in.readUnsignedByte()); assertEquals(207,in.readShort()); assertEquals(2,in.readInt());
            assertEquals(0,in.available());
        });
    }
    private static final FarmCookingCatalog.Recipe RICE =
            new FarmCookingCatalog.Recipe(3,"Cơm",207,30,Map.of(6,1440));

    private StarfruitHarvestTest.SavedUser install(StarfruitHarvestTest.CaptureSession session) throws Exception {
        StarfruitHarvestTest.SavedUser user=new StarfruitHarvestTest.SavedUser();
        user.setId(7); user.setSession(session); session.user=user;
        user.NongSan.add(new NongSan(6,2880));
        Constructor<FarmCookingCatalog> constructor=FarmCookingCatalog.class.getDeclaredConstructor(Map.class);
        constructor.setAccessible(true);
        Field catalog=FarmService.class.getDeclaredField("cookingCatalog"); catalog.setAccessible(true);
        catalog.set(session.getFarmService(),constructor.newInstance(Map.of(3,RICE)));
        return user;
    }
    private void join(FarmMsgHandler handler,int id) throws Exception {
        Message request=new Message(Cmd.JOIN); request.writer().writeInt(id);
        handler.onMessage(new Message(Cmd.JOIN,request.getData()));
    }
    private Message cook(int id) throws Exception {
        Message request=new Message(Cmd.COOKING); request.writer().writeShort(id);
        return new Message(Cmd.COOKING,request.getData());
    }
    private void commands(StarfruitHarvestTest.CaptureSession session,byte... expected) {
        assertEquals(expected.length,session.messages.size());
        for (int i=0;i<expected.length;i++) assertEquals(expected[i],session.messages.get(i).getCommand());
    }
    private void inventory(StarfruitHarvestTest.CaptureSession session,int index,int rice,int cooked) throws Exception {
        Message packet=session.messages.get(index); assertEquals(Cmd.INVENTORY,packet.getCommand());
        DataInputStream in=new Message(packet.getCommand(),packet.getData()).reader();
        assertEquals(0,in.readByte()); assertEquals(1,in.readByte());
        assertEquals(6,in.readByte()); assertEquals(rice,in.readShort());
        in.readInt(); in.readByte(); in.readByte();
        assertEquals(0,in.readByte());
        assertEquals(cooked==0 ? 0 : 1,in.readByte());
        if (cooked>0) { assertEquals(207,in.readShort()); assertEquals(cooked,in.readShort()); }
    }
    private void joinTail(Message packet,int id,int recipe,int remaining) throws Exception {
        assertEquals(Cmd.JOIN,packet.getCommand());
        DataInputStream in=new Message(packet.getCommand(),packet.getData()).reader();
        assertEquals(id,in.readInt()); assertEquals(0,in.readByte()); assertEquals(0,in.readByte());
        assertEquals(10,in.readByte()); assertEquals(10,in.readByte());
        assertEquals(10,in.readShort()); assertEquals(43,in.readShort()); assertEquals(46,in.readShort());
        in.readShort(); assertEquals(0,in.readShort()); assertEquals(0,in.readShort()); assertEquals(0,in.readShort());
        assertEquals(recipe,in.readShort()); assertEquals(remaining,in.readShort()); assertEquals(0,in.available());
    }

    @Test public void starfruitInfoDispatchUsesDialogAndRejectsVisitorOrMalformedRequestWithoutWrites() throws Exception {
        GameplayFeedbackTest.withSession(session -> GameplayFeedbackTest.withSession(ownerSession -> {
            StarfruitHarvestTest.SavedUser user=install(session),owner=install(ownerSession);
            user.setStarfruitQuantity(12); owner.setId(92); owner.setStarfruitQuantity(77);
            FarmMsgHandler handler=new FarmMsgHandler(session);
            join(handler,7); session.messages.clear();
            handler.onMessage(new Message(Cmd.GET_INFO_STARFRUIT,new byte[0]));
            commands(session,Cmd.SET_MONEY_ERROR);
            DataInputStream dialog=new Message(Cmd.SET_MONEY_ERROR,session.messages.get(0).getData()).reader();
            String text=dialog.readUTF(); assertTrue(text.contains("12/180")); assertTrue(text.contains("60 phút"));
            assertEquals(0,dialog.available());
            session.messages.clear(); handler.onMessage(new Message(Cmd.GET_INFO_STARFRUIT,new byte[]{0}));
            commands(session,Cmd.SET_MONEY_ERROR);
            UserManager.getInstance().add(owner);
            try {
                join(handler,92); session.messages.clear();
                handler.onMessage(new Message(Cmd.GET_INFO_STARFRUIT,new byte[0]));
                commands(session,Cmd.SET_MONEY_ERROR);
                dialog=new Message(Cmd.SET_MONEY_ERROR,session.messages.get(0).getData()).reader();
                assertTrue(dialog.readUTF().contains("của mình")); assertEquals(0,dialog.available());
                assertEquals(12,user.getStarfruitQuantity()); assertEquals(77,owner.getStarfruitQuantity());
                assertEquals(0,user.getStarfruitLastHarvestMs()); assertEquals(0,owner.getStarfruitLastHarvestMs());
                assertEquals(0,user.saves); assertEquals(0,owner.saves);
                assertTrue(user.NongSanDacBiet.isEmpty()); assertTrue(owner.NongSanDacBiet.isEmpty());
            } finally { UserManager.getInstance().remove(owner); }
        }));
    }

    @Test public void cachedClientCanCookWithoutRequestingCatalogAgain() throws Exception {
        org.junit.Assume.assumeTrue(java.nio.file.Files.isRegularFile(java.nio.file.Path.of("res/data/farm_info.dat")));
        GameplayFeedbackTest.withSession(session -> {
            StarfruitHarvestTest.SavedUser user=install(session);
            Field catalog=FarmService.class.getDeclaredField("cookingCatalog"); catalog.setAccessible(true);
            catalog.set(session.getFarmService(),null);
            FarmMsgHandler handler=new FarmMsgHandler(session);
            join(handler,7); session.messages.clear(); handler.onMessage(cook(3));
            commands(session,Cmd.COOKING,Cmd.INVENTORY);
            assertEquals(1440,user.NongSan.get(0).getSoluong());
        });
    }

    @Test public void ownerHandlerStartsCookingAndSendsNative91BeforeInventory() throws Exception {
        GameplayFeedbackTest.withSession(session -> {
            StarfruitHarvestTest.SavedUser user=install(session); FarmMsgHandler handler=new FarmMsgHandler(session);
            join(handler,7); session.messages.clear();
            handler.onMessage(cook(3));
            commands(session,Cmd.COOKING,Cmd.INVENTORY);
            DataInputStream in=new Message(Cmd.COOKING,session.messages.get(0).getData()).reader();
            assertEquals(3,in.readShort()); assertEquals(30,in.readShort()); assertEquals(0,in.available());
            inventory(session,1,1440,0); assertEquals(1,user.saves);
            session.messages.clear(); handler.onMessage(cook(3));
            commands(session,Cmd.SET_MONEY_ERROR); assertEquals(1,user.saves);
            assertEquals(1440,user.NongSan.get(0).getSoluong());
        });
    }

    @Test public void prematureCollectionAndFailedPersistenceNeverSendSuccessPackets() throws Exception {
        GameplayFeedbackTest.withSession(session -> {
            StarfruitHarvestTest.SavedUser user=install(session); FarmMsgHandler handler=new FarmMsgHandler(session);
            join(handler,7); handler.onMessage(cook(3)); session.messages.clear();
            handler.onMessage(new Message(Cmd.HARVEST_COOK,new byte[0]));
            commands(session,Cmd.SET_MONEY_ERROR); assertTrue(user.NongSanDacBiet.isEmpty()); assertEquals(1,user.saves);
            // Inject only the test user's clock; no database timer or real waiting is needed.
            Field ready=avatar.model.User.class.getDeclaredField("cookingReadyMs"); ready.setAccessible(true);
            ready.setLong(user,System.currentTimeMillis()-1); user.fail=true; session.messages.clear();
            handler.onMessage(new Message(Cmd.HARVEST_COOK,new byte[0]));
            commands(session,Cmd.SET_MONEY_ERROR); assertEquals(3,user.getCookingRecipe());
            assertTrue(user.NongSanDacBiet.isEmpty());
        });
    }

    @Test public void ownerHandlerCollectsOneProductWithEmpty92ThenInventoryAndRejectsReplay() throws Exception {
        GameplayFeedbackTest.withSession(session -> {
            StarfruitHarvestTest.SavedUser user=install(session); FarmMsgHandler handler=new FarmMsgHandler(session);
            user.startCooking(RICE,System.currentTimeMillis()-1_800_001L);
            join(handler,7); session.messages.clear();
            handler.onMessage(new Message(Cmd.HARVEST_COOK,new byte[0]));
            commands(session,Cmd.HARVEST_COOK,Cmd.INVENTORY);
            assertEquals(0,session.messages.get(0).getData().length); inventory(session,1,1440,1);
            assertEquals(2,user.saves); assertEquals(0,user.getCookingRecipe());
            session.messages.clear(); handler.onMessage(new Message(Cmd.HARVEST_COOK,new byte[0]));
            commands(session,Cmd.SET_MONEY_ERROR); assertEquals(2,user.saves);
            assertEquals(1,user.NongSanDacBiet.get(0).getSoluong());
        });
    }

    @Test public void beforeJoinVisitorsAndResetContextCannotMutateCooking() throws Exception {
        GameplayFeedbackTest.withSession(visitor -> GameplayFeedbackTest.withSession(ownerSession -> {
            StarfruitHarvestTest.SavedUser user=install(visitor),owner=install(ownerSession);
            owner.setId(92); owner.startCooking(RICE,System.currentTimeMillis()-1_800_001L);
            FarmMsgHandler handler=new FarmMsgHandler(visitor);
            handler.onMessage(cook(3)); commands(visitor,Cmd.SET_MONEY_ERROR);
            UserManager.getInstance().add(owner);
            try {
                visitor.messages.clear(); join(handler,92); joinTail(visitor.messages.get(0),92,3,0);
                visitor.messages.clear(); handler.onMessage(cook(3)); commands(visitor,Cmd.SET_MONEY_ERROR);
                visitor.messages.clear(); handler.onMessage(new Message(Cmd.HARVEST_COOK,new byte[0]));
                commands(visitor,Cmd.SET_MONEY_ERROR);
                assertEquals(0,user.saves); assertEquals(2880,user.NongSan.get(0).getSoluong());
                assertTrue(user.NongSanDacBiet.isEmpty()); assertEquals(3,owner.getCookingRecipe()); assertEquals(1,owner.saves);
                join(handler,7); visitor.getFarmService().resetVisit(); visitor.messages.clear();
                handler.onMessage(cook(3)); commands(visitor,Cmd.SET_MONEY_ERROR); assertEquals(0,user.saves);
            } finally { UserManager.getInstance().remove(owner); }
        }));
    }

    @Test public void joinSerializesIdleAndActiveNativeCookingTail() throws Exception {
        GameplayFeedbackTest.withSession(session -> {
            StarfruitHarvestTest.SavedUser user=install(session); FarmMsgHandler handler=new FarmMsgHandler(session);
            join(handler,7); commands(session,Cmd.JOIN); joinTail(session.messages.get(0),7,0,0);
            user.startCooking(RICE,System.currentTimeMillis()); session.messages.clear();
            join(handler,7); commands(session,Cmd.JOIN); joinTail(session.messages.get(0),7,3,30);
        });
    }

    @Test public void malformedAndUnknownRecipesDoNotConsumeIngredientsAndNextValidPacketWorks() throws Exception {
        GameplayFeedbackTest.withSession(session -> {
            StarfruitHarvestTest.SavedUser user=install(session); FarmMsgHandler handler=new FarmMsgHandler(session);
            join(handler,7);
            for (Message request:new Message[]{new Message(Cmd.COOKING,new byte[]{0}),
                    new Message(Cmd.COOKING,new byte[]{0,3,0}),cook(999),cook(-1),
                    new Message(Cmd.HARVEST_COOK,new byte[]{0})}) {
                session.messages.clear(); handler.onMessage(request); commands(session,Cmd.SET_MONEY_ERROR);
                assertEquals(0,user.saves); assertEquals(2880,user.NongSan.get(0).getSoluong());
            }
            session.messages.clear(); handler.onMessage(cook(3)); commands(session,Cmd.COOKING,Cmd.INVENTORY);
        });
    }
}
