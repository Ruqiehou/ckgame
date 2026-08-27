import { Effect } from './Effect.js';

/**
 * An event choice.
 */
export class EventChoice {
  public readonly id: number;
  public readonly text: string;
  public readonly effects: Effect[];
  public readonly aiWeight: number;

  constructor(id: number, text: string, effects: Effect[], aiWeight: number = 5) {
    this.id = id;
    this.text = text;
    this.effects = effects;
    this.aiWeight = aiWeight;
  }
}
