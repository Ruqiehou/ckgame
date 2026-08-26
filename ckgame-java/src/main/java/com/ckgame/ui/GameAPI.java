package com.ckgame.ui;

import com.ckgame.game.GameSimulation;
import com.ckgame.world.World;

import java.util.Scanner;

/**
 * GameAPI - 游戏接口层（TUI 版）。
 * 提供模拟控制接口，供 TUI 调用。
 */
public final class GameAPI {
    private final GameSimulation sim;
    private final Scanner scanner = new Scanner(System.in);

    public GameAPI() {
        this.sim = new GameSimulation();
    }

    /** 运行模拟直到用户退出。 */
    public void run() {
        while (true) {
            printStatus();
            System.out.print("输入命令 (help, run 10, quit): ");
            if (!scanner.hasNextLine()) {
                break;
            }
            String cmd = scanner.nextLine().trim().toLowerCase();
            if (cmd.equals("quit") || cmd.equals("exit")) {
                break;
            } else if (cmd.equals("help")) {
                printHelp();
            } else if (cmd.startsWith("run ")) {
                try {
                    int days = Integer.parseInt(cmd.substring(4));
                    sim.runDays(days);
                    System.out.println("已推进 " + days + " 天。");
                } catch (NumberFormatException e) {
                    System.out.println("请输入有效数字。");
                }
            }
        }
        scanner.close();
    }

    private void printStatus() {
        World w = sim.world;
        System.out.println("=== 1066 模拟器 ===");
        System.out.println("日期: " + w.date);
        System.out.println("存活角色: " + w.aliveCharacters().size());
        System.out.println("进行中战争: " + sim.wars.activeWars().size());
        System.out.println("进行中围城: " + sim.sieges.activeSieges().size());
        System.out.println("活跃派系: " + sim.factions.factions().size());
    }

    private void printHelp() {
        System.out.println("可用命令:");
        System.out.println("  help    - 显示帮助");
        System.out.println("  run N   - 推进 N 天");
        System.out.println("  quit    - 退出");
    }
}
