package com.aionemu.gameserver.path;

import java.lang.Math;

public class Cell {

    public float x;
    public float y;
    public float z;

    public float g = 0;
    public float h = 0;
    public float f = 0;

    public Cell parent;

    public Cell(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public boolean equals(Object o) {
        if (o instanceof Cell) {
            Cell c = (Cell) o;
            return c.x == x && c.y == y && c.z == z;
        }
        return false;
    }

    public boolean closeto(Cell c, float offset) {
        boolean pos = Math.abs(c.x-x) <= offset && Math.abs(c.y-y) <= offset; //&&
        //if (pos)
          //return pos && Math.abs(c.z-z) <= offset;
        return pos;
          //Math.abs(c.z-z) < offset;
    }

    public float diff(Cell c) {
      return Math.abs(c.x-x) + Math.abs(c.y-y);
    }

    public boolean isBlocked(Cell c, float tolerance) {
      float dx = Math.abs(c.x-x);
      float dy = Math.abs(c.y-y);
      float tx = (float) Math.pow(dx,2f);
      float ty = (float) Math.pow(dy,2f);

      float dt = (float) Math.pow(tx + ty, 0.5f);
      float dz = Math.abs(c.z-z);
      return (dz <= (dt * tolerance));
    }

    public int hashCode() {
        return Math.round(x * 31 + y * 31 + z * 31);
    }

    public String toString() {
        return "Cell(" + x + ", " + y + ", " + z + ")";
    }
}
