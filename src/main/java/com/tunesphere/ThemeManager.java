package com.tunesphere;

import javafx.scene.Scene;
import java.util.prefs.Preferences;

public class ThemeManager {
    private static final Preferences prefs = Preferences.userRoot().node("com.tunesphere.theme");
    private static String currentTheme = prefs.get("theme", "light");

    public static void applyTheme(Scene scene) {
        scene.getStylesheets().removeIf(s ->
            s.endsWith("light-theme.css") || s.endsWith("dark-theme.css")
        );

        scene.getStylesheets().add(
            ThemeManager.class.getResource("/" + currentTheme + "-theme.css").toExternalForm()
        );
    }

    public static void toggleTheme(Scene scene) {
        currentTheme = currentTheme.equals("light") ? "dark" : "light";
        prefs.put("theme", currentTheme);
        applyTheme(scene);
    }

    public static boolean isDarkMode() {
        return currentTheme.equals("dark");
    }

    public static String getCurrentTheme() {
        return currentTheme;
    }
}
