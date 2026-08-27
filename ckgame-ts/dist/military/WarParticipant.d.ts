import { GameDate } from '../core/calendar/GameDate.js';
/**
 * A participant in a war.
 */
export declare class WarParticipant {
    character: number;
    isAttacker: boolean;
    joined: GameDate;
    contribution: number;
    constructor(character: number, isAttacker: boolean, joined: GameDate);
}
