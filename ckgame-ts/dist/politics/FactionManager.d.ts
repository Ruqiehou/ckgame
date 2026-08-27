import type { World } from '../world/World.js';
import { Faction } from './Faction.js';
import { FactionKind } from './FactionKind.js';
import { FactionEvent } from './FactionEvent.js';
/**
 * Faction manager: creates, tracks, and ticks factions.
 */
export declare class FactionManager {
    private world;
    private factions;
    private nextId;
    constructor(world?: World | null);
    getWorld(): World | null;
    /** Return factions map (read-only view). */
    getFactions(): ReadonlyMap<number, Faction>;
    /** Create a new faction and return its id. */
    create(kind: FactionKind, liege: number, claimant?: number): number;
    /** A character joins a faction. */
    join(factionId: number, who: number): void;
    /** A character leaves a faction. */
    leave(factionId: number, who: number): void;
    /** Dissolve a faction. */
    dissolve(factionId: number): void;
    /**
     * Liege appeases a faction: reduces discontent; may dissolve if low enough.
     * @param amount If undefined, uses default appease value.
     */
    appease(factionId: number, amount?: number): boolean;
    /** Find a faction for a given liege and kind. Returns faction id or null. */
    findForLiege(liege: number, kind: FactionKind): number | null;
    /** Recompute faction power from military power maps. */
    recomputePower(militaryPower: Map<number, number>, liegePower: Map<number, number>): void;
    /** Tick discontent based on opinion of liege. */
    tickDiscontent(opinionOfLiege: Map<number, number>): void;
    /**
     * Monthly AI for factions.
     * @param vassals Array of [vassal, liege, opinion] triples
     * @param rngRoll Function returning 0~1 random number
     */
    monthlyAi(vassals: number[][], rngRoll: () => number): FactionEvent[];
    /** Serialize faction state for save games. */
    saveState(): Record<string, unknown>;
    /** Restore faction state from a save. */
    loadState(s: Record<string, unknown>): void;
}
