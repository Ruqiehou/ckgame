package com.ckgame.ui;

/**
 * TUI 主界面。
 * 启动 GameTUI 进行交互。
 */
public final class Main {
    public static void main(String[] args) {
        System.out.println("=== 十字军之王 Java 版 ===");
        System.out.println("1066 年诺曼征服模拟器");
        new GameTUI().run();
    }
}
