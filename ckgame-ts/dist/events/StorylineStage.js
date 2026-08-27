/**
 * A storyline stage.
 */
export class StorylineStage {
    stageId;
    title;
    description;
    eventIds;
    nextStage;
    requiresCondition;
    constructor(stageId, title, description, eventIds, nextStage, requiresCondition) {
        this.stageId = stageId;
        this.title = title;
        this.description = description;
        this.eventIds = eventIds;
        this.nextStage = nextStage ?? null;
        this.requiresCondition = requiresCondition ?? null;
    }
}
//# sourceMappingURL=StorylineStage.js.map