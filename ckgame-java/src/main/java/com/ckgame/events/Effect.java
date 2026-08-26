package com.ckgame.events;

import com.ckgame.world.Character;
import com.ckgame.world.World;

/**
 * 事件效果：gold/prestige/piety/stress/health/trait/log。
 */
public final class Effect {
    public final String kind;
    public final double amount;
    public final String text;

    public Effect(String kind) {
        this(kind, 0.0, "");
    }

    public Effect(String kind, double amount) {
        this(kind, amount, "");
    }

    public Effect(String kind, double amount, String text) {
        this.kind = kind;
        this.amount = amount;
        this.text = text;
    }

    public void apply(World world, int who) {
        Character c = world.character(who);
        if (c == null) {
            return;
        }
        switch (kind) {
            case "gold" -> c.addGold(amount);
            case "prestige" -> c.addPrestige(amount);
            case "piety" -> c.piety = Math.max(0.0, c.piety + amount);
            case "stress" -> c.addStress((int) amount);
            case "health" -> c.health += amount;
            case "trait" -> {
                int tid = (int) amount;
                if (!c.traits.contains(tid)) {
                    c.traits.add(tid);
                }
            }
            case "log" -> {
                if (text != null && !text.isEmpty()) {
                    world.pushLog(text);
                }
            }
            default -> {
                // 未知效果类型，保持静默
            }
        }
    }
}
