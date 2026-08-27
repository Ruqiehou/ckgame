import { EventChoice } from './EventChoice.js';

/**
 * A pending event instance for a specific character.
 */
export class EventInstance {
  public readonly eventId: number;
  public readonly character: number;
  public readonly title: string;
  public readonly description: string;
  public readonly choices: EventChoice[];

  constructor(
    eventId: number,
    character: number,
    title: string,
    description: string,
    choices: EventChoice[],
  ) {
    this.eventId = eventId;
    this.character = character;
    this.title = title;
    this.description = description;
    this.choices = choices;
  }
}
