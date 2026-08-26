package com.ckgame.events;

import java.util.List;

/**
 * 剧情线阶段。
 */
public final class StorylineStage {
    public final int stageId;
    public final String title;
    public final String description;
    public final List<Integer> eventIds;
    public final Integer nextStage;
    public final String requiresCondition;

    public StorylineStage(int stageId, String title, String description,
                          List<Integer> eventIds, Integer nextStage, String requiresCondition) {
        this.stageId = stageId;
        this.title = title;
        this.description = description;
        this.eventIds = eventIds;
        this.nextStage = nextStage;
        this.requiresCondition = requiresCondition;
    }

    public StorylineStage(int stageId, String title, String description, List<Integer> eventIds) {
        this(stageId, title, description, eventIds, null, null);
    }
}
