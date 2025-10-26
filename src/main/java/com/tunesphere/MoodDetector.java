package com.tunesphere;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.bytedeco.javacv.*;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;

import javax.swing.*;
import java.awt.image.BufferedImage;

import static org.bytedeco.opencv.global.opencv_imgproc.*;
import static org.bytedeco.opencv.global.opencv_objdetect.CASCADE_SCALE_IMAGE;

public class MoodDetector {

    private CascadeClassifier faceDetector;
    private volatile boolean running = true;
    private String detectedMood = "neutral";

    public void start(Stage stage) {
        faceDetector = new CascadeClassifier("haarcascade_frontalface_default.xml");
        if (faceDetector.empty()) {
            JOptionPane.showMessageDialog(null, "Face detector not found. Please add haarcascade_frontalface_default.xml to project root.");
            return;
        }

        // --- UI Components ---
        ImageView imageView = new ImageView();
        imageView.setFitWidth(640);
        imageView.setFitHeight(480);
        imageView.setPreserveRatio(true);

        Label moodLabel = new Label("Detecting mood...");
        moodLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #2c3e50;");
        Label songLabel = new Label("Now Playing: None");

        // Control Buttons
        Button playBtn = new Button("▶ Play");
        Button pauseBtn = new Button("⏸ Pause");
        Button nextBtn = new Button("⏭ Next");
        Button stopBtn = new Button("⏹ Stop");
        Button redetectBtn = new Button("🔄 Detect Again");
        Button backBtn = new Button("⬅ Back");

        // --- Manual Mood Selection ---
        Label chooseMoodLabel = new Label("🎭 Or choose a mood manually:");
        ComboBox<String> moodDropdown = new ComboBox<>();
        moodDropdown.getItems().addAll("Happy", "Sad", "Neutral");
        moodDropdown.setPromptText("Select Mood");

        Button chooseMoodBtn = new Button("Play Mood");
        chooseMoodBtn.setOnAction(e -> {
            String selectedMood = moodDropdown.getValue();

            if (selectedMood == null) {
                new Alert(Alert.AlertType.WARNING, "Please select a mood first!").show();
                return;
            }

            running = false; // stop camera
            MusicPlayer.stop();

            // Play songs directly based on user selection
            new Thread(() -> {
                MusicPlayer.playMoodSongs(selectedMood.toLowerCase());
                Platform.runLater(() -> {
                    moodLabel.setText("Selected Mood: " + selectedMood + " 🎵");
                    songLabel.setText("Now Playing: " + MusicPlayer.getCurrentSongName());
                });
            }).start();
        });

        // --- Button Actions ---
        playBtn.setOnAction(e -> {
            MusicPlayer.resume();
            songLabel.setText("Now Playing: " + MusicPlayer.getCurrentSongName());
        });

        pauseBtn.setOnAction(e -> MusicPlayer.pause());
        nextBtn.setOnAction(e -> {
            MusicPlayer.nextSong();
            songLabel.setText("Now Playing: " + MusicPlayer.getCurrentSongName());
        });
        stopBtn.setOnAction(e -> MusicPlayer.stop());

        redetectBtn.setOnAction(e -> {
            running = false;
            MusicPlayer.stop();
            stage.close();

            Platform.runLater(() -> {
                MoodDetector newDetector = new MoodDetector();
                Stage newStage = new Stage();
                newDetector.start(newStage);
            });
        });

        backBtn.setOnAction(e -> {
            running = false;
            MusicPlayer.stop();
            stage.close();

            Platform.runLater(() -> {
                Stage mainStage = new Stage();
                new Login().showMainApp(mainStage, "User");
            });
        });

        // --- Layout ---
        HBox controlButtons = new HBox(10, playBtn, pauseBtn, nextBtn, stopBtn);
        controlButtons.setAlignment(Pos.CENTER);

        HBox topBar = new HBox(backBtn);
        topBar.setAlignment(Pos.TOP_LEFT);
        topBar.setPadding(new Insets(10));

        VBox bottomPanel = new VBox(10,
                moodLabel,
                songLabel,
                controlButtons,
                redetectBtn,
                chooseMoodLabel,
                moodDropdown,
                chooseMoodBtn
        );
        bottomPanel.setAlignment(Pos.CENTER);
        bottomPanel.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(imageView);
        root.setBottom(bottomPanel);

        Scene scene = new Scene(root, 800, 650);
        stage.setTitle("TuneSphere - Mood Detection");
        stage.setScene(scene);
        stage.show();

        // --- Camera Thread ---
        new Thread(() -> {
            try (OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0)) {
                grabber.start();

                CascadeClassifier smileDetector = new CascadeClassifier("haarcascade_smile.xml");
                if (smileDetector.empty()) {
                    System.out.println("Smile detector not found!");
                    return;
                }

                OpenCVFrameConverter.ToMat matConverter = new OpenCVFrameConverter.ToMat();
                Java2DFrameConverter java2dConverter = new Java2DFrameConverter();

                boolean detected = false;
                String currentMood = "neutral";

                while (running) {
                    Frame frame = grabber.grab();
                    if (frame == null) continue;

                    BufferedImage img = java2dConverter.convert(frame);
                    if (img != null) {
                        Image fxImage = SwingFXUtils.toFXImage(img, null);
                        Platform.runLater(() -> imageView.setImage(fxImage));
                    }

                    if (!detected) {
                        Mat mat = matConverter.convert(frame);
                        if (mat == null) continue;

                        RectVector faces = new RectVector();
                        faceDetector.detectMultiScale(mat, faces, 1.1, 5, CASCADE_SCALE_IMAGE,
                                new Size(100, 100), new Size());

                        if (faces.size() > 0) {
                            Rect face = faces.get(0);
                            rectangle(mat, face, new Scalar(0, 255, 0, 1));

                            Mat faceROI = new Mat(mat, face);
                            RectVector smiles = new RectVector();
                            smileDetector.detectMultiScale(faceROI, smiles, 1.7, 22, 0,
                                    new Size(25, 25), new Size());

                            double ratio = (double) face.height() / face.width();

                            if (smiles.size() > 0) {
                                currentMood = "happy";
                            } else if (ratio > 1.5) {
                                currentMood = "sad";
                            } else {
                                currentMood = "neutral";
                            }

                            String moodText = switch (currentMood) {
                                case "happy" -> "Happy 😊";
                                case "sad" -> "Sad 😢";
                                default -> "Neutral 😐";
                            };

                            Platform.runLater(() -> moodLabel.setText("Detected Mood: " + moodText));

                            String moodForThread = currentMood;
                            new Thread(() -> {
                                MusicPlayer.playMoodSongs(moodForThread);
                                Platform.runLater(() ->
                                        songLabel.setText("Now Playing: " + MusicPlayer.getCurrentSongName()));
                            }).start();

                            detected = true;
                        }
                    }

                    Thread.sleep(15);
                }

                grabber.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
