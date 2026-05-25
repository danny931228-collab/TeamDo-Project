# 雲端共編清單 Cloud Collaborative List

本專案依照「進階程式設計期末專題企畫書」實作，內容包含：JavaFX 使用者介面、TCP Socket 長連線、多人即時同步、SQLite 任務資料儲存、任務新增/編輯/勾選/刪除、在線成員顯示、同步狀態提示、操作音效與 CSS 主題切換。

## 一、專案功能

### 1. 登入與房間加入
- 輸入使用者名稱
- 輸入 Server IP
- 輸入 Port
- 輸入協作房間名稱
- 支援「個人模式」快速產生 private_使用者名稱 的私有房間

### 2. 任務清單主畫面
- 顯示任務列表
- 新增任務
- Checkbox 勾選完成
- 狀態切換：待辦、進行中、已完成
- 編輯任務內容
- 刪除任務
- 顯示任務編號、建立者、最後修改時間
- 顯示在線人數與成員名稱
- 顯示同步狀態與系統通知

### 3. Server 管理
- 多 Client 連線管理
- 依房間名稱隔離任務與成員
- SQLite 任務資料持久化
- 由 SQLite AUTOINCREMENT 分配唯一任務 ID，避免編號重複
- 任務新增、修改、刪除後即時廣播 Snapshot 給同房間所有成員
- 成員加入與離線通知

### 4. 互動體驗
- 新增任務音效
- 完成任務音效
- 刪除任務音效
- 可切換音效開關
- CSS 主題切換：商務藍、簡約黑、活力橘

## 二、開發環境

- JDK 21
- Maven 3.9+
- JavaFX 21.0.4
- SQLite JDBC 3.46.1.3

## 三、專案結構

```text
cloud-collab-list/
├─ pom.xml
├─ README.md
├─ run-server.bat
├─ run-client.bat
├─ run-server.sh
├─ run-client.sh
└─ src/main/
   ├─ java/com/cloudlist/
   │  ├─ client/          JavaFX Client、Controller、Socket Client、音效控制
   │  ├─ server/          TCP Server、ClientHandler、SQLite 資料庫
   │  ├─ model/           TaskItem 任務資料模型
   │  └─ net/             Message 共用封包模型
   └─ resources/
      ├─ fxml/            JavaFX FXML 介面
      ├─ css/             基礎樣式與三種主題
      └─ sounds/          操作音效 wav
```

## 四、執行方式

### 方法 A：Windows 批次檔

先啟動 Server：

```bat
run-server.bat
```

再啟動 Client：

```bat
run-client.bat
```

### 方法 B：Maven 指令

Server：

```bash
mvn -q exec:java -Dexec.mainClass=com.cloudlist.server.CloudListServer -Dexec.args="5555"
```

Client：

```bash
mvn -q javafx:run
```

## 五、多人測試方式

1. 在同一台電腦先執行 Server。
2. 執行兩個以上 Client 視窗。
3. 每個 Client 輸入相同 IP、Port 與房間名稱，例如：
   - Server IP：127.0.0.1
   - Port：5555
   - 房間名稱：main
4. 任一使用者新增、勾選、編輯或刪除任務，其他 Client 會即時同步更新。

## 六、區域網路測試方式

1. Server 電腦與 Client 電腦/筆電需在同一個 Wi-Fi 或區域網路。
2. Server 電腦查詢區域網路 IP，例如 Windows 可使用：

```bat
ipconfig
```

3. Client 的 Server IP 輸入 Server 電腦的 IPv4 位址，例如：192.168.1.20。
4. 防火牆若阻擋 Java 或 Port 5555，請允許連線。

## 七、資料庫說明

Server 啟動後會在執行目錄建立：

```text
cloud_list.db
```

資料表：

```sql
CREATE TABLE IF NOT EXISTS tasks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    room_id TEXT NOT NULL,
    content TEXT NOT NULL,
    status TEXT NOT NULL,
    creator TEXT NOT NULL,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);
```

## 八、通訊設計

Client 與 Server 使用 Java TCP Socket 建立長連線，並以 `ObjectInputStream` / `ObjectOutputStream` 傳輸 `Message` 物件。

主要訊息類型：

- JOIN：加入房間
- LEAVE：離開房間
- ADD_TASK：新增任務
- UPDATE_TASK：更新任務內容或狀態
- DELETE_TASK：刪除任務
- SNAPSHOT：Server 廣播完整任務清單
- USERS：Server 廣播在線成員
- SYSTEM：Server 廣播系統通知
- ERROR：Server 傳送錯誤訊息

## 九、備註

- 若 JavaFX 音效在部分環境無法播放，系統會自動忽略，不影響主要功能。
- 若要重新開始測試，可關閉 Server 後刪除 `cloud_list.db`。
- 若多台電腦無法連線，請優先檢查 Server IP、Port、防火牆與是否在同一區域網路。
