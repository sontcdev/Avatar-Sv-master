package org.microemu.cldc.socket;

import java.io.*;
import java.util.concurrent.*;
import okhttp3.*;
import okio.ByteString;

/** Avatar's byte stream transported through the HTTPS Cloudflare route. */
public final class WebSocketConnection extends SocketConnection {
    private static final byte[] END = new byte[0];
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .pingInterval(30, TimeUnit.SECONDS).build();
    private final BlockingQueue<byte[]> incoming = new ArrayBlockingQueue<>(128);
    private final CountDownLatch opened = new CountDownLatch(1);
    private volatile boolean closed;
    private volatile IOException failure;
    private final WebSocket webSocket;

    public WebSocketConnection() throws IOException {
        webSocket = CLIENT.newWebSocket(new Request.Builder()
                .url("wss://avatar.memora.io.vn/game").build(), new WebSocketListener() {
            @Override public void onOpen(WebSocket ws, Response response) { opened.countDown(); }
            @Override public void onMessage(WebSocket ws, ByteString data) {
                if (data.size() > 65536 || !incoming.offer(data.toByteArray())) {
                    fail(new IOException("Game receive buffer exceeded"));
                    ws.cancel();
                }
            }
            @Override public void onClosing(WebSocket ws, int code, String reason) {
                ws.close(code, null); fail(null);
            }
            @Override public void onFailure(WebSocket ws, Throwable t, Response response) {
                fail(new IOException("Game connection failed", t));
            }
            @Override public void onClosed(WebSocket ws, int code, String reason) { fail(null); }
        });
        try {
            if (!opened.await(20, TimeUnit.SECONDS)) {
                close(); throw new IOException("Game connection timed out");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); close(); throw new IOException(e);
        }
        if (closed) throw failure == null ? new IOException("Game connection closed") : failure;
    }
    private void fail(IOException error) {
        failure = error; closed = true; opened.countDown();
        if (!incoming.offer(END)) { incoming.clear(); incoming.offer(END); }
    }
    @Override public InputStream openInputStream() {
        return new InputStream() {
            byte[] current = END; int offset;
            @Override public int read() throws IOException {
                byte[] one = new byte[1]; return read(one, 0, 1) < 0 ? -1 : one[0] & 255;
            }
            @Override public int read(byte[] bytes, int start, int length) throws IOException {
                if (start < 0 || length < 0 || start > bytes.length - length) throw new IndexOutOfBoundsException();
                if (length == 0) return 0;
                while (offset == current.length) {
                    if (closed && incoming.isEmpty()) {
                        if (failure != null) throw failure;
                        return -1;
                    }
                    try { current = incoming.take(); offset = 0; }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IOException(e); }
                    if (current == END) { if (failure != null) throw failure; return -1; }
                }
                int count = Math.min(length, current.length - offset);
                System.arraycopy(current, offset, bytes, start, count); offset += count; return count;
            }
            @Override public void close() { WebSocketConnection.this.close(); }
        };
    }
    @Override public OutputStream openOutputStream() {
        return new ByteArrayOutputStream() {
            @Override public synchronized void write(int value) {
                if (count >= 65536) throw new IllegalStateException("Game send buffer exceeded");
                super.write(value);
            }
            @Override public synchronized void write(byte[] data, int offset, int length) {
                if (length > 65536 - count) throw new IllegalStateException("Game send buffer exceeded");
                super.write(data, offset, length);
            }
            @Override public synchronized void flush() throws IOException {
                if (closed) throw new IOException("Game connection closed");
                if (count > 0 && !webSocket.send(ByteString.of(toByteArray()))) throw new IOException("Game send failed");
                reset();
            }
            @Override public void close() { WebSocketConnection.this.close(); }
        };
    }
    @Override public void close() { fail(null); webSocket.cancel(); }
    @Override public String getAddress() { return "avatar.memora.io.vn"; }
    @Override public int getPort() { return 443; }
    @Override public String getLocalAddress() { return "127.0.0.1"; }
    @Override public int getLocalPort() { return 0; }
    @Override public void setSocketOption(byte option, int value) { }
    @Override public int getSocketOption(byte option) { return 0; }
}
