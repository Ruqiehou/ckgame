package com.ckgame.politics;

/**
 * 阴谋月度结算结果。
 */
public final class SchemeOutcome {
    public String kind; // success / exposed / progressed
    public int schemeId;
    public SchemeKind schemeKind = null;
    public int owner = 0;
    public int target = 0;
    public double progress = 0.0;

    public SchemeOutcome(String kind, int schemeId) {
        this.kind = kind;
        this.schemeId = schemeId;
    }
}
