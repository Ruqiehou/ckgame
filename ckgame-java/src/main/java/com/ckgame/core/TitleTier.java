package com.ckgame.core;

public enum TitleTier {
    BARONY(1, "男爵"),
    COUNTY(2, "伯爵"),
    DUCHY(3, "公爵"),
    KINGDOM(4, "国王"),
    EMPIRE(5, "皇帝");

    private final int value;
    private final String rankName;

    TitleTier(int value, String rankName) {
        this.value = value;
        this.rankName = rankName;
    }

    public int value() { return value; }

    public String rankName() { return rankName; }

    public double creationCost() {
        return switch (this) {
            case BARONY -> 0.0;
            case COUNTY -> 50.0;
            case DUCHY -> 200.0;
            case KINGDOM -> 500.0;
            case EMPIRE -> 1000.0;
        };
    }

    public boolean isDestroyable() {
        return this.compareTo(DUCHY) >= 0;
    }
}
