package agent;

import java.util.*;

public class World {
    private Cell[][] grid     = new Cell[5][5];
    private boolean[][] hasPit  = new boolean[5][5];
    private boolean[][] hasGold = new boolean[5][5];
    private int wumpusX, wumpusY;
    private boolean wumpusAlive = true;
    private int agentX = 1, agentY = 1;
    private Direction agentDir = Direction.EAST; 
    private boolean hasGoldFlag = false;
    private boolean done = false;

    public World() {
        Random rand = new Random();
        for (int x = 1; x <= 4; x++)
            for (int y = 1; y <= 4; y++)
                grid[x][y] = new Cell(x, y);

        // 금 위치 결정 (1,1 제외)
        int gx, gy;
        do { gx = rand.nextInt(4) + 1; gy = rand.nextInt(4) + 1; }
        while (gx == 1 && gy == 1);
        hasGold[gx][gy] = true;

        // Wumpus 위치 결정
        do {
            wumpusX = rand.nextInt(4) + 1;
            wumpusY = rand.nextInt(4) + 1;
        } while ((wumpusX == 1 && wumpusY == 1) || (wumpusX == gx && wumpusY == gy));

        // 구덩이 위치 결정 (15% 확률)
        for (int x = 1; x <= 4; x++) {
            for (int y = 1; y <= 4; y++) {
                if ((x == 1 && y == 1) || (x == gx && y == gy) || (x == wumpusX && y == wumpusY)) {
                    hasPit[x][y] = false;
                } else {
                    hasPit[x][y] = rand.nextDouble() < 0.15;
                }
            }
        }
    }

    public Percept step(ActionType action) {
        boolean bump = false, scream = false;

        switch (action) {
            case GO_FORWARD:
                int nx = agentX, ny = agentY;
                switch (agentDir) {
                    case NORTH: ny++; break;
                    case EAST:  nx++; break;
                    case SOUTH: ny--; break;
                    case WEST:  nx--; break;
                }
                if (nx < 1 || nx > 4 || ny < 1 || ny > 4) {
                    bump = true;
                } else {
                    agentX = nx;
                    agentY = ny;
                    // 함정 밟으면 사망 미션 즉시 실패 처리
                    if (hasPit[agentX][agentY] || (wumpusAlive && agentX == wumpusX && agentY == wumpusY)) {
                        done = true;
                    }
                }
                break;

            case TURN_LEFT:
                agentDir = agentDir.turnLeft();
                break;

            case TURN_RIGHT:
                agentDir = agentDir.turnRight();
                break;

            case GRAB:
                if (hasGold[agentX][agentY]) {
                    hasGold[agentX][agentY] = false;
                    hasGoldFlag = true;
                }
                break;

            case SHOOT:
                if (wumpusAlive) {
                    if ((agentDir == Direction.NORTH && agentX == wumpusX && wumpusY > agentY)
                     || (agentDir == Direction.SOUTH && agentX == wumpusX && wumpusY < agentY)
                     || (agentDir == Direction.EAST  && agentY == wumpusY && wumpusX > agentX)
                     || (agentDir == Direction.WEST  && agentY == wumpusY && wumpusX < agentX)) {
                        wumpusAlive = false;
                        scream = true;
                    }
                }
                break;

            case CLIMB:
                done = true;
                break;
        }

        boolean stench = false, breeze = false, glitter = false;
        if (wumpusAlive && Math.abs(agentX - wumpusX) + Math.abs(agentY - wumpusY) == 1) {
            stench = true;
        }
        for (Direction d : Direction.values()) {
            int x = agentX + dx(d), y = agentY + dy(d);
            if (x >= 1 && x <= 4 && y >= 1 && y <= 4 && hasPit[x][y]) {
                breeze = true;
            }
        }
        if (hasGold[agentX][agentY]) {
            glitter = true;
        }

        return new Percept(stench, breeze, glitter, bump, scream);
    }

    public boolean isDone() { return done; }

    public Percept perceive() {
        boolean stench = wumpusAlive && (Math.abs(agentX - wumpusX) + Math.abs(agentY - wumpusY) == 1);
        boolean breeze = checkBreeze();
        boolean glitter = hasGold[agentX][agentY];
        return new Percept(stench, breeze, glitter, false, false);
    }

    private boolean checkBreeze() {
        for (Direction d : Direction.values()) {
            int x = agentX + dx(d), y = agentY + dy(d);
            if (x >= 1 && x <= 4 && y >= 1 && y <= 4 && hasPit[x][y]) return true;
        }
        return false;
    }

    public int getAgentX() { return agentX; }
    public int getAgentY() { return agentY; }
    public Direction getAgentDir() { return agentDir; }

    private int dx(Direction d) {
        switch (d) { case EAST: return 1; case WEST: return -1; default: return 0; }
    }
    private int dy(Direction d) {
        switch (d) { case NORTH: return 1; case SOUTH: return -1; default: return 0; }
    }

    public boolean isWumpus(int x, int y) { return wumpusAlive && (wumpusX == x && wumpusY == y); }
    public boolean isPit(int x, int y) { return (x >= 1 && x <= 4 && y >= 1 && y <= 4) ? this.hasPit[x][y] : false; }
    public boolean isGold(int x, int y) { return (x >= 1 && x <= 4 && y >= 1 && y <= 4) ? this.hasGold[x][y] : false; }
}