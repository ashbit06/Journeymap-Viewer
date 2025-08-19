package Journeymap;

public record Waypoint(String name, PointXYZ position, Dimension primaryDimension, Dimension[] dimensions, WaypointIcon icon, String guid) {
    @Override
    public String toString() {
        return String.format("[%s] \"%s\" at %s, in %s", guid, name, position.toString(), primaryDimension);
    }
}
