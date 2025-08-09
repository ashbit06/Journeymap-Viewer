package Journeymap;

import net.querz.nbt.io.NBTDeserializer;
import net.querz.nbt.tag.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.*;
import java.util.*;
import java.util.List;

public class Journeymap {
    private final File path;
    private World world;
    private final HashMap<String, Waypoint> waypoints;

    public Journeymap(String path, World world) throws Exception {
        this.path = new File(path);
        if (!this.path.exists()) throw new FileNotFoundException();
        if (!this.path.isDirectory()) throw new NotDirectoryException(path);
        if (this.path.getName().compareTo("journeymap") != 0)
            throw new Exception("selected folder needs to be named \"journeymap\"");

        this.world = world;

        File file = new File(getFullPath()+"/waypoints/WaypointData.dat");
        NBTDeserializer deserializer = new NBTDeserializer(false);
        CompoundTag root = (CompoundTag) deserializer.fromFile(file).getTag();

        CompoundTag waypoints = root.getCompoundTag("waypoints");
        this.waypoints = new HashMap<>();

        int i = 0;
        for (String key : waypoints.keySet()) {
            CompoundTag wp = waypoints.getCompoundTag(key);
            CompoundTag pos = wp.getCompoundTag("pos");
            ListTag<StringTag> dims = (ListTag<StringTag>) wp.getListTag("dimensions");
            CompoundTag icon = wp.getCompoundTag("icon");

            List<Dimension> dimensions = new ArrayList<>();
            dims.forEach(d -> {
                try {
                    dimensions.add(Dimension.from(d.getValue()));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            Waypoint waypoint = new Waypoint(
                    wp.getString("name"),
                    new PointXYZ(
                            pos.getInt("x"),
                            pos.getInt("y"),
                            pos.getInt("z")
                    ),
                    dimensions.toArray(new Dimension[0]),
                    new WaypointIcon(
                            icon.getString("resourceLocation"),
                            new Color(wp.getInt("color")),
                            icon.getInt("textureWidth"),
                            icon.getInt("textureHeight")
                    ),
                    wp.getString("guid")
            );
            System.out.println(waypoint);

            this.waypoints.put(waypoint.guid(), waypoint);
            i++;
        }

    }

    public File getPath() { return this.path; }

    public String getFullPath() {
        return String.format("%s/data/%s", this.path.toString(), this.world.toString());
    }

    public World getWorld() { return this.world; }

    public void setWorld(World world) { this.world = world; }

    public List<String> getWorldList(boolean retrieveMultiplayer) {
        File worlds = new File(path + String.format("/data/%sp", retrieveMultiplayer ? 'm' : 's'));

        return List.of(Objects.requireNonNull(worlds.list()));
    }

    public List<String> getDimensions() {
        ArrayList<String> dimensions = new ArrayList<>();

        File[] files = this.path.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory() && List.of(Dimension.names).contains(file.getName())) {
                    dimensions.add(file.getName());
                }
            }
        }

        return dimensions;
    }

    public HashMap<String, Waypoint> getWaypoints() {
        return waypoints;
    }

    public BufferedImage getRegion(Dimension dimension, MapType mapType, int x, int z) throws Exception {
        if (mapType == MapType.CAVE)
            throw new Exception("when using the cave map, you must include a Y value");

        System.out.printf("%s/%s/%s/%d,%d.png%n", this.getFullPath(), dimension.getId(), mapType.getId(), x, z);
        File regionPath = new File(String.format("%s/%s/%s/%d,%d.png", this.getFullPath(), dimension.getId(), mapType.getId(), x, z));
        if (!regionPath.exists()) {
            System.out.printf("region %d,%d has not been mapped yet.\n", x, z);
            return null;
        }

        return ImageIO.read(regionPath);
    }

    public BufferedImage getRegion(Dimension dimension, MapType mapType, PointXZ pointZX) throws Exception {
        return getRegion(dimension, mapType, pointZX.x,  pointZX.z);
    }

    public BufferedImage getCaveRegion(Dimension dimension, int x, int y, int z) throws Exception {
        if (y > 23 || y < -4) throw new Exception("Y value needs to be between -4 and 23 inclusive");

        System.out.printf("%s/%s/%d/%d,%d.png%n", this.getFullPath(), dimension.getId(), y, x, z);
        File regionPath = new File(String.format("%s/%s/%d/%d,%d.png", this.getFullPath(), dimension.getId(), y, x, z));
        if (!regionPath.exists()) {
            System.out.printf("region %d/%d,%d has not been mapped yet.\n", y, x, z);
            return null;
        }

        return ImageIO.read(regionPath);
    }

    public BufferedImage getCaveRegion(Dimension dimension, int y, PointXZ pointXZ) throws Exception {
        return getCaveRegion(dimension, pointXZ.x, y, pointXZ.z);
    }
}