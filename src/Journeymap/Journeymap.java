package Journeymap;

import net.querz.nbt.io.NBTDeserializer;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.StringTag;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.NotDirectoryException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class Journeymap {
    private final File path;
    private World world;
    private HashMap<String, Waypoint> waypoints;

    public Journeymap(String path) throws Exception {
        this.path = new File(path);
        if (!this.path.exists()) throw new FileNotFoundException();
        if (!this.path.isDirectory()) throw new NotDirectoryException(path);
        if (this.path.getName().compareTo("journeymap") != 0)
            throw new Exception("selected folder needs to be named \"journeymap\"");
    }

    private void setWaypoints() throws IOException {
        File file = new File(getFullPath()+"/waypoints/WaypointData.dat");
        NBTDeserializer deserializer = new NBTDeserializer(false);
        CompoundTag root = (CompoundTag) deserializer.fromFile(file).getTag();

        CompoundTag waypoints = root.getCompoundTag("waypoints");
        this.waypoints = new HashMap<>();

        for (String key : waypoints.keySet()) {
            CompoundTag wp = waypoints.getCompoundTag(key);
            CompoundTag pos = wp.getCompoundTag("pos");
            ListTag<StringTag> dims = (ListTag<StringTag>) wp.getListTag("dimensions");
            CompoundTag icon = wp.getCompoundTag("icon");

            Dimension primary;
            try {
                primary = Dimension.from(pos.getString("dimension"));
            } catch (Exception e) {
                primary = Dimension.OVERWORLD;
            }

            List<Dimension> dimensions = new ArrayList<>();
            dims.forEach(d -> {
                try {
                    dimensions.add(Dimension.from(d.getValue()));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            WaypointIcon waypointIcon;
            try {
                String[] a = icon.getString("resourceLocation").split("/");
                waypointIcon = new WaypointIcon(
                        String.format("assets/%s", a[a.length-1]),
                        new Color(wp.getInt("color")),
                        icon.getInt("textureWidth"),
                        icon.getInt("textureHeight")
                );
                System.out.println(icon.getString("resourceLocation"));
            } catch (FileNotFoundException e) {
                try {
                    waypointIcon = new WaypointIcon(
                            new File("assets/waypoint-icon.png"),
                            new Color(wp.getInt("color")),
                            icon.getInt("textureWidth"),
                            icon.getInt("textureHeight")
                    );
                } catch (FileNotFoundException ex) {
                    throw new RuntimeException(ex);
                }
            }

            Waypoint waypoint = new Waypoint(
                    wp.getString("name"),
                    new PointXYZ(
                            pos.getInt("x"),
                            pos.getInt("y"),
                            pos.getInt("z")
                    ),
                    primary,
                    dimensions.toArray(new Dimension[0]),
                    waypointIcon,
                    wp.getString("guid")
            );
//            System.out.println(waypoint);

            this.waypoints.put(waypoint.guid(), waypoint);
        }
    }

    public File getPath() { return this.path; }

    public String getFullPath() {
        return String.format("%s/data/%s", this.path.toString(), this.world.toString());
    }

    public World getWorld() { return this.world; }

    public void setWorld(World w) throws IOException {
        world = w;
        setWaypoints();
    }

    public List<String> getWorldList(boolean retrieveMultiplayer) {
        File w = new File(path + String.format("/data/%sp", retrieveMultiplayer ? 'm' : 's'));
        List<String> worlds = new ArrayList<>(List.of(Objects.requireNonNull(w.list())));
        worlds.remove(".DS_Store");

        return worlds;
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

//        System.out.printf("%s/%s/%s/%d,%d.png%n", this.getFullPath(), dimension.getId(), mapType.getId(), x, z);
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

//        System.out.printf("%s/%s/%d/%d,%d.png%n", this.getFullPath(), dimension.getId(), y, x, z);
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