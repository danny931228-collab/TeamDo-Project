package com.cloudlist.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * JavaFX Client 主程式。
 */
public class CloudListClientApp extends Application {
    private Stage primaryStage;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws IOException {
        this.primaryStage = stage;
        showLogin();
    }

    public void showLogin() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
        Scene scene = new Scene(loader.load(), 620, 500);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/base.css")).toExternalForm());
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/theme-blue.css")).toExternalForm());

        LoginController controller = loader.getController();
        controller.setApp(this);

        primaryStage.setTitle("雲端共編清單 - 登入");
        primaryStage.setOnCloseRequest(null);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(560);
        primaryStage.setMinHeight(460);
        primaryStage.show();
    }

    public void showMain(ClientConnection connection, String username, String roomId) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        Scene scene = new Scene(loader.load(), 980, 680);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/base.css")).toExternalForm());
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/theme-blue.css")).toExternalForm());

        MainController controller = loader.getController();
        controller.init(this, connection, username, roomId);

        primaryStage.setTitle("雲端共編清單 - " + roomId);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(880);
        primaryStage.setMinHeight(600);
        primaryStage.setOnCloseRequest(event -> controller.leaveRoom());
        primaryStage.show();
    }
}
