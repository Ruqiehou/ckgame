package com.ckgame.events;

import java.util.List;

/**
 * 事件定义。
 */
public final class EventDef {
    public final int id;
    public final String title;
    public final String description;
    public final double weight;
    public final int cooldownDays;
    public final boolean major;
    public final List<EventChoice> choices;
    public final boolean requiresRuler;
    public final boolean requiresAdult;
    public final double minGold;
    public final boolean requiresMarried;

    public EventDef(int id, String title, String description,
                    double weight, int cooldownDays, boolean major,
                    List<EventChoice> choices) {
        this(id, title, description, weight, cooldownDays, major, choices,
                true, true, 0.0, false);
    }

    public EventDef(int id, String title, String description,
                    double weight, int cooldownDays, boolean major,
                    List<EventChoice> choices,
                    boolean requiresRuler, boolean requiresAdult,
                    double minGold, boolean requiresMarried) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.weight = weight;
        this.cooldownDays = cooldownDays;
        this.major = major;
        this.choices = choices;
        this.requiresRuler = requiresRuler;
        this.requiresAdult = requiresAdult;
        this.minGold = minGold;
        this.requiresMarried = requiresMarried;
    }
}
