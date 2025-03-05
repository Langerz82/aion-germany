package com.aionemu.gameserver.path;

import java.util.ArrayList;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.controllers.movement.NpcMoveController;

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

    private final Cell start;
    private final Cell end;
    private final Cell[] neighbours;
    private Npc owner;
    private final float offset;
    private final float tolerance;

    public Pathfinder(final Cell start, final Cell end, final Cell[] neighbours,
    float offset, float tolerance) {
        this.start = start;
        this.end = end;
        this.neighbours = neighbours;
        this.offset = offset;
        this.tolerance = tolerance;
        for (Cell neighbor : this.neighbours) {
          neighbor.x *= offset;
          neighbor.y *= offset;
          neighbor.z *= offset;
        }
    }

    public Cell getStart() {
        return start;
    }

    public Cell getEnd() {
        return end;
    }

    public Cell[] getNeighbours() {
        return neighbours;
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
            if(current.closeto(end, this.offset)) {
                break;
            }

            // Generate children
            final ArrayList<Cell> children = new ArrayList<>();
            for(final Cell neighbor : neighbours) {
                neighbor.z = this.owner.getMoveController().getZ(this.owner);
                final Cell child = new Cell(current.x + neighbor.x, current.y + neighbor.y, current.z + neighbor.z);
                child.parent = current;

                if (current.isBlocked(child, this.tolerance))
                  continue;

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
                child.h = (int) (Math.pow(child.x - end.x, 2) + Math.pow(child.y - end.y, 2) + Math.pow(child.z - end.z, 2));
                child.f = child.g + child.h;

                // Child is already in the open list
                if(open.contains(child) && open.get(open.indexOf(child)).g > child.g) {
                    continue;
                }

                open.add(child);
            }

            loops++;
        }

        final ArrayList<Cell> path = new ArrayList<>();
        Cell cur = current;
        while(cur != null) {
            path.add(cur);
            cur = cur.parent;
        }
        // Reverse the list
        Collections.reverse(path);

        return path;
    }
}
