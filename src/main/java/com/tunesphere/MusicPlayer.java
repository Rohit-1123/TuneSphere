package com.tunesphere;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;
import java.io.*;
import java.util.*;

public class MusicPlayer {
    private static Player player;
    private static Thread playThread;
    private static boolean stopped = false;
    private static boolean paused = false;
    private static File currentSong;
    private static List<File> playlist = new ArrayList<>();
    private static int currentIndex = 0;

    // Play mood folder
    public static synchronized void playMoodSongs(String moodFolderName) {
        stop(); // stop anything playing

        File folder = new File("songs/" + moodFolderName);
        if (!folder.exists()) {
            System.out.println("Folder not found: " + folder.getAbsolutePath());
            return;
        }

        File[] files = folder.listFiles((d, n) -> n.toLowerCase().endsWith(".mp3"));
        if (files == null || files.length == 0) {
            System.out.println("No MP3s in " + folder.getAbsolutePath());
            return;
        }

        playlist = Arrays.asList(files);
        Collections.sort(playlist);
        currentIndex = 0;
        playSong(playlist.get(currentIndex));
    }

    private static synchronized void playSong(File song) {
        stop();
        stopped = false;
        paused = false;
        currentSong = song;

        playThread = new Thread(() -> {
            try (FileInputStream fis = new FileInputStream(song)) {
                player = new Player(fis);
                System.out.println("Playing: " + song.getName());
                player.play();
            } catch (Exception e) {
                if (!stopped) e.printStackTrace();
            }
        });
        playThread.start();
    }

    public static synchronized void nextSong() {
        if (playlist.isEmpty()) return;
        stop();
        currentIndex = (currentIndex + 1) % playlist.size();
        playSong(playlist.get(currentIndex));
    }

    public static synchronized void stop() {
        stopped = true;
        try {
            if (player != null) {
                player.close();
                player = null;
            }
            if (playThread != null && playThread.isAlive()) {
                playThread.interrupt();
            }
        } catch (Exception ignored) {}
    }

    public static synchronized void pause() {
        if (player != null) {
            paused = true;
            stop();
        }
    }

    public static synchronized void resume() {
        if (paused && currentSong != null) {
            paused = false;
            playSong(currentSong);
        }
    }

    public static synchronized String getCurrentSongName() {
        return currentSong != null ? currentSong.getName() : "None";
    }
}
