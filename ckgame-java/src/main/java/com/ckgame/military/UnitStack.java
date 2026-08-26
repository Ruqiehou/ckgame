package com.ckgame.military;

/**
 * 兵力堆叠：同一单位类型的人数与最大人数。
 */
public final class UnitStack {
    public UnitType unitType;
    public int men;
    public int maxMen;

    public UnitStack(UnitType unitType, int men, int maxMen) {
        this.unitType = unitType;
        this.men = men;
        this.maxMen = maxMen;
    }

    /** 当前兵力占最大兵力的比例。 */
    public double strengthRatio() {
        return maxMen == 0 ? 0.0 : (double) men / maxMen;
    }

    /** 考虑兵力比例后的战斗力。 */
    public double combatPower() {
        return men * unitType.damage() * strengthRatio();
    }

    /** 承受伤亡，返回实际死亡人数。 */
    public int takeCasualties(int amount) {
        int killed = Math.min(men, amount);
        men -= killed;
        return killed;
    }
}
