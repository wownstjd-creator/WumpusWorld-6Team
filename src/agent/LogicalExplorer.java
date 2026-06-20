package agent;

import java.util.*;

public class LogicalExplorer extends Agent {
    private boolean wumpusRemoved = false;
    private boolean shotFired = false; 
    private int[][] pitProbability = new int[5][5];
    private int[][] wumpusProbability = new int[5][5];
    private boolean initialized = false;
    private int lastX = 1, lastY = 1;

    private void initArrays() {
        for (int i = 0; i < 5; i++) {
            Arrays.fill(pitProbability[i], 0);
            Arrays.fill(wumpusProbability[i], 0);
        }
        pitProbability[1][1] = -1;
        wumpusProbability[1][1] = -1;
        initialized = true;
    }

    @Override
    public ActionType nextAction(Percept p) {
        if (!initialized) initArrays();
        if (p.bump) { plan.clear(); x = lastX; y = lastY; return ActionType.TURN_RIGHT; }

        lastX = x; lastY = y;
        kb[x][y].setVisited(true);
        kb[x][y].setSafe(true);

        if (p.scream) {
            wumpusRemoved = true;
            shotFired = true;
            for (int i = 1; i <= 4; i++) Arrays.fill(wumpusProbability[i], -1);
        }
        updateKnowledge(p);

        if (p.stench && !wumpusRemoved && !shotFired && plan.isEmpty()) {
            Direction targetDir = findWumpusDirection();
            if (targetDir != null) {
                plan.clear();
                Direction curr = dir;
                while (curr != targetDir) {
                    plan.add(ActionType.TURN_RIGHT);
                    curr = curr.turnRight();
                }
                plan.add(ActionType.SHOOT);
                shotFired = true;
                return doAction();
            }
        }

        if (p.glitter && !hasGold) { hasGold = true; plan.clear(); return ActionType.GRAB; }

        if (hasGold) {
            if (x == 1 && y == 1) return ActionType.CLIMB;
            if (plan.isEmpty()) plan = makePath(x, y, 1, 1, true);
        } else if (plan.isEmpty()) {
            int[] target = findNearestUnvisitedSafe();
            if (target != null) {
                plan = makePath(x, y, target[0], target[1], true);
            } else {
                int[] riskyTarget = findBestRiskCell();
                if (riskyTarget != null) {
                    plan = makePath(x, y, riskyTarget[0], riskyTarget[1], false);
                } else {
                    return ActionType.TURN_RIGHT;
                }
            }
        }
        return doAction();
    }

    private int[] findBestRiskCell() {
        int bestX = -1, bestY = -1;
        double minRisk = Double.MAX_VALUE;

        for (int i = 1; i <= 4; i++) {
            for (int j = 1; j <= 4; j++) {
                if (!kb[i][j].isVisited()) {
                    double pProb = Math.max(0, pitProbability[i][j]);
                    double wProb = Math.max(0, wumpusProbability[i][j]);
                    
                    // 핵심: 중복된 위험(pProb가 높음)은 제곱하여 페널티를 대폭 강화(10.0배)
                    // 거리를 가중치에 추가하여 너무 가까운 곳만 맴도는 현상 방지
                    double distance = Math.abs(i - x) + Math.abs(j - y);
                    double currentRisk = (pProb * pProb * 10.0) + (wProb * wProb * 10.0) + (distance * 0.5);
                    
                    if (currentRisk < minRisk) {
                        minRisk = currentRisk;
                        bestX = i; bestY = j;
                    }
                }
            }
        }
        return (bestX != -1) ? new int[]{bestX, bestY} : null;
    }

    private void updateKnowledge(Percept p) {
        if (!p.stench && !wumpusRemoved) {
            for (Direction d : Direction.values()) {
                int nx = x + dx(d), ny = y + dy(d);
                if (inBounds(nx, ny)) wumpusProbability[nx][ny] = -999;
            }
        } else if (p.stench && !wumpusRemoved) {
            for (Direction d : Direction.values()) {
                int nx = x + dx(d), ny = y + dy(d);
                if (inBounds(nx, ny) && wumpusProbability[nx][ny] > -999) wumpusProbability[nx][ny]++;
            }
        }
        
        if (!p.breeze) {
            for (Direction d : Direction.values()) {
                int nx = x + dx(d), ny = y + dy(d);
                if (inBounds(nx, ny)) pitProbability[nx][ny] = -999;
            }
        } else {
            for (Direction d : Direction.values()) {
                int nx = x + dx(d), ny = y + dy(d);
                if (inBounds(nx, ny) && pitProbability[nx][ny] != -999) pitProbability[nx][ny]++;
            }
        }

        for (int i = 1; i <= 4; i++) {
            for (int j = 1; j <= 4; j++) {
                boolean safeW = (wumpusRemoved || wumpusProbability[i][j] == -999);
                boolean safeP = (pitProbability[i][j] == -999);
                kb[i][j].setSafe(kb[i][j].isVisited() || (safeW && safeP));
            }
        }
    }

