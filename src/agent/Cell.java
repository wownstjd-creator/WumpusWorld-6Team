package agent;

public class Cell {
    private int x, y;
    private boolean safe = false;
    private boolean visited = false;
    private boolean stench = false;
    private boolean breeze = false;
    private boolean glitter = false;

    public Cell(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public boolean isSafe() {
        return safe;
    }

    public void setSafe(boolean safe) {
        this.safe = safe;
    }

    public boolean isVisited() {
        return visited;
    }

    public void setVisited(boolean visited) {
        this.visited = visited;
    }

    public void setStench(boolean stench) {
        this.stench = stench;
    }

    public void setBreeze(boolean breeze) {
        this.breeze = breeze;
    }

    public void setGlitter(boolean glitter) {
        this.glitter = glitter;
    }

    public boolean hasStench() {
        return stench;
    }

    public boolean hasBreeze() {
        return breeze;
    }

    public boolean hasGlitter() {
        return glitter;
    }
}
