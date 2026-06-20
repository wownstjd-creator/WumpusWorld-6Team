package agent;

import java.util.*;

public class WumpusGame {
    public static void main(String[] args) {
        World world = new World();
        Agent agent = new LogicalExplorer();
        Percept percept = new Percept(false, false, false, false, false);
        Scanner scanner = new Scanner(System.in);

        while (!world.isDone()) {
            ActionType action = agent.nextAction(percept);
            percept = world.step(action);
            if (action == ActionType.GO_FORWARD) {
                agent.applyMove(percept.bump);
            }
            System.out.println("Position: (" + agent.x + ", " + agent.y + ") Dir=" + agent.dir);
           
            System.out.println("Action: " + action + ", Percept: [Stench=" + percept.stench
                    + ", Breeze=" + percept.breeze + ", Glitter=" + percept.glitter
                    + ", Bump=" + percept.bump + ", Scream=" + percept.scream + "]");
            System.out.print("Press Enter to continue...");
            scanner.nextLine();
        }
        System.out.println("Game over.");
        scanner.close();
    }
}
