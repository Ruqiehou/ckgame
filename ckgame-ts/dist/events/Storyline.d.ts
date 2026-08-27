import { StorylineStatus } from './StorylineStatus.js';
import { StorylineStage } from './StorylineStage.js';
/**
 * A storyline (long-term narrative arc).
 */
export declare class Storyline {
    readonly id: number;
    readonly title: string;
    readonly description: string;
    readonly characterId: number;
    status: StorylineStatus;
    currentStage: number;
    stages: StorylineStage[];
    startYear: number;
    endYear: number;
    tags: string[];
    constructor(id: number, title: string, description: string, characterId: number);
    isActive(): boolean;
    isCompleted(): boolean;
    isFailed(): boolean;
}
