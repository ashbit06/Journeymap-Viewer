package com.ashbit06.journeymapviewer.Journeymap;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;


public final class WaypointIcon {
    public static final List<String> ASSET_LIST = List.of(
        "waypoint-icon.png",
        "waypoint-death-icon.png",
        "waypoint-house.png",
        "waypoint-house-2.png"
    );

    private final InputStream assetStream;
    private final Color color;
    private final int width;
    private final int height;

    public WaypointIcon(String resourceName, Color color, int width, int height) throws FileNotFoundException {
        if (!ASSET_LIST.contains(resourceName)) this.assetStream = WaypointIcon.class.getResourceAsStream("/assets/waypoint-icon.png");
        else this.assetStream = WaypointIcon.class.getResourceAsStream("/assets/" + resourceName);

        this.color = color;
        this.width = width;
        this.height = height;
    }

    public BufferedImage render() throws IOException {
        BufferedImage icon = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = icon.createGraphics();
        g2.drawImage(ImageIO.read(assetStream), 0, 0, null);
        g2.setComposite(AlphaComposite.SrcAtop);
        g2.setColor(color);
        g2.fillRect(0, 0, width, height);
        g2.dispose();

        return icon;
    }

    public InputStream getAssetStream() {
        return assetStream;
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
        return Objects.equals(this.assetStream, that.assetStream) &&
                Objects.equals(this.color, that.color) &&
                this.width == that.width &&
                this.height == that.height;
    }

    @Override
    public int hashCode() {
        return Objects.hash(assetStream, color, width, height);
    }

    @Override
    public String toString() {
        return "WaypointIcon[" +
                "assetFile=" + assetStream + ", " +
                "color=" + color + ", " +
                "width=" + width + ", " +
                "height=" + height + ']';
    }

}
