package com.ckgame.ui;

import com.ckgame.game.ScenarioLoader;

import java.util.List;
import java.util.Scanner;

/**
 * TUI 主界面。
 * 启动 GameTUI 进行交互。
 */
public final class Main {
    public static void main(String[] args) {
        System.out.println("=== 十字军之王 Java 版 ===");
        System.out.println("1066 年诺曼征服模拟器");

        String scenario = args.length > 0 ? args[0] : null;
        if (scenario == null) {
            List<ScenarioLoader.ScenarioInfo> scenarios = ScenarioLoader.listScenarios();
            if (!scenarios.isEmpty()) {
                System.out.println("可选场景：");
                for (int i = 0; i < scenarios.size(); i++) {
                    ScenarioLoader.ScenarioInfo s = scenarios.get(i);
                    String desc = s.description.isEmpty() ? "" : " — " + s.description;
                    System.out.println("  " + (i + 1) + ". " + s.name + " (" + s.id + ")" + desc);
                }
                System.out.print("选择场景 [回车默认 " + ScenarioLoader.DEFAULT_SCENARIO + "] > ");
                Scanner sc = new Scanner(System.in);
                String raw = sc.nextLine().trim();
                if (!raw.isEmpty()) {
                    scenario = raw;
                    try {
                        int idx = Integer.parseInt(raw);
                        if (idx >= 1 && idx <= scenarios.size()) {
                            scenario = scenarios.get(idx - 1).id;
                        }
                    } catch (NumberFormatException ignored) {
                        // 视为场景 id
                    }
                }
            }
        }
        new GameTUI(scenario).run();
    }
}
