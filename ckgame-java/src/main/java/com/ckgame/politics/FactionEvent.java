package com.ckgame.politics;

import com.ckgame.core.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * 派系相关事件。
 */
public final class FactionEvent {
    public String kind;
    public int factionId;
    public int liege = 0;
    public int founder = 0;
    public int who = 0;
    public FactionKind factionKind = null;
    public final List<Integer> members = new ArrayList<>();
    public String reason = "";

    public FactionEvent(String kind, int factionId) {
        this.kind = kind;
        this.factionId = factionId;
    }

    public FactionEvent(String kind, int factionId, int liege, int founder, int who,
                        FactionKind factionKind, List<Integer> members, String reason) {
        this.kind = kind;
        this.factionId = factionId;
        this.liege = liege;
        this.founder = founder;
        this.who = who;
        this.factionKind = factionKind;
        if (members != null) {
            this.members.addAll(members);
        }
        this.reason = reason != null ? reason : "";
    }
}
