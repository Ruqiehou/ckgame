import type { World } from '../world/World.js';
import { Council } from './Council.js';
/**
 * Council registry: manages councils per ruler.
 */
export declare class CouncilManager {
    private world;
    private byRuler;
    constructor(world?: World | null);
    getWorld(): World | null;
    /** Get or create a council for the given ruler. */
    councilFor(ruler: number): Council;
    /** Get a ruler's council; returns undefined if none exists. */
    get(ruler: number): Council | undefined;
    /** Return all councils. */
    allCouncils(): Map<number, Council>;
    /** Serialize council state for save games. */
    saveState(): Record<string, unknown>;
    /** Restore council state from a save. */
    loadState(s: Record<string, unknown>): void;
}
