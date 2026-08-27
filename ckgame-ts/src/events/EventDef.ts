import { EventChoice } from './EventChoice.js';

/**
 * Event definition.
 */
export class EventDef {
  public readonly id: number;
  public readonly title: string;
  public readonly description: string;
  public readonly weight: number;
  public readonly cooldownDays: number;
  public readonly major: boolean;
  public readonly choices: EventChoice[];
  public readonly requiresRuler: boolean;
  public readonly requiresAdult: boolean;
  public readonly minGold: number;
  public readonly requiresMarried: boolean;

  constructor(
    id: number,
    title: string,
    description: string,
    weight: number,
    cooldownDays: number,
    major: boolean,
    choices: EventChoice[],
    requiresRuler: boolean = true,
    requiresAdult: boolean = true,
    minGold: number = 0,
    requiresMarried: boolean = false,
  ) {
    this.id = id;
    this.title = title;
    this.description = description;
    this.weight = weight;
    this.cooldownDays = cooldownDays;
    this.major = major;
    this.choices = choices;
    this.requiresRuler = requiresRuler;
    this.requiresAdult = requiresAdult;
    this.minGold = minGold;
    this.requiresMarried = requiresMarried;
  }
}
