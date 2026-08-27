/**
 * A storyline stage.
 */
export declare class StorylineStage {
    readonly stageId: number;
    readonly title: string;
    readonly description: string;
    readonly eventIds: number[];
    readonly nextStage: number | null;
    readonly requiresCondition: string | null;
    constructor(stageId: number, title: string, description: string, eventIds: number[], nextStage?: number | null, requiresCondition?: string | null);
}
