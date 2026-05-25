package com.cloudlist.server;

import com.cloudlist.model.TaskItem;
import com.cloudlist.net.Message;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 雲端共編清單 Server。
 * 負責：連線管理、任務資料持久化、任務 ID 分配、即時廣播同步。
 */
public class CloudListServer {
    private final int port;
    private final String dbPath;
    private final DatabaseManager database;
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final Map<String, Set<String>> roomUsers = new ConcurrentHashMap<>();

    public CloudListServer(int port, String dbPath) throws SQLException {
        this.port = port;
        this.dbPath = dbPath;
        this.database = new DatabaseManager(dbPath);
    }

    public static void main(String[] args) {
        int port = args.length >= 1 ? Integer.parseInt(args[0]) : 5555;
        String dbPath = args.length >= 2 ? args[1] : "cloud_list.db";

        try {
            CloudListServer server = new CloudListServer(port, dbPath);
            server.start();
        } catch (Exception ex) {
            System.err.println("Server 啟動失敗：" + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public void start() throws IOException {
        System.out.println("雲端共編清單 Server 已啟動，Port = " + port);
        System.out.println("資料庫位置：" + new File(dbPath).getAbsolutePath());
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket, this);
                clients.add(handler);
                new Thread(handler, "client-handler-" + socket.getPort()).start();
            }
        }
    }

    public void onJoin(ClientHandler handler, String username, String roomId) throws SQLException {
        handler.setUsername(username);
        handler.setRoomId(roomId);

        roomUsers.computeIfAbsent(roomId, key -> ConcurrentHashMap.newKeySet()).add(username);
        System.out.printf("%s 加入房間 %s%n", username, roomId);

        broadcastSystem(roomId, username + " 已加入協作空間");
        broadcastUsers(roomId);
        sendSnapshotToRoom(roomId);
    }

    public void onMessage(ClientHandler handler, Message msg) throws SQLException {
        String roomId = handler.getRoomId();
        if (roomId == null || roomId.isBlank()) {
            handler.send(Message.error("尚未加入房間，無法執行操作"));
            return;
        }

        switch (msg.getType()) {
            case ADD_TASK -> {
                String content = msg.getContent() == null ? "" : msg.getContent().trim();
                if (content.isBlank()) {
                    handler.send(Message.error("任務內容不可空白"));
                    return;
                }
                TaskItem task = database.addTask(roomId, content, handler.getUsername());
                broadcastSystem(roomId, handler.getUsername() + " 新增任務 #" + task.getId());
                sendSnapshotToRoom(roomId);
            }
            case UPDATE_TASK -> {
                String content = msg.getContent();
                String status = normalizeStatus(msg.getStatus());
                database.updateTask(msg.getTaskId(), content, status);
                broadcastSystem(roomId, handler.getUsername() + " 更新任務 #" + msg.getTaskId());
                sendSnapshotToRoom(roomId);
            }
            case DELETE_TASK -> {
                database.deleteTask(msg.getTaskId());
                broadcastSystem(roomId, handler.getUsername() + " 刪除任務 #" + msg.getTaskId());
                sendSnapshotToRoom(roomId);
            }
            case LEAVE -> disconnect(handler);
            default -> handler.send(Message.error("Server 無法處理此訊息類型：" + msg.getType()));
        }
    }

    private String normalizeStatus(String status) {
        if (TaskItem.STATUS_DOING.equals(status) || TaskItem.STATUS_DONE.equals(status)) {
            return status;
        }
        if (TaskItem.STATUS_TODO.equals(status)) {
            return status;
        }
        return null;
    }

    public void disconnect(ClientHandler handler) {
        boolean wasConnected = clients.remove(handler);
        String roomId = handler.getRoomId();
        String username = handler.getUsername();

        if (wasConnected && roomId != null && username != null) {
            Set<String> users = roomUsers.get(roomId);
            if (users != null) {
                users.remove(username);
                if (users.isEmpty()) {
                    roomUsers.remove(roomId);
                }
            }
            broadcastSystem(roomId, username + " 已離開協作空間");
            broadcastUsers(roomId);
        }
        handler.closeQuietly();
    }

    private void sendSnapshotToRoom(String roomId) throws SQLException {
        Message msg = new Message(Message.Type.SNAPSHOT);
        msg.setRoomId(roomId);
        msg.setTasks(database.getTasks(roomId));
        broadcast(roomId, msg);
    }

    private void broadcastUsers(String roomId) {
        Message msg = new Message(Message.Type.USERS);
        msg.setRoomId(roomId);
        msg.setUsers(new LinkedHashSet<>(roomUsers.getOrDefault(roomId, Set.of())));
        broadcast(roomId, msg);
    }

    private void broadcastSystem(String roomId, String text) {
        Message msg = Message.system(text);
        msg.setRoomId(roomId);
        broadcast(roomId, msg);
    }

    private void broadcast(String roomId, Message msg) {
        List<ClientHandler> sameRoomClients = new ArrayList<>();
        for (ClientHandler client : clients) {
            if (roomId != null && roomId.equals(client.getRoomId())) {
                sameRoomClients.add(client);
            }
        }
        for (ClientHandler client : sameRoomClients) {
            client.send(msg);
        }
    }
}
