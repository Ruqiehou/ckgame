import { StorylineStatus } from './StorylineStatus.js';
/**
 * A storyline (long-term narrative arc).
 */
export class Storyline {
    id;
    title;
    description;
    characterId;
    status = StorylineStatus.AVAILABLE;
    currentStage = 0;
    stages = [];
    startYear = 0;
    endYear = 0;
    tags = [];
    constructor(id, title, description, characterId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.characterId = characterId;
    }
    isActive() {
        return this.status === StorylineStatus.ACTIVE;
    }
    isCompleted() {
        return this.status === StorylineStatus.COMPLETED;
    }
    isFailed() {
        return this.status === StorylineStatus.FAILED;
    }
}
//# sourceMappingURL=Storyline.js.map