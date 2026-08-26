package com.ckgame.world.buildings;

/** 建筑类型与数值。 */
public enum BuildingKind {
    FARM("农田", "增加省份税收收入", 5, 50, 5.0),
    BARRACKS("兵营", "增加征兵上限和训练速度", 3, 100, 20.0),
    WALLS("城墙", "增加守军数量和防御加成", 5, 80, 15.0),
    MARKET("市场", "增加贸易和税收收入", 3, 120, 8.0),
    CHURCH("教堂", "增加稳定度和民众忠诚", 3, 100, 1.0),
    WATCH_TOWER("瞭望塔", "增加视野范围，提前预警", 2, 60, 2.0),
    STABLE("马厩", "提升骑兵战斗效率", 2, 150, 0.1),
    WORKSHOP("工坊", "提升装备生产效率", 2, 100, 0.05);

    private final String nameZh;
    private final String description;
    private final int maxLevel;
    private final int baseCost;
    private final double effectPerLevel;

    BuildingKind(String nameZh, String description, int maxLevel, int baseCost, double effectPerLevel) {
        this.nameZh = nameZh;
        this.description = description;
        this.maxLevel = maxLevel;
        this.baseCost = baseCost;
        this.effectPerLevel = effectPerLevel;
    }

    public String nameZh() { return nameZh; }
    public String description() { return description; }
    public int maxLevel() { return maxLevel; }

    /** 升级到下一级所需的金币。 */
    public int upgradeCost(int level) {
        return (int) (baseCost * Math.pow(1.5, level));
    }

    /** 每级提供的效果值。 */
    public double effectValue(int level) {
        return effectPerLevel * level;
    }
}
