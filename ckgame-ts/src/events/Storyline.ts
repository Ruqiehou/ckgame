import { StorylineStatus } from './StorylineStatus.js';
import { StorylineStage } from './StorylineStage.js';

/**
 * A storyline (long-term narrative arc).
 */
export class Storyline {
  public readonly id: number;
  public readonly title: string;
  public readonly description: string;
  public readonly characterId: number;
  public status: StorylineStatus = StorylineStatus.AVAILABLE;
  public currentStage: number = 0;
  public stages: StorylineStage[] = [];
  public startYear: number = 0;
  public endYear: number = 0;
  public tags: string[] = [];

  constructor(id: number, title: string, description: string, characterId: number) {
    this.id = id;
    this.title = title;
    this.description = description;
    this.characterId = characterId;
  }

  isActive(): boolean {
    return this.status === StorylineStatus.ACTIVE;
  }

  isCompleted(): boolean {
    return this.status === StorylineStatus.COMPLETED;
  }

  isFailed(): boolean {
    return this.status === StorylineStatus.FAILED;
  }
}
