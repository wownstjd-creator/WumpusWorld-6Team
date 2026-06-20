package agent;

import java.util.*;

public abstract class Agent {
    protected int x = 1, y = 1;
    protected Direction dir = Direction.EAST;
    protected boolean hasGold = false;
    protected boolean wumpusDead = false;
    protected Cell[][] kb = new Cell[5][5];
    protected Deque<ActionType> plan = new ArrayDeque<>();

    public Agent() {
        for (int i = 1; i <= 4; i++)
            for (int j = 1; j <= 4; j++)
                kb[i][j] = new Cell(i, j);
        kb[1][1].setSafe(true);
        kb[1][1].setVisited(true);
    }

    public abstract ActionType nextAction(Percept p);

    public void applyMove(boolean bump) {
        if (!bump) {
            switch (dir) {
                case NORTH: if (y < 4) y++; break;
                case EAST:  if (x < 4) x++; break;
                case SOUTH: if (y > 1) y--; break;
                case WEST:  if (x > 1) x--; break;
            }
        }
    }
}
