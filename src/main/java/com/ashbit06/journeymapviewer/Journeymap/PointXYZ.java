package com.ashbit06.journeymapviewer.Journeymap;

import java.awt.*;

public class PointXYZ extends Point {
    public final int z;

    public PointXYZ(int x, int y, int z) {
        super(x, y);
        this.z = z;
    }

    public PointXZ getXZ() {
        return new PointXZ(x, z);
    }

    @Override
    public String toString() {
        return String.format("PointXYZ[x=%d,y=%d,z=%d]", x, y, z);
    }
}
