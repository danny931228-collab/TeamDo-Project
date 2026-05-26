import java.io.*;
import java.net.*;

public class TaskClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public static void main(String[] args) {
        TaskClient client = new TaskClient();
        try {
            System.out.println("====== 正在嘗試連線至 TeamDo 雲端伺服器 ======");
            // 連線到本機測試，Port 要跟 Server 一模一樣
            client.connect("127.0.0.1", 8888);
            System.out.println("【系統通知】成功連上伺服器！可以開始同步資料。");

            // 模擬發送測試資料給 Server
            client.sendMessage("USER:Danny | ACTION:ADD | TASK:親手寫完連線測試！");

            // 讓主程式暫停一下，等待接收 Server 的廣播回傳
            Thread.sleep(2000);

        } catch (Exception e) {
            System.err.println("連線失敗，請確認 TaskServer 是否已經啟動。");
            e.printStackTrace();
        }
    }

    // 建立連線的方法
    public void connect(String ip, int port) throws IOException {
        this.socket = new Socket(ip, port);
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // 開啟一個獨立的背景執行緒（Thread），專門在背景默默收聽 Server 隨時傳過來的廣播
        new Thread(this::listenFromServer).start();
    }

    // 傳送訊息給 Server
    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    // 背景監聽伺服器廣播的邏輯
    private void listenFromServer() {
        try {
            String response;
            // 當伺服器有廣播任何新任務狀態時，這裡會立刻收到
            while ((response = in.readLine()) != null) {
                System.out.println("\n【前端收到雲端廣播】" + response);
            }
        } catch (IOException e) {
            System.out.println("【系統通知】與雲端伺服器斷開連線。");
        }
    }
}