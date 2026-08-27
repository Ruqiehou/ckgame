import { GameDate } from '../core/calendar/GameDate.js';
import { Siege } from './Siege.js';
import { SiegeEvent } from './SiegeEvent.js';
/**
 * Siege manager: tracks all active sieges.
 */
export declare class SiegeManager {
    sieges: Map<number, Siege>;
    nextId: number;
    /**
     * Start a siege on a county. If the county already has an active siege, return its id.
     */
    start(county: number, attackerArmy: number, attacker: number, defender: number, fortLevel: number, garrison: number, date: GameDate): number;
    /** Find the active siege at a county. */
    activeAt(county: number): Siege | undefined;
    /** All active sieges. */
    activeSieges(): Siege[];
    /**
     * Daily tick for all sieges.
     * @param armyMen Map from army id to current men count
     * @param armyMartial Map from army id to commander martial
     * @param armyLocation Map from army id to current location
     */
    tickDay(armyMen: Map<number, number>, armyMartial: Map<number, number>, armyLocation: Map<number, number>): SiegeEvent[];
}
