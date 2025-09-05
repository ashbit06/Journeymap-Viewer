package com.ashbit06.journeymapviewer;

import java.util.prefs.Preferences;

public class Settings {
    private static final Preferences prefs = Preferences.userNodeForPackage(Settings.class);

    public static void saveLastWorld(String world) {
        prefs.put("lastWorld", world);
    }

    public static String getLastWorld() {
        return prefs.get("lastWorld", null); // null if not set
    }

    public static void clearLastWorld() {
        prefs.remove("lastWorld");
    }

    public static void saveLastJourneymap(String path) {
        prefs.put("lastJourneymap", path);
    }

    public static String getLastJourneymap() {
        return prefs.get("lastJourneymap", null);
    }

    public static void clearLastJourneymap() {
        prefs.remove("lastJourneymap");
    }
}
