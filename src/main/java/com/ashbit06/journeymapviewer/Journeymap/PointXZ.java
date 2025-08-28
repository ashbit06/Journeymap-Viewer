package com.ashbit06.journeymapviewer.Journeymap;

import java.awt.*;

public class PointXZ extends Point {
    public final int x;
    public final int z;

    public PointXZ(int x, int z) {
        this.x = x;
        this.z = z;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PointXZ other)) return false;
        return this.x == other.x && this.z == other.z;
    }

    @Override
    public int hashCode() {
        return 31 * x + z;
    }

    @Override
    public String toString() {
        return String.format("(%d,%d)", x, z);
    }
}