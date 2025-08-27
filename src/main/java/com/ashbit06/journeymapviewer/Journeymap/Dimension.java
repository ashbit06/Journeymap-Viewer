package Journeymap;

import java.util.List;
import java.util.Objects;

public enum Dimension {
    OVERWORLD("overworld"),
    NETHER("the_nether"),
    END("the_end");

    public static final String[] names = {
            "overworld", "the_nether", "the_end"
    };

    private final String id;

    public static Dimension from(String id) throws Exception {
        if (id.isEmpty()) return OVERWORLD;
        if (List.of("overworld", "minecraft:overworld").contains(id)) return OVERWORLD;
        if (List.of("the_nether", "minecraft:the_nether").contains(id)) return NETHER;
        if (List.of("the_end", "minecraft:the_end").contains(id)) return END;

        System.out.println(id);

        throw new Exception(String.format("%s is not a valid dimensions id", id));
    }

    Dimension(String id) { this.id = id; }

    public String getId() { return this.id; }
}