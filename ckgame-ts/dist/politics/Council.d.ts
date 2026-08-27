import { AttributeSet } from '../core/stats/AttributeSet.js';
import { CouncilPosition } from './CouncilPosition.js';
import { CouncilTask } from './CouncilTask.js';
/**
 * Monthly council output result.
 */
export declare class CouncilMonthlyResult {
    gold: number;
    prestige: number;
    piety: number;
    controlGain: number;
    developmentChance: number;
    claimProgress: number;
    logs: string[];
}
/**
 * A single ruler's council.
 */
export declare class Council {
    readonly ruler: number;
    chancellor: number;
    marshal: number;
    steward: number;
    spymaster: number;
    chaplain: number;
    tasks: Map<CouncilPosition, CouncilTask>;
    constructor(ruler: number);
    /** Create an empty council with default tasks. */
    static empty(ruler: number): Council;
    /** Get the current holder of a position. */
    get(pos: CouncilPosition): number;
    /** Appoint someone to a position. */
    set(pos: CouncilPosition, who: number): void;
    /** Return all appointed positions with their holder ids. */
    members(): [CouncilPosition, number][];
    /** Get the current task for a position. */
    taskOf(pos: CouncilPosition): CouncilTask;
    /**
     * Auto-appoint council members from candidates.
     * Each candidate is: [id, diplomacy, martial, stewardship, intrigue, learning].
     */
    autoAppoint(candidates: number[][]): void;
    /**
     * Compute monthly council effects.
     * @param skillOf Map from character id to their AttributeSet; defaults to AttributeSet.defaults()
     */
    monthlyEffect(skillOf: Map<number, AttributeSet>): CouncilMonthlyResult;
}
