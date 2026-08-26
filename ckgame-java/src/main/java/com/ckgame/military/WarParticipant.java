package com.ckgame.military;

import com.ckgame.core.calendar.GameDate;

/**
 * 战争参与者。
 */
public final class WarParticipant {
    public int character;
    public boolean isAttacker;
    public GameDate joined;
    public double contribution = 0.0;

    public WarParticipant(int character, boolean isAttacker, GameDate joined) {
        this.character = character;
        this.isAttacker = isAttacker;
        this.joined = joined;
    }
}
