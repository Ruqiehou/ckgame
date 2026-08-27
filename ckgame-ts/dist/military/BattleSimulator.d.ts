import { Army } from './Army.js';
import type { BattleResult } from './BattleResult.js';
/**
 * Battle simulator: resolves combat between two armies.
 */
export declare class BattleSimulator {
    /**
     * Resolve a battle.
     * @param attacker Attacking army
     * @param defender Defending army
     * @param atkMartial Attacker commander martial skill
     * @param defMartial Defender commander martial skill
     * @param terrainWidth Terrain combat width compression factor (0~1)
     * @param rng Optional random number generator (returns 0~1)
     * @param seasonMod Season combat modifier
     */
    static resolve(attacker: Army, defender: Army, atkMartial: number, defMartial: number, terrainWidth: number, rng?: () => number, seasonMod?: number): BattleResult;
    private static applyLosses;
    /** Compute total army combat power. */
    private static computePower;
}
