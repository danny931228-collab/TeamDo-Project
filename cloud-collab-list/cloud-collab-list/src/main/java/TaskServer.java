import com.cloudlist.model.TaskItem;
import com.cloudlist.net.Message;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TaskServer {
    private static final int PORT = 8888;
    private static final Set<ObjectOutputStream> clientOutputs = new HashSet<>();

    // 伺服器端的任務記憶庫
    private static final List<TaskItem> globalTasks = new ArrayList<>();
    private static int taskIdCounter = 1;

    // 💡【新功能】這行用來記錄目前在線上的組員名字，這就是人數不再是 0 的秘密！
    private static final Set<String> activeUsers = new HashSet<>();

    public static void main(String[] args) {
        System.out.println("====== TeamDo 雲端共編清單伺服器（在線人數完美版）啟動中 ======");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("伺服器已成功啟動，正在監聽通訊埠: " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("【系統通知】新成員連線加入！");

                ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());
                output.flush();
                ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream());

                synchronized (clientOutputs) {
                    clientOutputs.add(output);
                }

                new Thread(() -> handleClient(clientSocket, input, output)).start();
            }
        } catch (IOException e) {
            System.err.println("伺服器啟動失敗: " + e.getMessage());
        }
    }

    private static void handleClient(Socket socket, ObjectInputStream input, ObjectOutputStream output) {
        String clientUsername = "未知用戶"; // 用來記錄這條連線是誰的名字，斷線時才能扣人數
        try {
            while (true) {
                Object obj = input.readObject();
                if (obj instanceof Message msg) {
                    System.out.println("【收到物件】用戶: " + msg.getUsername() + ", 動作: " + msg.getType());

                    // 核心處理邏輯
                    handleBusinessLogic(msg, output);
                }
            }
        } catch (EOFException | SocketException e) {
            System.out.println("【系統通知】有組員關閉視窗中斷連線。");
        } catch (Exception e) {
            System.err.println("【核心異常】" + e.getMessage());
        } finally {
            synchronized (clientOutputs) {
                clientOutputs.remove(output);
            }
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    // 處理前端所有動作的大腦
    private static synchronized void handleBusinessLogic(Message msg, ObjectOutputStream currentOutput) throws IOException {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        switch (msg.getType()) {
            case JOIN -> {
                // 1. 收到有人點擊登入加入房間，立刻把他的名字塞進在線名單
                if (msg.getUsername() != null && !msg.getUsername().isBlank()) {
                    activeUsers.add(msg.getUsername());
                }

                // 2. 把目前的任務清單打包成 SNAPSHOT 單獨丟回給這名新加入的人
                Message snapshotMsg = new Message(Message.Type.SNAPSHOT);
                snapshotMsg.setTasks(new ArrayList<>(globalTasks));
                currentOutput.writeObject(snapshotMsg);
                currentOutput.flush();
                currentOutput.reset();

                // 3. 廣播系統聊天室文字通知所有人
                sendSystemText("歡迎 " + msg.getUsername() + " 進入房間！");

                // 4. 關鍵：打包「最新的在線名單」，廣播給所有人更新人數 UI！
                broadcastActiveUsers();
            }
            case ADD_TASK -> {
                TaskItem newTask = new TaskItem();
                newTask.setId(taskIdCounter++);
                newTask.setContent(msg.getContent());
                newTask.setCreator(msg.getUsername());
                newTask.setStatus(TaskItem.STATUS_TODO);
                newTask.setUpdatedAt(now);

                globalTasks.add(newTask);
                broadcastSnapshot();
            }
            case UPDATE_TASK -> {
                for (TaskItem item : globalTasks) {
                    if (item.getId() == msg.getTaskId()) {
                        if (msg.getContent() != null) item.setContent(msg.getContent());
                        if (msg.getStatus() != null) item.setStatus(msg.getStatus());
                        item.setUpdatedAt(now);
                        break;
                    }
                }
                broadcastSnapshot();
            }
            case DELETE_TASK -> {
                globalTasks.removeIf(item -> item.getId() == msg.getTaskId());
                broadcastSnapshot();
            }
            case LEAVE -> {
                // 有人點擊離開房間，名單移除他並更新人數
                if (msg.getUsername() != null) {
                    activeUsers.remove(msg.getUsername());
                }
                sendSystemText(msg.getUsername() + " 離開了房間。");
                broadcastActiveUsers();
            }
        }
    }

    // 廣播最新清單快照給所有人
    private static void broadcastSnapshot() {
        Message snapshot = new Message(Message.Type.SNAPSHOT);
        snapshot.setTasks(new ArrayList<>(globalTasks));
        broadcast(snapshot);
    }

    // 廣播系統文字通知
    private static void sendSystemText(String text) {
        Message sysMsg = new Message(Message.Type.SYSTEM);
        sysMsg.setText(text);
        broadcast(sysMsg);
    }

    // 💡【新功能】專門把目前「真正有誰在線上」打包發送給秉宸的 JavaFX 介面
    private static void broadcastActiveUsers() {
        Message usersMsg = new Message(Message.Type.USERS);
        usersMsg.setUsers(new HashSet<>(activeUsers));
        broadcast(usersMsg);
    }

    // 底層 Socket 廣播傳送
    private static void broadcast(Message msg) {
        synchronized (clientOutputs) {
            Iterator<ObjectOutputStream> iterator = clientOutputs.iterator();
            while (iterator.hasNext()) {
                ObjectOutputStream out = iterator.next();
                try {
                    out.writeObject(msg);
                    out.flush();
                    out.reset();
                } catch (IOException e) {
                    iterator.remove();
                }
            }
        }
    }
}