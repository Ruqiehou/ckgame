package com.ckgame.world;

/** 地形，影响补给、战斗宽度与开发上限。 */
public enum Terrain {
    PLAINS(1.0, 1.0, 80),
    HILLS(0.8, 0.8, 60),
    MOUNTAINS(0.5, 0.6, 40),
    FOREST(0.7, 0.8, 60),
    DESERT(0.4, 0.9, 40),
    WETLAND(0.6, 0.6, 50),
    FARMLAND(1.3, 1.0, 100),
    COASTAL(1.1, 0.95, 80);

    private final double supplyLimit;
    private final double combatWidth;
    private final int developmentCap;

    Terrain(double supplyLimit, double combatWidth, int developmentCap) {
        this.supplyLimit = supplyLimit;
        this.combatWidth = combatWidth;
        this.developmentCap = developmentCap;
    }

    public double supplyLimit() { return supplyLimit; }
    public double combatWidth() { return combatWidth; }
    public int developmentCap() { return developmentCap; }
}
