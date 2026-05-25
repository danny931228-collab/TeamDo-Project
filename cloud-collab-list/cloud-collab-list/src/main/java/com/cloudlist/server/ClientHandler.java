package com.cloudlist.server;

import com.cloudlist.net.Message;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.SQLException;

/**
 * 單一客戶端連線處理器。
 * 每個 Client 使用一條 Background Thread 監聽訊息，避免阻塞其他使用者。
 */
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final CloudListServer server;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private String username;
    private String roomId;
    private volatile boolean running = true;

    public ClientHandler(Socket socket, CloudListServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            input = new ObjectInputStream(socket.getInputStream());

            while (running) {
                Object obj = input.readObject();
                if (!(obj instanceof Message msg)) {
                    send(Message.error("收到未知封包格式"));
                    continue;
                }

                if (msg.getType() == Message.Type.JOIN) {
                    String safeUsername = safeText(msg.getUsername(), "Guest" + socket.getPort());
                    String safeRoom = safeText(msg.getRoomId(), "main");
                    server.onJoin(this, safeUsername, safeRoom);
                } else {
                    server.onMessage(this, msg);
                }
            }
        } catch (EOFException ex) {
            // Client 正常關閉視窗或斷線。
        } catch (IOException | ClassNotFoundException | SQLException ex) {
            System.err.println("Client 連線處理錯誤：" + ex.getMessage());
            send(Message.error("Server 錯誤：" + ex.getMessage()));
        } finally {
            running = false;
            server.disconnect(this);
        }
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    public synchronized void send(Message msg) {
        if (output == null) return;
        try {
            output.writeObject(msg);
            output.flush();
            output.reset();
        } catch (IOException ex) {
            running = false;
        }
    }

    public void closeQuietly() {
        running = false;
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }
}
