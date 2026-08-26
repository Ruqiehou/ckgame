package com.ckgame.ui;



/**
 * TUI 主界面。
 * 直接调用 GameAPI 进行交互。
 */
public final class Main {
    public static void main(String[] args) {
        System.out.println("=== 十字军之王 Java 版 TUI ===");
        System.out.println("1066 年 Norman Conquest 模拟器");
        new GameAPI().run();
    }
}
