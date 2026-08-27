import { GameDate } from '../core/calendar/GameDate.js';
import type { World } from '../world/World.js';
import { Scheme } from './Scheme.js';
import { SchemeKind } from './SchemeKind.js';
import { SchemeOutcome } from './SchemeOutcome.js';
/**
 * Scheme manager: tracks and ticks all schemes.
 */
export declare class SchemeManager {
    private world;
    private schemes;
    private nextId;
    constructor(world?: World | null);
    getWorld(): World | null;
    /** Return all active schemes (read-only view). */
    getSchemes(): ReadonlyMap<number, Scheme>;
    /** Return scheme values as iterable. */
    schemeValues(): Iterable<Scheme>;
    /** Start a scheme and return its id. */
    start(kind: SchemeKind, owner: number, target: number, date: GameDate): number;
    /**
     * Monthly tick for all schemes.
     * @param intrigueOf Map from character id to intrigue skill
     * @param rngRoll Function returning 0~1 random number
     */
    monthlyTick(intrigueOf: Map<number, number>, rngRoll: () => number): SchemeOutcome[];
    /** Serialize scheme state for save games. */
    saveState(): Record<string, unknown>;
    /** Restore scheme state from a save. */
    loadState(s: Record<string, unknown>): void;
}
