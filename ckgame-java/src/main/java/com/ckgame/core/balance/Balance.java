package com.ckgame.core.balance;

/**
 * 集中游戏平衡常量，避免散落 magic number。
 */
public final class Balance {
    private Balance() {}

    // 战争疲劳
    public static final double WAR_EXHAUSTION_ON_DECLARE = 15.0;
    public static final double WAR_EXHAUSTION_MONTHLY_ATK = 1.0;
    public static final double WAR_EXHAUSTION_MONTHLY_DEF = 0.8;
    public static final double WAR_EXHAUSTION_DECAY = 1.5;
    public static final double WAR_EXHAUSTION_DECLARE_BLOCK = 60.0;

    // 白和
    public static final int WHITE_PEACE_MIN_MONTHS = 12;
    public static final int WHITE_PEACE_STALEMATE_MONTHS = 18;
    public static final int WHITE_PEACE_STALEMATE_SCORE = 15;
    public static final int WHITE_PEACE_FATIGUE_SCORE = 25;
    public static final double WHITE_PEACE_FATIGUE_THRESHOLD = 40.0;
    public static final int WHITE_PEACE_MAX_MONTHS = 36;
    public static final int WHITE_PEACE_MAX_SCORE = 50;
    public static final int WHITE_PEACE_TRUCE_YEARS = 3;
    public static final int VICTORY_TRUCE_YEARS = 5;

    // 军队
    public static final double ARMY_MAINTENANCE_MULT = 1.25;
    public static final double SUPPLY_RECOVER_FRIENDLY = 2.5;
    public static final double SUPPLY_DRAIN_ENEMY = 0.7;
    public static final double SUPPLY_DRAIN_WINTER = 1.2;
    public static final double SUPPLY_LOW_THRESHOLD = 25.0;
    public static final double SUPPLY_MOVE_SLOW_THRESHOLD = 30.0;
    public static final double MORALE_RECOVER_FRIENDLY = 0.3;
    public static final double MORALE_DRAIN_LOW_SUPPLY = 0.8;

    // 季节移动倍率
    public static final double MOVE_CHANCE_WINTER = 0.55;
    public static final double MOVE_CHANCE_AUTUMN = 0.85;
    public static final double MOVE_CHANCE_LOW_SUPPLY = 0.7;
    public static final double MOVE_CHANCE_MIN = 0.2;

    // 季节战斗倍率
    public static final double SEASON_COMBAT_SPRING = 1.0;
    public static final double SEASON_COMBAT_SUMMER = 1.05;
    public static final double SEASON_COMBAT_AUTUMN = 0.92;
    public static final double SEASON_COMBAT_WINTER = 0.8;

    // 派系
    public static final double FACTION_APPEASE_DEFAULT = 25.0;
    public static final double FACTION_APPEASE_PLAYER = 30.0;
    public static final double FACTION_APPEASE_GOLD = 25.0;
    public static final double FACTION_FEAST_APPEASE = 10.0;
    public static final double FACTION_DISSOLVE_DISCONTENT = 5.0;
    public static final double FACTION_DISSOLVE_POWER = 40.0;

    // 外交玩家操作
    public static final int IMPROVE_RELATIONS_GOLD = 10;
    public static final int FEAST_GOLD = 20;
    public static final int FEAST_PRESTIGE = 15;
    public static final int FEAST_STRESS = -10;
}
