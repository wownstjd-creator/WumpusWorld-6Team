package agent;

import javax.swing.*;
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.concurrent.*;

public class Simulator {
    private static JFrame guiFrame;
    private static DualWorldPanel dualPanel;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("\n========================================");
            System.out.println("   Wumpus World 듀얼 모니터 시뮬레이터");
            System.out.println("=======================================");
            System.out.println(" [1] 실시간 탐색 모드 (듀얼 GUI 시각화 출력)");
            System.out.println(" [2] 대량 검증 모드 (100판 고속 통계 분석)");
            System.out.println(" [3] 프로그램 종료");
            System.out.println(" [4] 테스트 실행 모드 (콘솔 단계별 수동 디버깅)");
            System.out.println("----------------------------------------");
            System.out.print(" 실행할 모드의 번호를 입력하세요: ");
            
            String input = scanner.nextLine().trim(); 
            
            if (input.equals("1")) {
                runSingleGameMode();
            } else if (input.equals("2")) {
                runMassiveSimulationMode();
            } else if (input.equals("3")) {
                System.out.println("시뮬레이터를 종료합니다.");
                break;
            } else if (input.equals("4")) {
                runTestExecutionMode(scanner); 
            } else {
                System.out.println("잘못된 입력입니다. 1~4 사이의 숫자를 입력해주세요.");
            }
        }
        scanner.close();
    }

    private static void runSingleGameMode() {
        try {
            World world = new World();
            LogicalExplorer agent = new LogicalExplorer(); 
            Percept percept = world.perceive();

            initGUI();

            int stepCount = 1;
            while (!world.isDone() && stepCount <= 100) {
                ActionType action = agent.nextAction(percept);
                
                System.out.printf("[Turn %d] 위치:(%d,%d) | 방향:%s | 행동:[%s]%n", 
                        stepCount, world.getAgentX(), world.getAgentY(), world.getAgentDir(), action);

                if (action == ActionType.SHOOT) {
                    dualPanel.triggerShootEffect(world.getAgentX(), world.getAgentY(), world.getAgentDir().toString());
                }

                dualPanel.updateState(world, agent, action);
                Thread.sleep(600); 

                percept = world.step(action);
                if (action == ActionType.GO_FORWARD) {
                    agent.applyMove(percept.bump);
                }

                if (world.isPit(world.getAgentX(), world.getAgentY()) || 
                    world.isWumpus(world.getAgentX(), world.getAgentY())) {
                    dualPanel.updateState(world, agent, action);
                    break;
                }
                stepCount++;
            }

            dualPanel.updateState(world, agent, ActionType.CLIMB);
            
            if (world.isPit(world.getAgentX(), world.getAgentY())) {
                JOptionPane.showMessageDialog(guiFrame, " 에이전트 실패: 구덩이(Pit)에 빠져 사망했습니다.", "게임 오버", JOptionPane.ERROR_MESSAGE);
            } else if (world.isWumpus(world.getAgentX(), world.getAgentY())) {
                JOptionPane.showMessageDialog(guiFrame, " 에이전트 실패: 웜퍼스(Wumpus)에게 뜯겨 사망했습니다.", "게임 오버", JOptionPane.ERROR_MESSAGE);
            } else if (agent.hasGold && world.getAgentX() == 1 && world.getAgentY() == 1) {
                JOptionPane.showMessageDialog(guiFrame, "미션 완료! 금을 성공적으로 가지고 복귀 탈출했습니다.", "성공", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(guiFrame, "탐색 완료: 복귀 후 안전하게 클라임(Climb) 종료했습니다.", "결과", JOptionPane.WARNING_MESSAGE);
            }

            guiFrame.dispose();

        } catch (InterruptedException e) {
            System.out.println("시뮬레이터 구동 오류: " + e.getMessage());
        }
    }

    private static void initGUI() {
        guiFrame = new JFrame("Wumpus World Dual-Vision Monitor");
        dualPanel = new DualWorldPanel();
        guiFrame.add(dualPanel);
        guiFrame.setSize(940, 520); 
        guiFrame.setLocationRelativeTo(null);
        guiFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        guiFrame.setVisible(true);
    }

    static class DualWorldPanel extends JPanel {
        private int agentX = 1, agentY = 1;
        private String agentDir = "EAST";
        private boolean hasGold = false;
        
        private boolean[][] agentVisited = new boolean[4][4];
        private boolean[][] agentSafe = new boolean[4][4];
        private World currentWorld;
        private LogicalExplorer currentAgent;

        private boolean drawArrowEffect = false;
        private int shootLogicalStartX, shootLogicalStartY;
        private int shootLogicalEndX, shootLogicalEndY;
        private int deadWumpusX = -1;
        private int deadWumpusY = -1;

        public void triggerShootEffect(int startX, int startY, String direction) {
            this.shootLogicalStartX = startX;
            this.shootLogicalStartY = startY;
            this.shootLogicalEndX = startX;
            this.shootLogicalEndY = startY;

            if (direction.equals("NORTH")) shootLogicalEndY = 4;
            else if (direction.equals("SOUTH")) shootLogicalEndY = 1;
            else if (direction.equals("EAST"))  shootLogicalEndX = 4;
            else if (direction.equals("WEST"))  shootLogicalEndX = 1;

            this.drawArrowEffect = true;
            repaint();

            Timer arrowTimer = new Timer(400, e -> {
                this.drawArrowEffect = false;
                repaint();
            });
            arrowTimer.setRepeats(false);
            arrowTimer.start();
        }

        public void updateState(World world, LogicalExplorer agent, ActionType action) {
            this.currentWorld = world;
            this.currentAgent = agent; 
            this.agentX = world.getAgentX();
            this.agentY = world.getAgentY();
            this.agentDir = world.getAgentDir().toString();
            this.hasGold = agent.hasGold;
            
            if (this.deadWumpusX == -1) {
                for (int x = 1; x <= 4; x++) {
                    for (int y = 1; y <= 4; y++) {
                        if (world.isWumpus(x, y)) {
                            this.deadWumpusX = x;
                            this.deadWumpusY = y;
                        }
                    }
                }
            }
            
            try {
                boolean[][] vMap = agent.getVisitedMap();
                boolean[][] sMap = agent.getSafeMap();
                for (int i = 0; i < 4; i++) {
                    for (int j = 0; j < 4; j++) {
                        this.agentVisited[i][j] = vMap[i][j];
                        this.agentSafe[i][j] = sMap[i][j];
                    }
                }
            } catch (Exception e) {}
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int cellSize = 90;
            int topOffset = 60;

            int leftStart = 40;
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("SansSerif", Font.BOLD, 13));
            g2.drawString("■ 에이전트 추론 지도 (황색:방문 / 녹색:안전확정)", leftStart, 35);

            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    int x = leftStart + i * cellSize;
                    int y = topOffset + j * cellSize;
                    
                    int vX = i; 
                    int vY = 3 - j; 
                    int wX = i + 1;
                    int wY = 4 - j;

                    if (agentVisited[vX][vY]) {
                        g2.setColor(new Color(255, 238, 160)); 
                    } else if (agentSafe[vX][vY]) {
                        g2.setColor(new Color(190, 245, 190)); 
                    } else {
                        g2.setColor(new Color(230, 230, 230)); 
                    }
                    g2.fillRect(x, y, cellSize, cellSize);
                    g2.setColor(Color.LIGHT_GRAY);
                    g2.drawRect(x, y, cellSize, cellSize);
                    
                    // --- [위험도 표시 로직 추가] ---
                    if (currentAgent != null) {
                        int pRisk = currentAgent.getPitRiskMap()[wX][wY];
                        int wRisk = currentAgent.getWumpusRiskMap()[wX][wY];
                        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
                        if (pRisk > 0) { g2.setColor(Color.RED); g2.drawString("P:" + pRisk, x + 5, y + 40); }
                        if (wRisk > 0) { g2.setColor(new Color(128, 0, 128)); g2.drawString("W:" + wRisk, x + 5, y + 55); }
                    }
                    // ---------------------------

                    g2.setColor(Color.GRAY);
                    g2.drawString("(" + wX + "," + wY + ")", x + 6, y + 18);

                    if (wX == agentX && wY == agentY) {
                        drawAgent(g2, x, y, cellSize);
                    }
                }
            }

            int rightStart = 480;
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("SansSerif", Font.BOLD, 13));
            g2.drawString("■ 전지적 참견 시점 (실제 전역 정답 월드)", rightStart, 35);

            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    int x = rightStart + i * cellSize;
                    int y = topOffset + j * cellSize;
                    int wX = i + 1;
                    int wY = 4 - j;

                    g2.setColor(new Color(250, 250, 250));
                    g2.fillRect(x, y, cellSize, cellSize);
                    g2.setColor(Color.LIGHT_GRAY);
                    g2.drawRect(x, y, cellSize, cellSize);
                    
                    g2.setColor(Color.LIGHT_GRAY);
                    g2.drawString("(" + wX + "," + wY + ")", x + 6, y + 18);

                    if (currentWorld != null) {
                        if (wX == deadWumpusX && wY == deadWumpusY) {
                            if (currentWorld.isWumpus(wX, wY)) {
                                g2.setColor(Color.RED);
                                g2.fillOval(x + 55, y + 15, 20, 20);
                                g2.setColor(Color.WHITE);
                                g2.setFont(new Font("SansSerif", Font.BOLD, 12));
                                g2.drawString("W", x + 61, y + 30);
                            } else {
                                g2.setColor(Color.DARK_GRAY);
                                g2.setStroke(new BasicStroke(4));
                                g2.drawLine(x + 20, y + 20, x + cellSize - 20, y + cellSize - 20);
                                g2.drawLine(x + cellSize - 20, y + 20, x + 20, y + cellSize - 20);
                                g2.setFont(new Font("SansSerif", Font.BOLD, 13));
                                g2.drawString("💀 DEAD", x + 18, y + 35);
                            }
                        }
                        if (currentWorld.isPit(wX, wY)) {
                            g2.setColor(Color.BLACK);
                            g2.fillOval(x + 15, y + 55, 20, 20);
                            g2.setColor(Color.WHITE);
                            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
                            g2.drawString("P", x + 21, y + 70);
                        }
                        if (currentWorld.isGold(wX, wY)) {
                            g2.setColor(new Color(230, 170, 0));
                            g2.fillOval(x + 55, y + 55, 22, 22);
                            g2.setColor(Color.BLACK);
                            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
                            g2.drawString("G", x + 62, y + 71);
                        }
                    }

                    if (wX == agentX && wY == agentY) {
                        drawAgent(g2, x, y, cellSize);
                    }
                }
            }

            if (drawArrowEffect) {
                g2.setColor(new Color(255, 69, 0, 240)); 
                g2.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int startIdxX = shootLogicalStartX - 1;
                int startIdxY = 4 - shootLogicalStartY;
                int endIdxX = shootLogicalEndX - 1;
                int endIdxY = 4 - shootLogicalEndY;
                int leftX1 = leftStart + startIdxX * cellSize + cellSize / 2;
                int leftY1 = topOffset + startIdxY * cellSize + cellSize / 2;
                int leftX2 = leftStart + endIdxX * cellSize + cellSize / 2;
                int leftY2 = topOffset + endIdxY * cellSize + cellSize / 2;
                g2.drawLine(leftX1, leftY1, leftX2, leftY2);
                int rightX1 = rightStart + startIdxX * cellSize + cellSize / 2;
                int rightY1 = topOffset + startIdxY * cellSize + cellSize / 2;
                int rightX2 = rightStart + endIdxX * cellSize + cellSize / 2;
                int rightY2 = topOffset + endIdxY * cellSize + cellSize / 2;
                g2.drawLine(rightX1, rightY1, rightX2, rightY2);
                g2.setColor(Color.YELLOW);
                g2.fillOval(leftX2 - 8, leftY2 - 8, 16, 16);
                g2.fillOval(rightX2 - 8, rightY2 - 8, 16, 16);
            }
        }

        private void drawAgent(Graphics2D g2, int x, int y, int cellSize) {
            if (currentWorld != null && (currentWorld.isPit(agentX, agentY) || currentWorld.isWumpus(agentX, agentY))) {
                g2.setColor(Color.RED);
                g2.fillOval(x + 22, y + 22, 46, 46);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 16));
                g2.drawString("💀", x + 35, y + 51);
                return;
            }
            g2.setColor(new Color(50, 120, 240));
            g2.fillOval(x + 22, y + 22, 46, 46);
            int cx = x + 45; int cy = y + 45;
            int[] px = new int[3]; int[] py = new int[3];
            if (agentDir.equals("NORTH")) { px[0] = cx; py[0] = cy - 14; px[1] = cx - 8; py[1] = cy + 8; px[2] = cx + 8; py[2] = cy + 8; }
            else if (agentDir.equals("SOUTH")) { px[0] = cx; py[0] = cy + 14; px[1] = cx - 8; py[1] = cy - 8; px[2] = cx + 8; py[2] = cy - 8; }
            else if (agentDir.equals("WEST")) { px[0] = cx - 14; py[0] = cy; px[1] = cx + 8; py[1] = cy - 8; px[2] = cx + 8; py[2] = cy + 8; }
            else { px[0] = cx + 14; py[0] = cy; px[1] = cx - 8; py[1] = cy - 8; px[2] = cx - 8; py[2] = cy + 8; }
            g2.setColor(Color.YELLOW);
            g2.fillPolygon(px, py, 3);
            if (hasGold) {
                g2.setColor(Color.ORANGE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 10));
                g2.drawString("★GOLD", x + 24, y + 15);
            }
        }
    }

    private static void runMassiveSimulationMode() {
        int totalGames = 100; int successCount = 0; int failureCount = 0;
        String fileName = "simulation_result.txt";
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
            for (int i = 1; i <= totalGames; i++) {
                final int gameNum = i;
                Callable<GameResult> gameTask = () -> {
                    World world = new World();
                    LogicalExplorer agent = new LogicalExplorer(); 
                    Percept percept = world.perceive();
                    int step = 0;
                    while (!world.isDone() && step < 100) {
                        ActionType action = agent.nextAction(percept);
                        percept = world.step(action);
                        if (action == ActionType.GO_FORWARD) agent.applyMove(percept.bump);
                        step++;
                        if (world.isPit(world.getAgentX(), world.getAgentY()) || world.isWumpus(world.getAgentX(), world.getAgentY())) break;
                    }
                    boolean isSuccess = !world.isPit(world.getAgentX(), world.getAgentY()) && 
                                        !world.isWumpus(world.getAgentX(), world.getAgentY()) && 
                                        agent.hasGold && world.getAgentX() == 1 && world.getAgentY() == 1;
                    return new GameResult(isSuccess, step, false);
                };
                GameResult result;
                try {
                    Future<GameResult> future = executor.submit(gameTask);
                    result = future.get(800, TimeUnit.MILLISECONDS); 
                } catch (Exception e) { result = new GameResult(false, 0, true); }
                if (result.isSuccess) successCount++; else failureCount++;
            }
            double successRate = ((double) successCount / totalGames) * 100;
            System.out.printf("성공: %d회 | 실패: %d회 | 성공률: %.1f%%%n", successCount, failureCount, successRate);
        } catch (IOException e) { System.out.println("파일 에러: " + e.getMessage()); } finally { executor.shutdownNow(); }
    }

    private static void runTestExecutionMode(Scanner scanner) {
        System.out.println("\n===== [4] 테스트 실행 모드 (단계별 디버깅) =====");
        try {
            World world = new World();
            LogicalExplorer agent = new LogicalExplorer(); 
            Percept percept = world.perceive();
            initGUI();
            dualPanel.updateState(world, agent, ActionType.CLIMB);
            int stepCount = 1;
            while (!world.isDone() && stepCount <= 100) {
                System.out.printf("\n[Test Turn %d] 엔터 키를 누르면 다음 행동을 수행합니다...", stepCount);
                scanner.nextLine();
                ActionType action = agent.nextAction(percept);
                if (action == ActionType.SHOOT) dualPanel.triggerShootEffect(world.getAgentX(), world.getAgentY(), world.getAgentDir().toString());
                dualPanel.updateState(world, agent, action);
                percept = world.step(action);
                if (action == ActionType.GO_FORWARD) agent.applyMove(percept.bump);
                if (world.isPit(world.getAgentX(), world.getAgentY()) || world.isWumpus(world.getAgentX(), world.getAgentY())) {
                    dualPanel.updateState(world, agent, action);
                    break;
                }
                stepCount++;
            }
            dualPanel.updateState(world, agent, ActionType.CLIMB);
            guiFrame.dispose();
        } catch (Exception e) { System.out.println("오류: " + e.getMessage()); }
    }

    static class GameResult {
        boolean isSuccess; int steps; boolean isTimeout;
        GameResult(boolean isSuccess, int steps, boolean isTimeout) {
            this.isSuccess = isSuccess; this.steps = steps; this.isTimeout = isTimeout;
        }
    }
}