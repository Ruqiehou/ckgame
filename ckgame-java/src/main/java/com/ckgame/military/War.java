package com.ckgame.military;

import com.ckgame.core.Constants;
import com.ckgame.core.balance.Balance;
import com.ckgame.core.calendar.GameDate;
import com.ckgame.politics.CasusBelli;

import java.util.ArrayList;
import java.util.List;

/**
 * 战争对象。
 */
public final class War {
    public int id;
    public String name;
    public CasusBelli cb;
    public int attackerPrimary;
    public int defenderPrimary;
    public GameDate start;
    public final List<WarParticipant> participants = new ArrayList<>();
    public int warscore = 0;
    public boolean active = true;
    public WarResult result = WarResult.ONGOING;
    public int targetTitle = Constants.NONE_ID;

    public War(int id, String name, CasusBelli cb,
               int attackerPrimary, int defenderPrimary, GameDate start) {
        this.id = id;
        this.name = name;
        this.cb = cb;
        this.attackerPrimary = attackerPrimary;
        this.defenderPrimary = defenderPrimary;
        this.start = start;
    }

    /** 指定角色是否参与本战争。 */
    public boolean involves(int who) {
        for (WarParticipant p : participants) {
            if (p.character == who) {
                return true;
            }
        }
        return false;
    }

    /** 指定角色是否为攻击方。 */
    public boolean isAttacker(int who) {
        for (WarParticipant p : participants) {
            if (p.character == who && p.isAttacker) {
                return true;
            }
        }
        return false;
    }

    /** 应用战争分数变化，限制在 [-100, 100]。 */
    public void applyWarscore(int delta) {
        warscore = Math.max(-100, Math.min(100, warscore + delta));
    }

    /** 攻击方是否可以强制执行和约。 */
    public boolean canEnforce() {
        return warscore >= cb.warscoreGoal();
    }

    /** 防御方是否可以投降。 */
    public boolean canSurrender() {
        return warscore <= -cb.warscoreGoal();
    }

    /** 战争已进行月数。 */
    public int monthsElapsed(GameDate now) {
        return Math.max(0, (now.toOrdinal() - start.toOrdinal()) / 30);
    }

    /**
     * 是否满足白和条件。
     *
     * @param now     当前日期
     * @param atkExh  攻击方战争疲劳
     * @param defExh  防守方战争疲劳
     */
    public boolean canWhitePeace(GameDate now, double atkExh, double defExh) {
        int months = monthsElapsed(now);
        if (months < Balance.WHITE_PEACE_MIN_MONTHS) {
            return false;
        }
        if (Math.abs(warscore) <= Balance.WHITE_PEACE_STALEMATE_SCORE
                && months >= Balance.WHITE_PEACE_STALEMATE_MONTHS) {
            return true;
        }
        if (Math.abs(warscore) <= Balance.WHITE_PEACE_FATIGUE_SCORE
                && atkExh >= Balance.WHITE_PEACE_FATIGUE_THRESHOLD
                && defExh >= Balance.WHITE_PEACE_FATIGUE_THRESHOLD) {
            return true;
        }
        if (months >= Balance.WHITE_PEACE_MAX_MONTHS
                && Math.abs(warscore) < Balance.WHITE_PEACE_MAX_SCORE) {
            return true;
        }
        return false;
    }
}
