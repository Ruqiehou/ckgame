/**
 * A storyline stage.
 */
export class StorylineStage {
  public readonly stageId: number;
  public readonly title: string;
  public readonly description: string;
  public readonly eventIds: number[];
  public readonly nextStage: number | null;
  public readonly requiresCondition: string | null;

  constructor(
    stageId: number,
    title: string,
    description: string,
    eventIds: number[],
    nextStage?: number | null,
    requiresCondition?: string | null,
  ) {
    this.stageId = stageId;
    this.title = title;
    this.description = description;
    this.eventIds = eventIds;
    this.nextStage = nextStage ?? null;
    this.requiresCondition = requiresCondition ?? null;
  }
}
