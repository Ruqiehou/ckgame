package com.ckgame.military;

/**
 * 战斗结果。
 */
public final class BattleResult {
    public final boolean attackerWon;
    public final int attackerLosses;
    public final int defenderLosses;
    public final int warscoreChange;
    public final String description;

    public BattleResult(boolean attackerWon, int attackerLosses, int defenderLosses,
                        int warscoreChange, String description) {
        this.attackerWon = attackerWon;
        this.attackerLosses = attackerLosses;
        this.defenderLosses = defenderLosses;
        this.warscoreChange = warscoreChange;
        this.description = description;
    }
}
