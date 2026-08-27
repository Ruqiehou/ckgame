import { GameDate } from '../core/calendar/GameDate.js';
import { DiplomacyFlags } from './DiplomacyFlags.js';
import { Treaty } from './Treaty.js';
import { Claim } from './Claim.js';
import type { World } from '../world/World.js';
/**
 * Diplomacy system: relations, treaties, claims, war exhaustion.
 */
export declare class Diplomacy {
    private world;
    private relations;
    private treaties;
    private claims;
    private truceUntil;
    private warExhaustion;
    constructor(world?: World | null);
    /** The world reference held at construction (may be null). */
    getWorld(): World | null;
    private pairKey;
    /** Get diplomatic flags between two rulers (read-only safe: returns new object if none). */
    flags(a: number, b: number): DiplomacyFlags;
    /** Get or create diplomatic flags between two rulers. */
    flagsMut(a: number, b: number): DiplomacyFlags;
    /** Set two rulers as rivals. */
    setRival(a: number, b: number): void;
    /** Form an alliance treaty. */
    formAlliance(a: number, b: number, date: GameDate): void;
    /** Add an arbitrary treaty to the internal list. */
    addTreaty(treaty: Treaty): void;
    /** Set war status; going to war automatically cancels alliance and non-aggression. */
    setAtWar(a: number, b: number, atWar: boolean): void;
    /** Whether two rulers are allied. */
    areAllied(a: number, b: number): boolean;
    /** Return all allies of a ruler. */
    alliesOf(who: number): number[];
    /** Add war exhaustion (default: declare-war constant). */
    addWarExhaustion(who: number, amount?: number): void;
    /** Check whether a ruler can declare war on another in a given year. */
    canDeclareWar(a: number, b: number, year: number): boolean;
    /** Whether a valid truce exists between two rulers. */
    hasTruce(a: number, b: number, year: number): boolean;
    /** Set truce expiry year. */
    setTruce(a: number, b: number, untilYear: number): void;
    /** Add a claim (with county). */
    addClaim(claimant: number, title: number, county: number | null, strength: number): void;
    /** Add a claim (without county). */
    addClaim(claimant: number, title: number, strength: number): void;
    /** Return all claims of a character. */
    claimsOf(who: number): Claim[];
    /** War exhaustion map (read-only view). */
    getWarExhaustion(): ReadonlyMap<number, number>;
    /** Treaty list (read-only view). */
    getTreaties(): readonly Treaty[];
    /** Monthly decay of war exhaustion; exceptIds are skipped. */
    tickWarExhaustion(exceptIds?: Iterable<number>): void;
    /** Expire treaties past the given year; returns log lines. */
    expireTreaties(year: number, world: World): string[];
    /** Compute opinion gain from a gift. */
    static giftOpinionGain(amount: number): number;
    /** Serialize diplomacy state for save games. */
    saveState(): Record<string, unknown>;
    /** Restore diplomacy state from a save. */
    loadState(s: Record<string, unknown>): void;
}
