package com.cloudlist.client;

import com.cloudlist.model.TaskItem;
import com.cloudlist.net.Message;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.text.Text;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 任務清單主畫面控制器。
 */
public class MainController {
    @FXML private BorderPane root;
    @FXML private Label userLabel;
    @FXML private Label roomLabel;
    @FXML private Label onlineLabel;
    @FXML private Label memberLabel;
    @FXML private Label syncLabel;
    @FXML private TextField taskInputField;
    @FXML private Button addButton;
    @FXML private ListView<TaskItem> taskListView;
    @FXML private ComboBox<String> themeComboBox;
    @FXML private CheckBox soundCheckBox;

    private final ObservableList<TaskItem> tasks = FXCollections.observableArrayList();
    private final SoundPlayer soundPlayer = new SoundPlayer();
    private CloudListClientApp app;
    private ClientConnection connection;
    private String username;
    private String roomId;
    private int lastTaskCount = 0;
    private boolean firstSnapshot = true;
    private volatile boolean leaving = false;

    public void init(CloudListClientApp app, ClientConnection connection, String username, String roomId) {
        this.app = app;
        this.connection = connection;
        this.username = username;
        this.roomId = roomId;

        userLabel.setText("使用者：" + username);
        roomLabel.setText("房間：" + roomId);
        onlineLabel.setText("在線人數：0");
        memberLabel.setText("成員：等待同步...");
        syncLabel.setText("同步狀態：已連線");

        taskListView.setItems(tasks);
        taskListView.setCellFactory(view -> new TaskCell());

        setupThemeBox();
        setupSoundBox();
        setupNetworkListeners();
        connection.startListening();
        soundPlayer.play("system");
    }

    @FXML
    private void initialize() {
        // FXML 載入後會先呼叫 initialize，實際連線資料由 init 注入。
    }

    private void setupThemeBox() {
        themeComboBox.setItems(FXCollections.observableArrayList("商務藍", "簡約黑", "活力橘"));
        themeComboBox.getSelectionModel().select("商務藍");
        themeComboBox.setOnAction(event -> applyTheme(themeComboBox.getValue()));
    }

    private void setupSoundBox() {
        soundCheckBox.setSelected(true);
        soundCheckBox.selectedProperty().addListener((obs, oldValue, newValue) -> soundPlayer.setEnabled(newValue));
    }

    private void setupNetworkListeners() {
        connection.setMessageHandler(msg -> Platform.runLater(() -> handleMessage(msg)));
        connection.setErrorHandler(ex -> Platform.runLater(() -> {
            if (!leaving) {
                syncLabel.setText("同步狀態：連線中斷");
                showAlert("連線中斷", "與 Server 的連線已中斷：" + ex.getMessage());
            }
        }));
    }

    private void handleMessage(Message msg) {
        switch (msg.getType()) {
            case SNAPSHOT -> updateTasks(msg.getTasks());
            case USERS -> updateUsers(msg.getUsers());
            case SYSTEM -> updateSystemText(msg.getText());
            case ERROR -> showAlert("Server 訊息", msg.getText());
            default -> syncLabel.setText("同步狀態：收到 " + msg.getType());
        }
    }

    private void updateTasks(List<TaskItem> serverTasks) {
        tasks.setAll(serverTasks);
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        syncLabel.setText("同步狀態：已更新，最後同步 " + now);

        if (!firstSnapshot && serverTasks.size() > lastTaskCount) {
            soundPlayer.play("add");
        }
        lastTaskCount = serverTasks.size();
        firstSnapshot = false;
    }

    private void updateUsers(Set<String> users) {
        onlineLabel.setText("在線人數：" + users.size());
        memberLabel.setText("成員：" + String.join("、", users));
    }

    private void updateSystemText(String text) {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        syncLabel.setText("同步狀態：" + text + "（" + now + "）");
    }

    @FXML
    private void onAddTask() {
        String content = taskInputField.getText() == null ? "" : taskInputField.getText().trim();
        if (content.isBlank()) {
            showAlert("新增失敗", "任務內容不可空白。");
            return;
        }

        Message msg = new Message(Message.Type.ADD_TASK);
        msg.setContent(content);
        msg.setRoomId(roomId);
        msg.setUsername(username);
        send(msg);
        taskInputField.clear();
        soundPlayer.play("add");
    }

    @FXML
    private void onLeaveRoom() {
        leaveRoom();
        try {
            app.showLogin();
        } catch (IOException ex) {
            Platform.exit();
        }
    }

    public void leaveRoom() {
        if (leaving) return;
        leaving = true;
        try {
            Message msg = new Message(Message.Type.LEAVE);
            msg.setUsername(username);
            msg.setRoomId(roomId);
            connection.send(msg);
        } catch (Exception ignored) {
        } finally {
            connection.close();
        }
    }

