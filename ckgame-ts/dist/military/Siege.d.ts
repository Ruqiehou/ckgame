import { GameDate } from '../core/calendar/GameDate.js';
/**
 * A siege on a county.
 */
export declare class Siege {
    id: number;
    county: number;
    attackerArmy: number;
    attacker: number;
    defender: number;
    progress: number;
    fortLevel: number;
    garrison: number;
    started: GameDate | null;
    active: boolean;
    constructor(id: number, county: number, attackerArmy: number, attacker: number, defender: number);
    /** Required total siege progress. */
    requiredProgress(): number;
    /** Whether the siege is complete. */
    isComplete(): boolean;
    /** Daily tick: advance siege progress. Returns daily gain. */
    dailyTick(besiegerMen: number, martial: number): number;
}