    private Direction findWumpusDirection() {
        int maxProb = 0;
        Direction bestDir = null;
        int count = 0;
        for (Direction d : Direction.values()) {
            int nx = x + dx(d), ny = y + dy(d);
            if (inBounds(nx, ny) && wumpusProbability[nx][ny] > maxProb) {
                maxProb = wumpusProbability[nx][ny];
                bestDir = d;
                count = 1;
            } else if (inBounds(nx, ny) && wumpusProbability[nx][ny] == maxProb && maxProb > 0) {
                count++;
            }
        }
        return (count == 1) ? bestDir : null;
    }

   
    public boolean[][] getVisitedMap() { boolean[][] v = new boolean[4][4]; for (int i = 1; i <= 4; i++) for (int j = 1; j <= 4; j++) v[i-1][j-1] = kb[i][j].isVisited(); return v; }
    public boolean[][] getSafeMap() { boolean[][] s = new boolean[4][4]; for (int i = 1; i <= 4; i++) for (int j = 1; j <= 4; j++) s[i-1][j-1] = kb[i][j].isSafe(); return s; }
    private int[] findNearestUnvisitedSafe() { Queue<int[]> q = new LinkedList<>(); q.add(new int[]{x, y}); boolean[][] visited = new boolean[5][5]; while (!q.isEmpty()) { int[] cur = q.poll(); if (!kb[cur[0]][cur[1]].isVisited() && kb[cur[0]][cur[1]].isSafe()) return cur; for (Direction d : Direction.values()) { int nx = cur[0] + dx(d), ny = cur[1] + dy(d); if (inBounds(nx, ny) && !visited[nx][ny] && kb[nx][ny].isSafe()) { visited[nx][ny] = true; q.add(new int[]{nx, ny}); } } } return null; }
    private Deque<ActionType> makePath(int sx, int sy, int gx, int gy, boolean safeOnly) { Queue<Node> q = new LinkedList<>(); q.add(new Node(sx, sy, null, null)); boolean[][] seen = new boolean[5][5]; seen[sx][sy] = true; Node target = null; while (!q.isEmpty()) { Node n = q.poll(); if (n.x == gx && n.y == gy) { target = n; break; } for (Direction d : Direction.values()) { int nx = n.x + dx(d), ny = n.y + dy(d); if (inBounds(nx, ny) && !seen[nx][ny]) { if (safeOnly && !kb[nx][ny].isSafe()) continue; seen[nx][ny] = true; q.add(new Node(nx, ny, n, d)); } } } Deque<ActionType> path = new ArrayDeque<>(); if (target != null) { List<Direction> dirs = new ArrayList<>(); while (target.prev != null) { dirs.add(target.dirFromPrev); target = target.prev; } Collections.reverse(dirs); Direction curr = dir; for (Direction d : dirs) { while (curr != d) { path.add(ActionType.TURN_RIGHT); curr = curr.turnRight(); } path.add(ActionType.GO_FORWARD); } } return path; }
    private ActionType doAction() { if (plan.isEmpty()) return ActionType.TURN_RIGHT; ActionType next = plan.poll(); if (next == ActionType.TURN_LEFT) dir = dir.turnLeft(); if (next == ActionType.TURN_RIGHT) dir = dir.turnRight(); return next; }
    private int dx(Direction d) { return (d == Direction.EAST) ? 1 : (d == Direction.WEST) ? -1 : 0; }
    private int dy(Direction d) { return (d == Direction.NORTH) ? 1 : (d == Direction.SOUTH) ? -1 : 0; }
    private boolean inBounds(int x, int y) { return x >= 1 && x <= 4 && y >= 1 && y <= 4; }
    private static class Node { int x, y; Node prev; Direction dirFromPrev; Node(int x, int y, Node p, Direction d) { this.x = x; this.y = y; prev = p; dirFromPrev = d; } }
   
public int[][] getPitRiskMap() {
    return pitProbability; 
}

public int[][] getWumpusRiskMap() {
    return wumpusProbability; 
}
}