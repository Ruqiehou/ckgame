package com.ckgame.events;

import java.util.List;

/**
 * 待处理事件实例。
 */
public final class EventInstance {
    public final int eventId;
    public final int character;
    public final String title;
    public final String description;
    public final List<EventChoice> choices;

    public EventInstance(int eventId, int character, String title,
                         String description, List<EventChoice> choices) {
        this.eventId = eventId;
        this.character = character;
        this.title = title;
        this.description = description;
        this.choices = choices;
    }
}
