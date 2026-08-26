package com.ckgame.politics;

import com.ckgame.core.calendar.GameDate;

/**
 * 两国/统治者之间的条约。
 */
public final class Treaty {
    public final int a;
    public final int b;
    public final TreatyKind kind;
    public final GameDate start;
    public final int expiresYear;

    public Treaty(int a, int b, TreatyKind kind, GameDate start, int expiresYear) {
        this.a = a;
        this.b = b;
        this.kind = kind;
        this.start = start;
        this.expiresYear = expiresYear;
    }
}
