package avatar.server;

import avatar.network.Session;
import org.junit.Test;
import java.net.*;
import java.util.*;
import static org.junit.Assert.*;

public class SessionLifecycleTest {
    @Test public void failedLogoutStillRemovesUserAndClosesTransport() throws Exception {
        try (ServerSocket listener=new ServerSocket(0,1,InetAddress.getLoopbackAddress());
             Socket peer=new Socket(listener.getInetAddress(),listener.getLocalPort());
             Socket accepted=listener.accept()) {
            Session session=new Session(accepted,999);
            avatar.model.User user=new avatar.model.User() {
                @Override public void close() { throw new IllegalStateException("Synthetic failed logout"); }
            };
            user.setId(999); session.user=user; session.connected=true; session.login=true;
            avatar.server.UserManager.getInstance().add(user);
            try {
                session.close(); session.close();
                assertNull(avatar.server.UserManager.getInstance().find(999));
                assertFalse(session.connected); assertFalse(session.login); assertTrue(accepted.isClosed());
            } finally { avatar.server.UserManager.getInstance().remove(user); }
        }
    }

    @Test public void stagingTraceCapturesIncomingPayloadWithoutConsumingIt() throws Exception {
        ServerManager.clients = Collections.synchronizedList(new ArrayList<>());
        ServerManager.numClients = 1;
        java.io.PrintStream original = System.out;
        String previous = System.getProperty("avatar.protocol.trace");
        java.io.ByteArrayOutputStream trace = new java.io.ByteArrayOutputStream();
        try (ServerSocket listener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
             Socket peer = new Socket(listener.getInetAddress(), listener.getLocalPort())) {
            Session session = new Session(listener.accept(), 3);
            ServerManager.clients.add(session);
            session.user = new avatar.model.User(); session.user.setId(71);
            java.util.concurrent.CountDownLatch received = new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.atomic.AtomicInteger target = new java.util.concurrent.atomic.AtomicInteger();
            session.setHandler(new avatar.message.MessageHandler(session) {
                @Override public void onMessage(avatar.network.Message message) {
                    try { target.set(message.reader().readInt()); }
                    catch (java.io.IOException failure) { throw new AssertionError(failure); }
                    finally { received.countDown(); }
                }
            });
            try {
                System.setProperty("avatar.protocol.trace","true"); System.setOut(new java.io.PrintStream(trace));
                session.startCollector();
                java.io.DataOutputStream output = new java.io.DataOutputStream(peer.getOutputStream());
                output.writeByte(-61); output.writeShort(4); output.writeInt(92); output.flush();
                assertTrue(received.await(2,java.util.concurrent.TimeUnit.SECONDS));
                assertEquals(92,target.get());
                assertTrue(trace.toString(java.nio.charset.StandardCharsets.UTF_8).contains("cmd=-61 bytes=4 hex=0000005c"));
            } finally { session.user = null; session.close(); }
        } finally {
            System.setOut(original);
            if (previous == null) System.clearProperty("avatar.protocol.trace");
            else System.setProperty("avatar.protocol.trace",previous);
        }
    }
    @Test
    public void bossesDoNotConsumePlayerConnectionsOrRequireAListener() throws Exception {
        try (ServerSocket listener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            listener.setSoTimeout(150);
            ServerManager.server = listener;
            try {
                for (int i = 0; i < 14; i++) {
                    Session session = avatar.model.Boss.createSession(new avatar.model.Boss());
                    assertNotNull(session);
                    assertNotNull(session.getParkService());
                    session.sendMessage(new avatar.network.Message((byte) -1));
                    session.user = null;
                    session.close();
                    assertFalse(session.isConnected());
                }
                try (Socket unexpected = listener.accept()) {
                    fail("Boss consumed a player connection");
                } catch (SocketTimeoutException expected) { }
            } finally { ServerManager.server = null; }
        }
        Session session = avatar.model.Boss.createSession(new avatar.model.Boss());
        assertNotNull(session);
        session.user = null;
        session.close();
    }

    @Test
    public void disconnectBeforeHandshakeClosesSocketAndRemovesSession() throws Exception {
        ServerManager.clients = Collections.synchronizedList(new ArrayList<>());
        ServerManager.numClients = 1;
        try (ServerSocket listener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
             Socket peer = new Socket(listener.getInetAddress(), listener.getLocalPort())) {
            Session session = new Session(listener.accept(), 1);
            ServerManager.clients.add(session);
            session.startCollector();
            peer.close();
            long deadline = System.currentTimeMillis() + 2000;
            while (!ServerManager.clients.isEmpty() && System.currentTimeMillis() < deadline) Thread.sleep(10);
            session.close();
            assertTrue(ServerManager.clients.isEmpty());
            assertEquals(0, ServerManager.numClients);
            assertTrue(session.sc.isClosed());
        }
    }
    @Test
    public void negotiatesBundledClientHandshake() throws Exception {
        ServerManager.clients = Collections.synchronizedList(new ArrayList<>());
        ServerManager.numClients = 1;
        try (ServerSocket listener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
             Socket peer = new Socket(listener.getInetAddress(), listener.getLocalPort())) {
            peer.setSoTimeout(2000);
            Session session = new Session(listener.accept(), 2);
            ServerManager.clients.add(session);
            session.startCollector();
            java.io.DataOutputStream output = new java.io.DataOutputStream(peer.getOutputStream());
            output.writeByte(-27);
            output.writeShort(0);
            output.flush();
            java.io.DataInputStream input = new java.io.DataInputStream(peer.getInputStream());
            assertEquals(-27, input.readByte());
            int size = input.readUnsignedShort();
            int keySize = input.readUnsignedByte();
            assertEquals(keySize + 1, size);
            byte[] key = new byte[keySize];
            key[0] = input.readByte();
            for (int j = 1; j < keySize; j++) key[j] = (byte) (input.readByte() ^ key[j - 1]);
            assertTrue(new String(key, java.nio.charset.StandardCharsets.UTF_8).endsWith("_kitakeyos"));
            session.close();
            assertTrue(ServerManager.clients.isEmpty());
        }
    }
}