    private void send(Message msg) {
        try {
            connection.send(msg);
        } catch (IOException ex) {
            syncLabel.setText("同步狀態：送出失敗");
            showAlert("送出失敗", ex.getMessage());
        }
    }

    private void applyTheme(String themeName) {
        if (root.getScene() == null) return;
        String cssFile = switch (themeName) {
            case "簡約黑" -> "/css/theme-dark.css";
            case "活力橘" -> "/css/theme-orange.css";
            default -> "/css/theme-blue.css";
        };
        Scene scene = root.getScene();
        scene.getStylesheets().clear();
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/base.css")).toExternalForm());
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource(cssFile)).toExternalForm());
        syncLabel.setText("同步狀態：已切換主題「" + themeName + "」");
    }

    private void updateTaskStatus(TaskItem task, String status) {
        Message msg = new Message(Message.Type.UPDATE_TASK);
        msg.setTaskId(task.getId());
        msg.setStatus(status);
        msg.setRoomId(roomId);
        msg.setUsername(username);
        send(msg);
        if (TaskItem.STATUS_DONE.equals(status)) {
            soundPlayer.play("done");
        }
    }

    private void editTask(TaskItem task) {
        TextInputDialog dialog = new TextInputDialog(task.getContent());
        dialog.setTitle("編輯任務");
        dialog.setHeaderText("修改任務 #" + task.getId());
        dialog.setContentText("任務內容：");
        Optional<String> result = dialog.showAndWait();
        result.map(String::trim)
                .filter(text -> !text.isBlank())
                .ifPresent(newText -> {
                    Message msg = new Message(Message.Type.UPDATE_TASK);
                    msg.setTaskId(task.getId());
                    msg.setContent(newText);
                    msg.setRoomId(roomId);
                    msg.setUsername(username);
                    send(msg);
                });
    }

    private void deleteTask(TaskItem task) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("刪除任務");
        confirm.setHeaderText(null);
        confirm.setContentText("確定要刪除任務 #" + task.getId() + " 嗎？");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Message msg = new Message(Message.Type.DELETE_TASK);
            msg.setTaskId(task.getId());
            msg.setRoomId(roomId);
            msg.setUsername(username);
            send(msg);
            soundPlayer.play("delete");
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content == null ? "" : content);
        alert.showAndWait();
    }

    private class TaskCell extends ListCell<TaskItem> {
        private final HBox box = new HBox(10);
        private final CheckBox doneBox = new CheckBox();
        private final Label idLabel = new Label();
        private final Text contentText = new Text();
        private final Label creatorLabel = new Label();
        private final Label timeLabel = new Label();
        private final ComboBox<String> statusBox = new ComboBox<>();
        private final Button editButton = new Button("編輯");
        private final Button deleteButton = new Button("刪除");
        private boolean refreshing = false;

        TaskCell() {
            statusBox.setItems(FXCollections.observableArrayList(
                    TaskItem.STATUS_TODO, TaskItem.STATUS_DOING, TaskItem.STATUS_DONE));
            contentText.getStyleClass().add("task-content");
            idLabel.getStyleClass().add("task-id");
            creatorLabel.getStyleClass().add("task-meta");
            timeLabel.getStyleClass().add("task-meta");
            deleteButton.getStyleClass().add("danger-button");

            HBox.setHgrow(contentText, Priority.ALWAYS);
            box.getChildren().addAll(doneBox, idLabel, contentText, statusBox, creatorLabel, timeLabel, editButton, deleteButton);
            box.getStyleClass().add("task-row");

            doneBox.setOnAction(event -> {
                if (refreshing || getItem() == null) return;
                updateTaskStatus(getItem(), doneBox.isSelected() ? TaskItem.STATUS_DONE : TaskItem.STATUS_TODO);
            });

            statusBox.setOnAction(event -> {
                if (refreshing || getItem() == null || statusBox.getValue() == null) return;
                updateTaskStatus(getItem(), statusBox.getValue());
            });

            editButton.setOnAction(event -> {
                if (getItem() != null) editTask(getItem());
            });

            deleteButton.setOnAction(event -> {
                if (getItem() != null) deleteTask(getItem());
            });
        }

        @Override
        protected void updateItem(TaskItem task, boolean empty) {
            super.updateItem(task, empty);
            if (empty || task == null) {
                setGraphic(null);
                return;
            }

            refreshing = true;
            idLabel.setText("#" + task.getId());
            contentText.setText(task.getContent());
            creatorLabel.setText("建立者：" + task.getCreator());
            timeLabel.setText("更新：" + task.getUpdatedAt());
            statusBox.getSelectionModel().select(task.getStatus());
            doneBox.setSelected(task.isDone());
            contentText.getStyleClass().remove("task-done-text");
            if (task.isDone()) {
                contentText.getStyleClass().add("task-done-text");
            }
            refreshing = false;
            setGraphic(box);
        }
    }
}
