package com.ckgame.military;

import com.ckgame.core.calendar.GameDate;

/**
 * 围城对象。
 */
public final class Siege {
    public int id;
    public int county;
    public int attackerArmy;
    public int attacker;
    public int defender;
    public double progress = 0.0;
    public int fortLevel = 1;
    public int garrison = 50;
    public GameDate started;
    public boolean active = true;

    public Siege(int id, int county, int attackerArmy, int attacker, int defender) {
        this.id = id;
        this.county = county;
        this.attackerArmy = attackerArmy;
        this.attacker = attacker;
        this.defender = defender;
    }

    /** 需要的总围城进度。 */
    public double requiredProgress() {
        return 100.0 + fortLevel * 40.0;
    }

    /** 是否已完成围城。 */
    public boolean isComplete() {
        return progress >= requiredProgress();
    }

    /** 每日推进，返回本日进度增量。 */
    public double dailyTick(int besiegerMen, int martial) {
        if (!active) {
            return 0.0;
        }
        double menFactor = Math.max(0.2, Math.min(3.0, (double) besiegerMen / Math.max(50, garrison)));
        double fortPenalty = 1.0 / (1.0 + fortLevel * 0.25);
        double martialBonus = 1.0 + (martial - 8) * 0.03;
        double gain = 1.2 * menFactor * fortPenalty * martialBonus;
        progress = Math.min(requiredProgress(), progress + gain);
        return gain;
    }
}
