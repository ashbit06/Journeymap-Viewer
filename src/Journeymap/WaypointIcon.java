package Journeymap;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Objects;
import java.util.List;


public final class WaypointIcon {
    private final File assetFile;
    private final Color color;
    private final int width;
    private final int height;

    public WaypointIcon(String resourceLocation, Color color, int width, int height) throws FileNotFoundException {
        System.out.println(resourceLocation);

        List<String> assets = java.util.List.of(Objects.requireNonNull(new File("assets").list()));
        if (!assets.contains(resourceLocation)) this.assetFile = new File("assets/waypoint-icon.png");
        else if (new File(resourceLocation).exists()) this.assetFile = new File(resourceLocation);
        else throw new FileNotFoundException();

        this.color = color;
        this.width = width;
        this.height = height;
    }

    public WaypointIcon(File assetFile, Color color, int width, int height) throws FileNotFoundException {
        if (!assetFile.exists()) throw new FileNotFoundException();

        this.assetFile = assetFile;
        this.color = color;
        this.width = width;
        this.height = height;
    }

    public BufferedImage render() throws IOException {
        BufferedImage icon = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = icon.createGraphics();
        g2.drawImage(ImageIO.read(assetFile), 0, 0, null);
        g2.setComposite(AlphaComposite.SrcAtop);
        g2.setColor(color);
        g2.fillRect(0, 0, width, height);
        g2.dispose();

        return icon;
    }

    public File getAssetFile() {
        return assetFile;
    }

    public Color getColor() {
        return color;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (WaypointIcon) obj;
        return Objects.equals(this.assetFile, that.assetFile) &&
                Objects.equals(this.color, that.color) &&
                this.width == that.width &&
                this.height == that.height;
    }

    @Override
    public int hashCode() {
        return Objects.hash(assetFile, color, width, height);
    }

    @Override
    public String toString() {
        return "WaypointIcon[" +
                "assetFile=" + assetFile + ", " +
                "color=" + color + ", " +
                "width=" + width + ", " +
                "height=" + height + ']';
    }

}
