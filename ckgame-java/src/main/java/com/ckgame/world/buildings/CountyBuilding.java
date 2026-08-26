package com.ckgame.world.buildings;

/** 省份中的一座建筑及其等级。 */
public final class CountyBuilding {
    public BuildingKind kind;
    public int level;

    public CountyBuilding(BuildingKind kind, int level) {
        this.kind = kind;
        this.level = level;
    }

    public CountyBuilding(BuildingKind kind) {
        this(kind, 0);
    }

    public boolean canUpgrade() {
        return level < kind.maxLevel();
    }

    public int upgradeCost() {
        return kind.upgradeCost(level);
    }
}
