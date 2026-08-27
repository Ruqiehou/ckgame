import { GameDate } from '../core/calendar/GameDate.js';
import { CasusBelli } from '../politics/CasusBelli.js';
import { WarParticipant } from './WarParticipant.js';
import { WarResult } from './WarResult.js';
/**
 * A war object.
 */
export declare class War {
    id: number;
    name: string;
    cb: CasusBelli;
    attackerPrimary: number;
    defenderPrimary: number;
    start: GameDate;
    participants: WarParticipant[];
    warscore: number;
    active: boolean;
    result: WarResult;
    targetTitle: number;
    constructor(id: number, name: string, cb: CasusBelli, attackerPrimary: number, defenderPrimary: number, start: GameDate);
    /** Whether a character is involved in this war. */
    involves(who: number): boolean;
    /** Whether a character is on the attacker side. */
    isAttacker(who: number): boolean;
    /** Apply warscore change, clamped to [-100, 100]. */
    applyWarscore(delta: number): void;
    /** Whether the attacker can enforce their demands. */
    canEnforce(): boolean;
    /** Whether the defender can surrender. */
    canSurrender(): boolean;
    /** Months elapsed since war started. */
    monthsElapsed(now: GameDate): number;
    /**
     * Check if white peace conditions are met.
     * @param now Current date
     * @param atkExh Attacker war exhaustion
     * @param defExh Defender war exhaustion
     */
    canWhitePeace(now: GameDate, atkExh: number, defExh: number): boolean;
}
