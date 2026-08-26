package com.ckgame.events;

import java.util.List;

/**
 * 事件选项。
 */
public final class EventChoice {
    public final int id;
    public final String text;
    public final List<Effect> effects;
    public final double aiWeight;

    public EventChoice(int id, String text, List<Effect> effects, double aiWeight) {
        this.id = id;
        this.text = text;
        this.effects = effects;
        this.aiWeight = aiWeight;
    }

    public EventChoice(int id, String text, List<Effect> effects) {
        this(id, text, effects, 5.0);
    }
}
