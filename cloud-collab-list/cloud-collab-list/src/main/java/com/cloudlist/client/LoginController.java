package com.cloudlist.client;

import com.cloudlist.net.Message;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * 登入畫面控制器。
 */
public class LoginController {
    @FXML private TextField usernameField;
    @FXML private TextField hostField;
    @FXML private TextField portField;
    @FXML private TextField roomField;
    @FXML private Label statusLabel;
    @FXML private Button joinButton;
    @FXML private Button exitButton;

    private CloudListClientApp app;

    public void setApp(CloudListClientApp app) {
        this.app = app;
    }

    @FXML
    private void initialize() {
        usernameField.setText(System.getProperty("user.name", "Guest"));
        hostField.setText("127.0.0.1");
        portField.setText("8888");
        roomField.setText("main");
        statusLabel.setText("尚未連線");
    }

    @FXML
    private void onJoinRoom() {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String host = hostField.getText() == null ? "" : hostField.getText().trim();
        String portText = portField.getText() == null ? "" : portField.getText().trim();
        String roomId = roomField.getText() == null ? "" : roomField.getText().trim();

        if (username.isBlank() || host.isBlank() || portText.isBlank() || roomId.isBlank()) {
            showAlert("資料不完整", "請輸入使用者名稱、Server IP、Port 與房間名稱。");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException ex) {
            showAlert("Port 格式錯誤", "Port 必須是數字，例如 5555。");
            return;
        }

        joinButton.setDisable(true);
        statusLabel.setText("正在連線到 " + host + ":" + port + " ...");

        Thread connectThread = new Thread(() -> {
            try {
                ClientConnection connection = new ClientConnection(host, port);
                connection.send(Message.join(username, roomId));
                Platform.runLater(() -> {
                    try {
                        statusLabel.setText("連線成功，正在進入協作空間...");
                        app.showMain(connection, username, roomId);
                    } catch (Exception ex) {
                        connection.close();
                        joinButton.setDisable(false);
                        showAlert("切換畫面失敗", ex.getMessage());
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    joinButton.setDisable(false);
                    statusLabel.setText("連線失敗：" + ex.getMessage());
                    showAlert("無法連線", "請確認 Server 是否已啟動，IP 與 Port 是否正確。\n\n錯誤訊息：" + ex.getMessage());
                });
            }
        }, "connect-thread");
        connectThread.setDaemon(true);
        connectThread.start();
    }

    @FXML
    private void onUsePersonalRoom() {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        if (username.isBlank()) {
            username = "Guest";
            usernameField.setText(username);
        }
        hostField.setText("127.0.0.1");
        portField.setText("8888");
        roomField.setText("private_" + username);
        statusLabel.setText("已切換為個人模式房間，請確認本機 Server 已啟動後按加入。");
    }

    @FXML
    private void onExit() {
        Platform.exit();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
