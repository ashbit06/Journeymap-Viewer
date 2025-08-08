package Journeymap;

import java.util.Arrays;

public record Waypoint(String name, PointXYZ position, Dimension[] dimensions, WaypointIcon icon, String guid) {
    @Override
    public String toString() {
        return String.format("[%s] \"%s\" at %s, shown in %s", guid, name, position.toString(), Arrays.toString(dimensions));
    }
}
