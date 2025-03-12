package com.aionemu.gameserver.path;

import java.util.ArrayList;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.controllers.movement.NpcMoveController;

// Adapted from https://github.com/hax0r31337/Astar3d.

public class Pathfinder {
    private static final Logger log = LoggerFactory.getLogger(Pathfinder.class);

    public static Cell[] COMMON_NEIGHBORS = new Cell[] {
            new Cell(1, 0, 0),
            new Cell(-1, 0, 0),
            new Cell(0, 1, 0),
            new Cell(0, -1, 0),
            //new Cell(0, 0, 1),
            //new Cell(0, 0, -1)
    };
    public static Cell[] DIAGONAL_NEIGHBORS = new Cell[] {
            new Cell(1, 1, 0),
            new Cell(-1, -1, 0),
            new Cell(1, -1, 0),
            new Cell(-1, 1, 0),
            //new Cell(0, 1, 1),
            //new Cell(0, -1, -1),
            //new Cell(0, 1, -1),
            //new Cell(0, -1, 1),
            // also include the non-diagonal neighbors
            new Cell(1, 0, 0),
            new Cell(-1, 0, 0),
            new Cell(0, 1, 0),
            new Cell(0, -1, 0),
            //new Cell(0, 0, 1),
            //new Cell(0, 0, -1)
    };

    public static Cell[] neighbours;
    public static float gridStep;

    private final Cell start;
    private final Cell end;
    private Npc owner;
    private final float zTolerance;

    public Pathfinder(final Cell start, final Cell end, final float zTolerance)
    {
        this.start = start;
        this.end = end;
        this.zTolerance = zTolerance;
    }

    public static void setGridStepping(final Cell[] pneighbours, float pgridStep) {
      Pathfinder.gridStep = pgridStep;
      Pathfinder.neighbours = pneighbours;
      for (Cell neighbor : Pathfinder.neighbours) {
        neighbor.x *= pgridStep;
        neighbor.y *= pgridStep;
        neighbor.z *= pgridStep;
      }
    }

    public Cell getStart() {
        return start;
    }

    public Cell getEnd() {
        return end;
    }

    public static Cell[] getNeighbours() {
        return Pathfinder.neighbours;
    }

    public ArrayList<Cell> findPath() {
        return findPath(Integer.MAX_VALUE);
    }

    public void setOwner(Npc owner) {
      this.owner = owner;
    }

    /**
     * @param maxLoops used to prevent infinite loops caused by invalid path
     */
    public ArrayList<Cell> findPath(int maxLoops) {
        final ArrayList<Cell> open = new ArrayList<>();
        final ArrayList<Cell> closed = new ArrayList<>();

        open.add(start);

        Cell current = null;
        int currentIdx;
        int loops = 0;

        // Loop until you find the end
        while (!open.isEmpty() && loops < maxLoops) {
            // Get the current node
            current = open.get(0);
            currentIdx = 0;
            for(int i = 1; i < open.size(); i++) {
                if(open.get(i).f < current.f) {
                    current = open.get(i);
                    currentIdx = i;
                }
            }

            // Pop current off open list, add to closed list
            open.remove(currentIdx);
            closed.add(current);


            // Found the goal
            //log.info("[pathfinder] current: "+current.toString());
            //log.info("[pathfinder] end: "+end.toString());
            float dt = current.diff(end);
            //log.info("[pathfinder] Pathfinder.gridStep: "+Pathfinder.gridStep);
            if(current.closeto(end, Pathfinder.gridStep)) {
                break;
            }

            //log.info("[Pathfinder] current: "+current.toString());

            // Generate children
            final ArrayList<Cell> children = new ArrayList<>();
            for(final Cell neighbor : Pathfinder.neighbours) {

                final Cell child = new Cell(current.x + neighbor.x, current.y + neighbor.y, 0);
                child.z = this.owner.getMoveController().getZ(this.owner, child.x, child.y, current.z);
                child.parent = current;

                //log.info("[Pathfinder] child: "+child.toString());
                //log.info("[Pathfinder] dx: "+Math.abs(current.x-child.x)+",dy: "+Math.abs(current.y-child.y)+",dz: "+Math.abs(current.z-child.z));
                //log.info("[Pathfinder] dz: "+Math.abs(current.z-child.z));

                if (current.isBlocked(child, this.zTolerance)) {
                  //log.info("[Pathfinder] child is Blocked.");
                  continue;
                }
                //log.info("[Pathfinder] child is not Blocked.");

                children.add(child);
            }

            // Loop through children
            for(final Cell child : children) {
                // Child is on the closed list
                if(closed.contains(child)) {
                    continue;
                }

                // Create the f, g, and h values
                child.g = current.g + 1;
                int hx = (int) Math.pow(Math.abs(child.x - end.x), 2);
                int hy = (int) Math.pow(Math.abs(child.y - end.y), 2);
                int hz = 2 * ((int)Math.pow(Math.abs(child.z - end.z), 2));
                child.h = (int) (hx + hy + hz);
                child.f = child.g + child.h;

                // Child is already in the open list
                if(open.contains(child) && open.get(open.indexOf(child)).g > child.g) {
                    continue;
                }

                open.add(child);
            }

            loops++;
        }

        //log.info("[Pathfinder] loops:"+loops);

        if (loops >= maxLoops)
            return new ArrayList<Cell>();

        final ArrayList<Cell> path = new ArrayList<>();
        Cell cur = current;
        while(cur != null) {
            path.add(cur);
            cur = cur.parent;
        }
        // Reverse the list
        Collections.reverse(path);
        /*String listString = "";

        for (Cell s : path)
        {
            listString += s.toString() + ", ";
        }

        log.info("[Pathfinder] result: "+listString);*/
        return path;
    }
}
