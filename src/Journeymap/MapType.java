package Journeymap;

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

    MapType(String id) {
        this.id = id;
        this.hasLayers = id.compareTo("cave") == 0;
    }

    public String getId() { return this.id; }
    public boolean getHasLayers() { return this.hasLayers; }
}
