package avatar.service;

import avatar.constants.Cmd;
import avatar.db.DbManager;
import avatar.message.ParkMsgHandler;
import avatar.network.Message;
import avatar.server.UserManager;
import java.sql.*;
import java.nio.file.*;
import org.junit.*;
import static org.junit.Assert.*;

public class FriendshipPersistenceTest {
    @Test public void inviteAcceptListReloadAndDeleteUseOneMutualDurableRow() throws Exception {
        Assume.assumeTrue(Boolean.getBoolean("avatar.test.isolatedSalesDb"));
        DbManager db = DbManager.getInstance(); db.start();
        try {
            try (Connection c = db.getConnection()) {
                assertEquals("avatar2d_sales_test",c.getCatalog());
                c.createStatement().execute("DROP TABLE IF EXISTS gameplay_friendships");
                c.createStatement().execute(Files.readString(Path.of("database/migrations/20261028_friendships.sql")));
                c.createStatement().execute("CREATE TABLE IF NOT EXISTS users (id INT PRIMARY KEY, username VARCHAR(40))");
                c.createStatement().execute("REPLACE INTO users VALUES (71,'Sender'),(92,'Receiver')");
            }
            GameplayFeedbackTest.withSession(sender -> GameplayFeedbackTest.withSession(receiver -> {
                sender.user.setId(71); sender.user.setUsername("Sender"); receiver.user.setId(92); receiver.user.setUsername("Receiver");
                UserManager.getInstance().add(sender.user); UserManager.getInstance().add(receiver.user);
                try {
                    ParkMsgHandler a = new ParkMsgHandler(sender), b = new ParkMsgHandler(receiver);
                    Message invite = new Message(Cmd.AVATAR_REQUEST_ADD_FRIEND); invite.writer().writeInt(92);
                    a.onMessage(new Message(Cmd.AVATAR_REQUEST_ADD_FRIEND,invite.getData()));
                    Message accept = new Message(Cmd.ADD_FRIEND); accept.writer().writeInt(71); accept.writer().writeBoolean(true);
                    b.onMessage(new Message(Cmd.ADD_FRIEND,accept.getData()));
                    assertTrue(sender.messages.stream().anyMatch(m -> m.getCommand() == Cmd.ADD_FRIEND));
                    try (Connection c = db.getConnection(); ResultSet r = c.createStatement().executeQuery("SELECT user_low,user_high FROM gameplay_friendships")) {
                        assertTrue(r.next()); assertEquals(71,r.getInt(1)); assertEquals(92,r.getInt(2)); assertFalse(r.next());
                    }
                    // New session/service exercises persisted list, not the invitation's memory.
                    GameplayFeedbackTest.withSession(reloaded -> {
                        reloaded.user.setId(71); ParkMsgHandler h = new ParkMsgHandler(reloaded);
                        h.onMessage(new Message(Cmd.REQUEST_FRIENDLIST,new byte[0]));
                        Message list = new Message(Cmd.CUSTOM_LIST,reloaded.messages.get(0).getData());
                        assertEquals("friendlist",list.reader().readUTF()); list.reader().readUTF();
                        int listId = list.reader().readInt();
                        assertEquals(1,list.reader().readByte()); assertEquals(1,list.reader().readByte());
                        assertEquals(1,list.reader().readShort()); assertEquals(0,list.reader().readByte());
                        assertEquals(92,list.reader().readInt()); list.reader().readShort(); list.reader().readShort(); list.reader().readShort();
                        assertEquals("Receiver",list.reader().readUTF()); list.reader().readUTF();
                        assertEquals(2,list.reader().readByte());
                        for(int i=0;i<2;i++){ list.reader().readByte(); list.reader().readUTF(); }
                        assertEquals(0,list.reader().available());
                        // Captured Avatar 2.5.8 visit: list int, page byte, index short, action byte.
                        reloaded.messages.clear();
                        byte[] visit = java.util.HexFormat.of().parseHex("0000000101000002");
                        assertEquals(1,listId);
                        h.onMessage(new Message(Cmd.CUSTOM_LIST,visit));
                        assertTrue("Captured native visit must return the owner farm",reloaded.messages.stream().anyMatch(m -> m.getCommand()==Cmd.JOIN));
                        Message farm = reloaded.messages.stream().filter(m -> m.getCommand()==Cmd.JOIN).findFirst().orElseThrow();
                        assertEquals(92,new Message(Cmd.JOIN,farm.getData()).reader().readInt());
                        // A forged page must not delete the persisted friendship.
                        h.onMessage(new Message(Cmd.CUSTOM_LIST,java.util.HexFormat.of().parseHex("0000000102000001")));
                        try (Connection c = db.getConnection(); ResultSet r = c.createStatement().executeQuery("SELECT COUNT(*) FROM gameplay_friendships")) {
                            assertTrue(r.next()); assertEquals(1,r.getInt(1));
                        }
                        Message delete = new Message(Cmd.CUSTOM_LIST); delete.writer().writeInt(listId); delete.writer().writeByte(1); delete.writer().writeShort(0); delete.writer().writeByte(1);
                        h.onMessage(new Message(Cmd.CUSTOM_LIST,delete.getData()));
                        try (Connection c = db.getConnection(); ResultSet r = c.createStatement().executeQuery("SELECT COUNT(*) FROM gameplay_friendships")) {
                            assertTrue(r.next()); assertEquals(0,r.getInt(1));
                        }
                    });
                } finally { UserManager.getInstance().remove(sender.user); UserManager.getInstance().remove(receiver.user); }
            }));
        } finally { db.shutdown(); }
    }
}
