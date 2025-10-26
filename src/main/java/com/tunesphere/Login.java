package com.tunesphere;

import javafx.application.Application;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.sql.*;

public class Login extends Application {

    private static boolean isDarkMode = false; // shared theme state

    @Override
    public void start(Stage primaryStage) {
        showLoginScreen(primaryStage);
    }

    // --- LOGIN SCREEN ---
    public void showLoginScreen(Stage primaryStage) {
        primaryStage.setTitle("TuneSphere - Login");

        // Title
        Label title = new Label("TuneSphere Login");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        // Username and password fields
        Label userLabel = new Label("Username:");
        TextField usernameField = new TextField();

        Label passLabel = new Label("Password:");
        PasswordField passwordField = new PasswordField();

        // Show password option
        TextField visiblePasswordField = new TextField();
        visiblePasswordField.managedProperty().bind(visiblePasswordField.visibleProperty());
        visiblePasswordField.visibleProperty().bind(passwordField.visibleProperty().not());
        visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());

        CheckBox showPassword = new CheckBox("Show Password");
        showPassword.setOnAction(e -> {
            boolean show = showPassword.isSelected();
            passwordField.setVisible(!show);
            visiblePasswordField.setVisible(show);
        });

        // Buttons
        Button loginBtn = new Button("Login");
        Button registerBtn = new Button("Create Account");
        Label messageLabel = new Label();

        // Theme toggle
        ToggleButton themeToggle = new ToggleButton(isDarkMode ? "🌙 Dark Mode" : "🌞 Light Mode");
        themeToggle.setSelected(isDarkMode);
        themeToggle.setOnAction(e -> toggleTheme(themeToggle, themeToggle.getScene()));

        // Login action
        loginBtn.setOnAction(e -> {
            String username = usernameField.getText();
            String password = passwordField.getText();

            if (username.isEmpty() || password.isEmpty()) {
                messageLabel.setText("Please fill all fields!");
                return;
            }

            if (authenticateUser(username, password)) {
                messageLabel.setText("Login successful!");
                showMainApp(primaryStage, username);
            } else {
                messageLabel.setText("Invalid credentials!");
            }
        });

        registerBtn.setOnAction(e -> openRegisterWindow(primaryStage));

        // Layout
        VBox centerLayout = new VBox(10);
        centerLayout.setAlignment(Pos.CENTER);
        centerLayout.setPadding(new Insets(25));
        centerLayout.getChildren().addAll(
                title,
                userLabel, usernameField,
                passLabel, passwordField, visiblePasswordField, showPassword,
                loginBtn, registerBtn, messageLabel
        );

        BorderPane root = new BorderPane();
        root.setCenter(centerLayout);
        root.setBottom(themeToggle);
        BorderPane.setAlignment(themeToggle, Pos.CENTER);
        BorderPane.setMargin(themeToggle, new Insets(10));

        Scene scene = new Scene(root, 400, 750);
        applyTheme(scene);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // --- REGISTER SCREEN ---
    private void openRegisterWindow(Stage mainStage) {
        Stage regStage = new Stage();
        regStage.setTitle("TuneSphere - Create Account");

        Label title = new Label("Create New Account");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label userLabel = new Label("Choose Username:");
        TextField usernameField = new TextField();

        Label emailLabel = new Label("Enter Email:");
        TextField emailField = new TextField();

        Label passLabel = new Label("Choose Password:");
        PasswordField passwordField = new PasswordField();

        // Show password option
        TextField visiblePasswordField = new TextField();
        visiblePasswordField.managedProperty().bind(visiblePasswordField.visibleProperty());
        visiblePasswordField.visibleProperty().bind(passwordField.visibleProperty().not());
        visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());

        CheckBox showPassword = new CheckBox("Show Password");
        showPassword.setOnAction(e -> {
            boolean show = showPassword.isSelected();
            passwordField.setVisible(!show);
            visiblePasswordField.setVisible(show);
        });

        Button registerBtn = new Button("Register");
        Button backBtn = new Button("Back");
        Label msgLabel = new Label();

        registerBtn.setOnAction(e -> {
            String username = usernameField.getText();
            String email = emailField.getText();
            String password = passwordField.getText();

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                msgLabel.setText("Please fill all fields!");
                return;
            }

            if (registerUser(username, email, password)) {
                msgLabel.setText("Registration successful!");
            } else {
                msgLabel.setText("Username or email already exists!");
            }
        });

        backBtn.setOnAction(e -> {
            regStage.close();
            showLoginScreen(mainStage);
        });

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(20));
        vbox.setAlignment(Pos.CENTER);
        vbox.getChildren().addAll(
                title,
                userLabel, usernameField,
                emailLabel, emailField,
                passLabel, passwordField, visiblePasswordField, showPassword,
                registerBtn, backBtn, msgLabel
        );

        Scene regScene = new Scene(vbox, 400, 450);
        applyTheme(regScene);
        regStage.setScene(regScene);
        regStage.show();
    }

    // --- MAIN APP SCREEN ---
    public void showMainApp(Stage stage, String username) {
        Label welcome = new Label("Welcome, " + username + "!");
        welcome.setStyle("-fx-font-size: 22px; -fx-text-fill: #2ecc71;");

        Button startMoodBtn = new Button("Start Mood Detection");
        Button logoutBtn = new Button("Logout");

        startMoodBtn.setOnAction(e -> {
            MoodDetector moodDetector = new MoodDetector();
            Stage moodStage = new Stage();
            moodDetector.start(moodStage);
        });

        logoutBtn.setOnAction(e -> showLoginScreen(stage));

        ToggleButton themeToggle = new ToggleButton(isDarkMode ? "🌙 Dark Mode" : "🌞 Light Mode");
        themeToggle.setSelected(isDarkMode);
        themeToggle.setOnAction(e -> toggleTheme(themeToggle, themeToggle.getScene()));

        VBox vbox = new VBox(20, welcome, startMoodBtn, logoutBtn);
        vbox.setPadding(new Insets(30));
        vbox.setAlignment(Pos.CENTER);

        BorderPane root = new BorderPane();
        root.setCenter(vbox);
        root.setBottom(themeToggle);
        BorderPane.setAlignment(themeToggle, Pos.CENTER);
        BorderPane.setMargin(themeToggle, new Insets(10));

        Scene scene = new Scene(root, 400, 350);
        applyTheme(scene);
        stage.setScene(scene);
        stage.show();
    }

    // --- DATABASE METHODS ---
    private boolean authenticateUser(String username, String password) {
        try (Connection conn = Database.connect()) {
            String query = "SELECT * FROM users WHERE username=? AND password=?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean registerUser(String username, String email, String password) {
        try (Connection conn = Database.connect()) {
            String query = "INSERT INTO users (username, email, password) VALUES (?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, username);
            stmt.setString(2, email);
            stmt.setString(3, password);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
            return false;
        }
    }

    // --- THEME HANDLING ---
    private void toggleTheme(ToggleButton toggle, Scene scene) {
        isDarkMode = !isDarkMode;
        applyTheme(scene);
        toggle.setText(isDarkMode ? "🌙 Dark Mode" : "🌞 Light Mode");
    }

    private void applyTheme(Scene scene) {
        scene.getStylesheets().clear();
        if (isDarkMode) {
            scene.getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());
        } else {
            scene.getStylesheets().add(getClass().getResource("/light-theme.css").toExternalForm());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
