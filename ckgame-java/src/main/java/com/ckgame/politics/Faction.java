package com.ckgame.politics;

import com.ckgame.core.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * 派系对象。
 */
public final class Faction {
    public final int id;
    public final FactionKind kind;
    public final int targetLiege;
    public final List<Integer> members = new ArrayList<>();
    public double power = 0.0;
    public double discontent = 0.0;
    public boolean ultimatumSent = false;
    public int claimant = Constants.NONE_ID;

    public Faction(int id, FactionKind kind, int targetLiege, Integer claimant) {
        this.id = id;
        this.kind = kind;
        this.targetLiege = targetLiege;
        if (claimant != null) {
            this.claimant = claimant;
        }
    }

    /** 添加成员（去重）。 */
    public void addMember(int who) {
        if (!members.contains(who)) {
            members.add(who);
        }
    }

    /** 移除成员。 */
    public void removeMember(int who) {
        members.remove(Integer.valueOf(who));
    }

    /** 是否已准备好叛乱。 */
    public boolean isReadyToRevolt() {
        return power >= 80 && discontent >= 50 && members.size() >= 2;
    }

    /** 是否可发出最后通牒。 */
    public boolean canSendUltimatum() {
        return power >= 60
                && discontent >= 40
                && !ultimatumSent
                && members.size() >= 2;
    }
}
