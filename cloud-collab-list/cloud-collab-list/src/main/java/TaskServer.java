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

    // 伺服器端的任務記憶庫（模擬資料庫功能，保證重新整理或新組員進來時都看得到歷史清單）
    private static final List<TaskItem> globalTasks = new ArrayList<>();
    // 專門發放任務 ID 的計數器
    private static int taskIdCounter = 1;

    public static void main(String[] args) {
        System.out.println("====== TeamDo 雲端共編清單伺服器（SNAPSHOT 終極完美版）啟動中 ======");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("伺服器已成功啟動，正在監聽通訊埠: " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("【系統通知】新成員連線加入！來自：" + clientSocket.getRemoteSocketAddress());

                ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());
                output.flush();
                ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream());

                synchronized (clientOutputs) {
                    clientOutputs.add(output);
                }

                // 為新連線開執行緒
                new Thread(() -> handleClient(clientSocket, input, output)).start();
            }
        } catch (IOException e) {
            System.err.println("伺服器啟動失敗: " + e.getMessage());
        }
    }

    private static void handleClient(Socket socket, ObjectInputStream input, ObjectOutputStream output) {
        try {
            while (true) {
                Object obj = input.readObject();
                if (obj instanceof Message msg) {
                    System.out.println("【收到物件】用戶: " + msg.getUsername() + ", 動作: " + msg.getType());

                    // 根據秉宸前端設計的 Message.Type 進行核心業務邏輯處理
                    handleBusinessLogic(msg, output);
                }
            }
        } catch (EOFException | SocketException e) {
            System.out.println("【系統通知】有組員關閉視窗，中斷連線。");
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

    // 處理前端所有按鈕傳過來的核心大腦邏輯
    private static synchronized void handleBusinessLogic(Message msg, ObjectOutputStream currentOutput) throws IOException {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        switch (msg.getType()) {
            case JOIN -> {
                // 當有人加入房間時，立刻把目前的任務清單打包成 SNAPSHOT 丟單獨回給他
                Message snapshotMsg = new Message(Message.Type.SNAPSHOT);
                snapshotMsg.setTasks(new ArrayList<>(globalTasks));
                currentOutput.writeObject(snapshotMsg);
                currentOutput.flush();
                currentOutput.reset();

                // 同時通知所有人有新成員，刷新在線名單（這裡簡化通知）
                sendSystemText("歡迎 " + msg.getUsername() + " 進入房間！");
            }
            case ADD_TASK -> {
                // 1. 新增任務：收到內容，在伺服器端實體化一個 TaskItem 物件
                TaskItem newTask = new TaskItem();
                newTask.setId(taskIdCounter++);
                newTask.setContent(msg.getContent());
                newTask.setCreator(msg.getUsername());
                newTask.setStatus(TaskItem.STATUS_TODO);
                newTask.setUpdatedAt(now);

                // 2. 存入伺服器庫
                globalTasks.add(newTask);
                System.out.println("【新增成功】目前總任務數：" + globalTasks.size());

                // 3. 關鍵廣播：打包最新的清單快照，發給「所有人」刷新視窗！
                broadcastSnapshot();
            }
            case UPDATE_TASK -> {
                // 修改或勾選任務
                for (TaskItem item : globalTasks) {
                    if (item.getId() == msg.getTaskId()) {
                        if (msg.getContent() != null) item.setContent(msg.getContent()); // 編輯內文
                        if (msg.getStatus() != null) item.setStatus(msg.getStatus());   // 變更狀態
                        item.setUpdatedAt(now);
                        break;
                    }
                }
                broadcastSnapshot();
            }
            case DELETE_TASK -> {
                // 刪除任務
                globalTasks.removeIf(item -> item.getId() == msg.getTaskId());
                broadcastSnapshot();
            }
            case LEAVE -> {
                sendSystemText(msg.getUsername() + " 離開了房間。");
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
                    iterator.remove(); // 斷線者踢除
                }
            }
        }
    }
}