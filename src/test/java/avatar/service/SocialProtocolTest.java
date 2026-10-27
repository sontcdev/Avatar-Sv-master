package avatar.service;

import avatar.constants.Cmd;
import avatar.message.ParkMsgHandler;
import avatar.model.User;
import avatar.network.Message;
import avatar.server.UserManager;
import org.junit.Test;
import static org.junit.Assert.*;

public class SocialProtocolTest {
    @Test public void unsolicitedFriendAcceptanceCannotCreateFriendship() throws Exception {
        GameplayFeedbackTest.withSession(receiver -> {
            Message request = new Message(Cmd.ADD_FRIEND);
            request.writer().writeInt(71); request.writer().writeBoolean(true);
            new ParkMsgHandler(receiver).onMessage(new Message(Cmd.ADD_FRIEND,request.getData()));
            assertFalse(receiver.messages.isEmpty());
            assertEquals(Cmd.SET_MONEY_ERROR,receiver.messages.get(0).getCommand());
        });
    }
    @Test public void privateChatUsesAuthenticatedSenderIdAndName() throws Exception {
        GameplayFeedbackTest.withSession(sender -> GameplayFeedbackTest.withSession(receiver -> {
            sender.user.setId(71); sender.user.setUsername("Sender");
            receiver.user.setId(92); receiver.user.setUsername("Receiver");
            UserManager.getInstance().add(receiver.user);
            try {
                Message request = new Message(Cmd.CHAT_TO);
                request.writer().writeInt(92); request.writer().writeUTF("Xin chào");
                new ParkMsgHandler(sender).onMessage(new Message(Cmd.CHAT_TO, request.getData()));
                assertEquals(1, receiver.messages.size());
                Message response = new Message(receiver.messages.get(0).getCommand(), receiver.messages.get(0).getData());
                assertEquals(Cmd.CHAT_TO, response.getCommand());
                assertEquals(71, response.reader().readInt());
                assertEquals("Sender", response.reader().readUTF());
                assertEquals("Xin chào", response.reader().readUTF());
                assertEquals(0, response.reader().available());
            } finally { UserManager.getInstance().remove(receiver.user); }
        }));
    }
    @Test public void offlinePrivateChatReturnsFeedback() throws Exception {
        GameplayFeedbackTest.withSession(sender -> {
            Message request = new Message(Cmd.CHAT_TO);
            request.writer().writeInt(999999); request.writer().writeUTF("hello");
            sender.getAvatarService().chatToUser(new Message(Cmd.CHAT_TO, request.getData()));
            assertEquals(Cmd.SET_MONEY_ERROR, sender.messages.get(0).getCommand());
        });
    }
    @Test public void friendInvitationIdentifiesInviter() throws Exception {
        GameplayFeedbackTest.withSession(sender -> GameplayFeedbackTest.withSession(receiver -> {
            sender.user.setId(71); sender.user.setUsername("Sender"); receiver.user.setId(92);
            UserManager.getInstance().add(receiver.user);
            try {
                Message request = new Message(Cmd.AVATAR_REQUEST_ADD_FRIEND); request.writer().writeInt(92);
                new ParkMsgHandler(sender).onMessage(new Message(Cmd.AVATAR_REQUEST_ADD_FRIEND, request.getData()));
                assertEquals(1, receiver.messages.size());
                Message response = new Message(receiver.messages.get(0).getCommand(), receiver.messages.get(0).getData());
                assertEquals(Cmd.AVATAR_REQUEST_ADD_FRIEND, response.getCommand());
                assertEquals(71, response.reader().readInt());
                assertEquals("Sender", response.reader().readUTF());
                assertTrue(response.reader().readUTF().contains("Sender"));
                assertEquals(0, response.reader().available());
            } finally { UserManager.getInstance().remove(receiver.user); }
        }));
    }
}
