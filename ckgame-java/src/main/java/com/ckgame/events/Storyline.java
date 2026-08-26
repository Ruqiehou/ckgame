package com.ckgame.events;

import java.util.ArrayList;
import java.util.List;

/**
 * 剧情线。
 */
public final class Storyline {
    public final int id;
    public final String title;
    public final String description;
    public final int characterId;
    public StorylineStatus status = StorylineStatus.AVAILABLE;
    public int currentStage = 0;
    public final List<StorylineStage> stages = new ArrayList<>();
    public int startYear = 0;
    public int endYear = 0;
    public final List<String> tags = new ArrayList<>();

    public Storyline(int id, String title, String description, int characterId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.characterId = characterId;
    }

    public boolean isActive() {
        return status == StorylineStatus.ACTIVE;
    }

    public boolean isCompleted() {
        return status == StorylineStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == StorylineStatus.FAILED;
    }
}
