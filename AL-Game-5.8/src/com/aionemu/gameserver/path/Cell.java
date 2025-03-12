package com.aionemu.gameserver.path;

import java.lang.Math;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Cell {
    private static final Logger log = LoggerFactory.getLogger(Cell.class);

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
        //log.info("[Cell] closeto called.");
        //log.info("[Cell] this:"+toString());
        //log.info("[Cell] c:"+c.toString());
        float dx = Math.abs(x-c.x);
        float dy = Math.abs(y-c.y);
        //log.info("[Cell] offset:"+offset);
        //log.info("[Cell] dx:"+dx);
        //log.info("[Cell] dy:"+dy);
        boolean res = (dx <= offset) && (dy <= offset); //&&
        //log.info("[Cell] result:"+res);
        return res;
    }

    public float diff(Cell c) {
      return Math.abs(c.x-x) + Math.abs(c.y-y);
    }

    public boolean isBlocked(Cell c, float zTolerance) {
      //log.info("[Cell] isBlocked called.");
      float dx = Math.abs(x-c.x);
      float dy = Math.abs(y-c.y);
      float tx = (float) Math.pow(dx,2f);
      float ty = (float) Math.pow(dy,2f);

      float dt = (float) Math.pow(tx + ty, 0.5f);
      float dz = (float) Math.abs(c.z-z);
      //log.info("[Cell] dt: "+dt);
      //log.info("[Cell] dz: "+dz+", dt: "+dt+", dtz: "+(dt * zTolerance));
      boolean result = (dz > (dt * zTolerance));
      //log.info("[Cell] result: "+result);
      return result;
    }

    public int hashCode() {
        return Math.round(x * 31 + y * 31 + z * 31);
    }

    public String toString() {
        return "Cell(" + x + ", " + y + ", " + z + ")";
    }
}
