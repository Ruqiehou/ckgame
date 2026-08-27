import { GameDate } from '../core/calendar/GameDate.js';

/**
 * A participant in a war.
 */
export class WarParticipant {
  public character: number;
  public isAttacker: boolean;
  public joined: GameDate;
  public contribution: number = 0;

  constructor(character: number, isAttacker: boolean, joined: GameDate) {
    this.character = character;
    this.isAttacker = isAttacker;
    this.joined = joined;
  }
}
