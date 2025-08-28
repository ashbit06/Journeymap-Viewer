package Journeymap;

public record World(String name, boolean isMultiplayer) {
    @Override
    public String toString() {
        return String.format("%sp/%s", (isMultiplayer ? "m" : "s"), name);
    }
}