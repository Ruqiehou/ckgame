package com.ckgame.military;

/**
 * 单位类型。
 */
public enum UnitType {
    LEVIES,
    LIGHT_INFANTRY,
    HEAVY_INFANTRY,
    PIKEMEN,
    ARCHERS,
    LIGHT_CAVALRY,
    HEAVY_CAVALRY,
    KNIGHTS;

    /** 伤害（攻击力）。 */
    public double damage() {
        return switch (this) {
            case LEVIES -> 5.0;
            case LIGHT_INFANTRY -> 8.0;
            case HEAVY_INFANTRY -> 14.0;
            case PIKEMEN -> 12.0;
            case ARCHERS -> 10.0;
            case LIGHT_CAVALRY -> 12.0;
            case HEAVY_CAVALRY -> 20.0;
            case KNIGHTS -> 28.0;
        };
    }

    /** 坚韧（防御/生命值）。 */
    public double toughness() {
        return switch (this) {
            case LEVIES -> 4.0;
            case LIGHT_INFANTRY -> 6.0;
            case HEAVY_INFANTRY -> 12.0;
            case PIKEMEN -> 14.0;
            case ARCHERS -> 5.0;
            case LIGHT_CAVALRY -> 8.0;
            case HEAVY_CAVALRY -> 14.0;
            case KNIGHTS -> 18.0;
        };
    }

    /** 每人每月维护费。 */
    public double maintenance() {
        return switch (this) {
            case LEVIES -> 0.05;
            case LIGHT_INFANTRY -> 0.1;
            case HEAVY_INFANTRY -> 0.25;
            case PIKEMEN -> 0.2;
            case ARCHERS -> 0.15;
            case LIGHT_CAVALRY -> 0.3;
            case HEAVY_CAVALRY -> 0.5;
            case KNIGHTS -> 0.8;
        };
    }
}
