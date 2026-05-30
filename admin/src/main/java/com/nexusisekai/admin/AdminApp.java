package com.nexusisekai.admin;

import com.nexusisekai.admin.api.ApiClient;
import com.nexusisekai.admin.ui.MainWindow;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Entry point Admin Panel JavaFX
 * Chạy: mvn javafx:run  hoặc  java -jar nexusisekai-admin.jar
 */
public class AdminApp extends Application {

    public static final String VERSION = "1.0.0";

    // Config kết nối đến server (có thể sửa trong Settings dialog)
    public static String serverHost = "localhost";
    public static int    serverPort = 8080;
    public static String adminKey   = "nexus_admin_secret_key";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // Khởi tạo API client singleton
        ApiClient.init(serverHost, serverPort, adminKey);

        MainWindow mainWindow = new MainWindow();
        Scene scene = new Scene(mainWindow.getRoot(), 1280, 800);

        // Dark theme CSS
        scene.getStylesheets().add(getClass().getResource("/styles/dark-theme.css").toExternalForm());

        primaryStage.setTitle("Nexus Isekai — Admin Panel v" + VERSION);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(600);
        primaryStage.show();

        // Kiểm tra kết nối khi khởi động
        mainWindow.checkConnection();
    }
}
