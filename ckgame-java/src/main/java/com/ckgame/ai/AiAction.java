package com.ckgame.ai;

import com.ckgame.politics.CasusBelli;
import com.ckgame.politics.SchemeKind;

/**
 * AI 待执行动作。
 */
public final class AiAction {
    public final String kind;
    public final int owner;
    public final int target;
    public final int location;
    public final int armyId;
    public final int destination;
    public final int levies;
    public final double amount;
    public final CasusBelli cb;
    public final SchemeKind schemeKind;

    public AiAction(String kind) {
        this(kind, 0, 0, 0, 0, 0, 0, 0.0, null, null);
    }

    public AiAction(String kind, int owner) {
        this(kind, owner, 0, 0, 0, 0, 0, 0.0, null, null);
    }

    public AiAction(String kind, int owner, int target) {
        this(kind, owner, target, 0, 0, 0, 0, 0.0, null, null);
    }

    public AiAction(String kind, int owner, int target, CasusBelli cb) {
        this(kind, owner, target, 0, 0, 0, 0, 0.0, cb, null);
    }

    public AiAction(String kind, int owner, int target, SchemeKind schemeKind) {
        this(kind, owner, target, 0, 0, 0, 0, 0.0, null, schemeKind);
    }

    public AiAction(String kind, int owner, int location, int levies) {
        this(kind, owner, 0, location, 0, 0, levies, 0.0, null, null);
    }

    public AiAction(String kind, int owner, int target, double amount) {
        this(kind, owner, target, 0, 0, 0, 0, amount, null, null);
    }

    public AiAction(String kind, int owner, int target, int location,
                    int armyId, int destination, int levies, double amount,
                    CasusBelli cb, SchemeKind schemeKind) {
        this.kind = kind;
        this.owner = owner;
        this.target = target;
        this.location = location;
        this.armyId = armyId;
        this.destination = destination;
        this.levies = levies;
        this.amount = amount;
        this.cb = cb;
        this.schemeKind = schemeKind;
    }
}
