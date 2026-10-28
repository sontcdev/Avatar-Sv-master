package avatar.service;

import avatar.constants.Cmd;
import avatar.handler.NpcHandler;
import avatar.handler.GlobalHandler;
import avatar.model.User;
import avatar.network.Message;
import avatar.play.Map;
import avatar.play.Zone;
import org.junit.Test;
import static org.junit.Assert.*;

public class CustomEnglishQuizTest {
    @Test public void approvedQuizScoresThreeAnswersAndRejectsStaleCallbacks() throws Exception {
        System.setProperty("avatar.customEnglishQuizV1", "true");
        try {
            GameplayFeedbackTest.withSession(s -> {
                s.connected = true; s.login = true;
                Map map = new Map(6, 0, 1) { @Override public void load() {} };
                Zone zone = map.getZones().get(0); User npc = new User();
                npc.setId(2000000645); npc.setX((short)150); npc.setY((short)100); zone.add(npc);
                s.user.setZone(zone); s.user.setX((short)150); s.user.setY((short)100);
                NpcHandler.handlerCommunicate(2000000645, s.user);
                assertEquals(Cmd.MENU_OPTION, s.messages.get(s.messages.size()-1).getCommand());
                Message menu = new Message(Cmd.MENU_OPTION, s.messages.get(s.messages.size()-1).getData());
                assertEquals(2000000645, menu.reader().readInt()); int start = menu.reader().readUnsignedByte();
                select(s.user, start, 0); int first = menuId(s); assertQuestion(s, "Cat là gì?");
                select(s.user, first, 0); int second = menuId(s); assertQuestion(s, "Dog là gì?");
                select(s.user, first, 0); assertEquals(second, menuId(s));
                select(s.user, second, 1); int third = menuId(s); assertQuestion(s, "Hello nghĩa là gì?");
                select(s.user, third, 0);
                Message result = s.messages.get(s.messages.size()-1);
                assertEquals(Cmd.SET_MONEY_ERROR, result.getCommand());
                assertTrue(new Message(result.getCommand(), result.getData()).reader().readUTF().contains("2/3"));
            });
        } finally { System.clearProperty("avatar.customEnglishQuizV1"); }
    }
    @Test public void exitWrongContextDisconnectAndFeatureFlag() throws Exception {
        System.setProperty("avatar.customEnglishQuizV1", "true");
        try {
            GameplayFeedbackTest.withSession(s -> {
                s.connected = true; s.login = true;
                Map map = new Map(6, 0, 1) { @Override public void load() {} };
                Zone zone = map.getZones().get(0); User npc = new User(); npc.setId(2000000645); npc.setX((short)150); npc.setY((short)100); zone.add(npc);
                s.user.setZone(zone); s.user.setX((short)150); s.user.setY((short)100);
                s.user.setXu(123); int inventory = s.user.chests.size();
                s.customEnglishQuiz.open(); int initial = menuId(s); select(s.user, initial, 0); int first = menuId(s);
                int count = s.messages.size(); select(s.user, first + 1, 0); select(s.user, first, 4); assertEquals(count, s.messages.size());
                select(s.user, first, 3); select(s.user, first, 0); assertEquals(count, s.messages.size());
                s.customEnglishQuiz.open(); initial = menuId(s); select(s.user, initial, 0); first = menuId(s);
                Message wrong = new Message(Cmd.MENU_OPTION); wrong.writer().writeInt(2000000646); wrong.writer().writeByte(first); wrong.writer().writeByte(0);
                new GlobalHandler(s.user).handleOptionMenu(new Message(Cmd.MENU_OPTION, wrong.getData()));
                count = s.messages.size(); select(s.user, first, 0); assertEquals(count, s.messages.size());
                s.customEnglishQuiz.open(); initial = menuId(s); select(s.user, initial, 0); first = menuId(s);
                s.user.setX((short)300); select(s.user, first, 0); assertEquals(first, menuId(s));
                s.user.setX((short)150); select(s.user, first, 0); assertEquals(first, menuId(s));
                s.customEnglishQuiz.open(); initial = menuId(s); select(s.user, initial, 0); first = menuId(s);
                s.user.setZone(new Map(7, 0, 1) { @Override public void load() {} }.getZones().get(0));
                select(s.user, first, 0); s.user.setZone(zone); select(s.user, first, 0); assertEquals(first, menuId(s));
                s.customEnglishQuiz.open(); initial = menuId(s); select(s.user, initial, 0); first = menuId(s);
                s.customEnglishQuiz.clear(); s.login = false; s.connected = false; select(s.user, first, 0);
                s.connected = true; s.login = true; select(s.user, first, 0); assertEquals(first, menuId(s));
                assertEquals(123, s.user.getXu()); assertEquals(inventory, s.user.chests.size());
                System.clearProperty("avatar.customEnglishQuizV1"); count = s.messages.size(); s.customEnglishQuiz.open(); assertEquals(count, s.messages.size());
            });
        } finally { System.clearProperty("avatar.customEnglishQuizV1"); }
    }
    @Test public void sessionsDoNotShareProgress() throws Exception {
        System.setProperty("avatar.customEnglishQuizV1", "true");
        try { GameplayFeedbackTest.withSession(a -> GameplayFeedbackTest.withSession(b -> {
            Map map = new Map(6, 0, 1) { @Override public void load() {} }; Zone z = map.getZones().get(0);
            User npc = new User(); npc.setId(2000000645); z.add(npc);
            for (StarfruitHarvestTest.CaptureSession s : new StarfruitHarvestTest.CaptureSession[]{a,b}) {
                s.connected=true; s.login=true; s.user.setZone(z); s.customEnglishQuiz.open();
            }
            int aStart=menuId(a), bStart=menuId(b); select(a.user,aStart,0);
            assertEquals(bStart,menuId(b)); select(b.user,bStart,1); int size=b.messages.size();
            select(b.user,bStart,0); assertEquals(size,b.messages.size());
        })); } finally { System.clearProperty("avatar.customEnglishQuizV1"); }
    }
    @Test public void closingSessionClearsStateAndAReconnectionStartsEmpty() throws Exception {
        System.setProperty("avatar.customEnglishQuizV1", "true");
        try {
            User user = new User(); avatar.network.Session session = avatar.network.Session.forInternalActor(user); user.setSession(session);
            Map map = new Map(6, 0, 1) { @Override public void load() {} }; Zone zone = map.getZones().get(0);
            User npc = new User(); npc.setId(2000000645); zone.add(npc); user.setZone(zone);
            session.customEnglishQuiz.open(); assertNotNull(user.getMenus());
            user.setZone(null); session.close(); assertNull(user.getMenus());
            avatar.network.Session reconnect = avatar.network.Session.forInternalActor(user); user.setSession(reconnect); user.setZone(zone);
            reconnect.customEnglishQuiz.select(0, 0); assertNull(user.getMenus()); user.setZone(null); reconnect.close();
        } finally { System.clearProperty("avatar.customEnglishQuizV1"); }
    }
    @Test public void isolatedDatabaseRemainsUnchanged() throws Exception {
        org.junit.Assume.assumeTrue(Boolean.getBoolean("avatar.test.isolatedSalesDb"));
        avatar.db.DbManager db=avatar.db.DbManager.getInstance(); db.start();
        System.setProperty("avatar.customEnglishQuizV1", "true");
        try (java.sql.Connection c=db.getConnection()) {
            assertEquals("avatar2d_sales_test",c.getCatalog());
            try (java.sql.Statement fixture=c.createStatement()) {
                fixture.execute("CREATE TABLE IF NOT EXISTS players (user_id INT PRIMARY KEY, xu BIGINT NOT NULL, persistence_revision BIGINT NOT NULL DEFAULT 0)");
                fixture.execute("CREATE TABLE IF NOT EXISTS farm_data (user_id INT PRIMARY KEY, land_data LONGTEXT, persistence_revision BIGINT NOT NULL DEFAULT 0)");
                fixture.execute("INSERT IGNORE INTO players (user_id,xu) VALUES (119997,555)");
                fixture.execute("INSERT IGNORE INTO farm_data (user_id,land_data) VALUES (119997,'quiz fixture')");
            }
            String players=snapshot(c,"players"), farms=snapshot(c,"farm_data");
            GameplayFeedbackTest.withSession(s -> {
                Map map=new Map(6,0,1) { @Override public void load() {} }; Zone z=map.getZones().get(0);
                User npc=new User(); npc.setId(2000000645); z.add(npc);
                s.connected=true; s.login=true; s.user.setId(119997); s.user.setZone(z); s.user.setXu(555); int inventory=s.user.chests.size();
                s.customEnglishQuiz.open(); select(s.user,menuId(s),0); select(s.user,menuId(s),0);
                select(s.user,menuId(s),0); select(s.user,menuId(s),0);
                assertEquals(555,s.user.getXu()); assertEquals(inventory,s.user.chests.size());
            });
            assertEquals(players,snapshot(c,"players")); assertEquals(farms,snapshot(c,"farm_data"));
        } finally { System.clearProperty("avatar.customEnglishQuizV1"); db.shutdown(); }
    }
    private static String snapshot(java.sql.Connection c,String table) throws Exception {
        StringBuilder result=new StringBuilder();
        try (java.sql.Statement statement=c.createStatement(); java.sql.ResultSet rows=statement.executeQuery("SELECT * FROM "+table+" ORDER BY user_id")) {
            int columns=rows.getMetaData().getColumnCount();
            while(rows.next()) for(int i=1;i<=columns;i++) result.append(java.util.Objects.toString(rows.getString(i),"<NULL>")).append('\u0000');
        }
        return result.toString();
    }
    private static void assertQuestion(StarfruitHarvestTest.CaptureSession session, String question) throws Exception {
        Message wire=session.messages.get(session.messages.size()-1);
        Message decoded=new Message(wire.getCommand(),wire.getData());
        decoded.reader().readInt(); decoded.reader().readByte(); int count=decoded.reader().readUnsignedByte();
        for (int i=0;i<count;i++) decoded.reader().readUTF();
        for (int i=0;i<count;i++) decoded.reader().readShort();
        assertEquals("Cô giáo tiếng Anh", decoded.reader().readUTF()); assertEquals(question, decoded.reader().readUTF());
    }
    private static int menuId(StarfruitHarvestTest.CaptureSession s) throws Exception {
        Message m = s.messages.get(s.messages.size()-1);
        assertEquals(Cmd.MENU_OPTION, m.getCommand());
        Message decoded = new Message(m.getCommand(), m.getData()); decoded.reader().readInt();
        return decoded.reader().readUnsignedByte();
    }
    private static void select(User user, int menu, int choice) throws Exception {
        Message m = new Message(Cmd.MENU_OPTION); m.writer().writeInt(2000000645); m.writer().writeByte(menu); m.writer().writeByte(choice);
        new GlobalHandler(user).handleOptionMenu(new Message(Cmd.MENU_OPTION, m.getData()));
    }
}
