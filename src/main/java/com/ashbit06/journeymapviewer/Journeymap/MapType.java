package com.ashbit06.journeymapviewer.Journeymap;

import java.util.List;

public enum MapType {
    CAVE("cave"),
    BIOME("biome"),
    DAY("day"),
    NIGHT("night"),
    TOPO("topo");

    public static final String[] names = {
            "cave", "biome", "day", "night", "topo"
    };

    private final String id;
    private final boolean hasLayers;

    public static MapType from(String id) throws Exception {
        if (!List.of(names).contains(id)) throw new Exception(String.format("%s is not a valid dimensions id", id));

        return switch (id) {
            case "cave" -> CAVE;
            case "biome" -> BIOME;
            case "topo" -> TOPO;
            case "night" -> NIGHT;
            default -> DAY;
        };
    }

    MapType(String id) {
        this.id = id;
        this.hasLayers = id.compareTo("cave") == 0;
    }

    public String getId() { return this.id; }
    public boolean getHasLayers() { return this.hasLayers; }
}
