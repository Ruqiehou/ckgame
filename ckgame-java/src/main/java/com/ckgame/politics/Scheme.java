package com.ckgame.politics;

import com.ckgame.core.calendar.GameDate;

import java.util.ArrayList;
import java.util.List;

/**
 * 阴谋对象。
 */
public final class Scheme {
    public final int id;
    public final SchemeKind kind;
    public final int owner;
    public final int target;
    public double progress = 0.0;
    public double secrecy = 50.0;
    public final List<Integer> agents = new ArrayList<>();
    public GameDate started = null;
    public boolean exposed = false;

    public Scheme(int id, SchemeKind kind, int owner, int target) {
        this.id = id;
        this.kind = kind;
        this.owner = owner;
        this.target = target;
    }

    /** 阴谋是否已完成。 */
    public boolean isComplete() {
        return progress >= 100.0;
    }
}
