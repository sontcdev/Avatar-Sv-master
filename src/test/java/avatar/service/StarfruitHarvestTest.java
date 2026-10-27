package avatar.service;

import avatar.Farm.NongSanDacBiet;
import avatar.constants.Cmd;
import avatar.message.FarmMsgHandler;
import avatar.model.User;
import avatar.network.Message;
import avatar.network.Session;
import org.junit.Test;
import java.net.*;
import java.sql.SQLException;
import java.util.*;
import static org.junit.Assert.*;

public class StarfruitHarvestTest {
    static class SavedUser extends User {
        int saves;
        boolean fail;
        @Override public synchronized void saveFarmData(int id) throws SQLException {
            if (fail) throw new SQLException("test write failure");
            saves++;
        }
    }
    static class CaptureSession extends Session {
        final List<Message> messages = new ArrayList<>();
        CaptureSession(Socket socket) throws Exception { super(socket, 1); }
        @Override public void sendMessage(Message message) { messages.add(message); }
    }
    @Test public void handlerHarvestsOnceAndRefreshesFarmAndInventory() throws Exception {
        try (ServerSocket listener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
             Socket peer = new Socket(listener.getInetAddress(), listener.getLocalPort());
             Socket accepted = listener.accept()) {
            CaptureSession session = new CaptureSession(accepted);
            SavedUser user = new SavedUser(); user.setId(7); user.setSession(session); session.user = user;
            FarmMsgHandler handler = new FarmMsgHandler(session);
            Message join = new Message(Cmd.JOIN); join.writer().writeInt(7);
            handler.onMessage(new Message(Cmd.JOIN, join.getData()));
            handler.onMessage(new Message(Cmd.HARVEST_STARFRUIT, new byte[0]));
            assertEquals(0, user.getStarfruitQuantity());
            assertEquals(180, user.NongSanDacBiet.get(0).getSoluong());
            assertEquals(1, user.saves);
            assertTrue(session.messages.stream().anyMatch(m -> m.getCommand() == 60));
            handler.onMessage(new Message(Cmd.HARVEST_STARFRUIT, new byte[0]));
            assertEquals(180, user.NongSanDacBiet.get(0).getSoluong());
            assertEquals(1, user.saves);
        }
    }
    @Test public void failedSaveRestoresFruitAndExistingStock() throws Exception {
        SavedUser user = new SavedUser(); user.fail = true;
        user.NongSanDacBiet.add(new NongSanDacBiet(215, 12));
        try { user.harvestStarfruit(); fail("Expected database failure"); }
        catch (SQLException expected) { }
        assertEquals(180, user.getStarfruitQuantity());
        assertEquals(12, user.NongSanDacBiet.get(0).getSoluong());
    }
    @Test public void concurrentClicksCreditOnlyOnce() throws Exception {
        SavedUser user = new SavedUser();
        java.util.concurrent.ExecutorService workers = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Future<Integer> a = workers.submit(() -> user.harvestStarfruit());
            java.util.concurrent.Future<Integer> b = workers.submit(() -> user.harvestStarfruit());
            assertEquals(180, a.get() + b.get());
            assertEquals(1, user.saves);
        } finally { workers.shutdownNow(); }
    }
}
