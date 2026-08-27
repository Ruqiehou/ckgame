import { Army } from './Army.js';
import { ArmyStatus } from './ArmyStatus.js';
import type { BattleResult } from './BattleResult.js';

/**
 * Battle simulator: resolves combat between two armies.
 */
export class BattleSimulator {

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
  static resolve(
    attacker: Army,
    defender: Army,
    atkMartial: number,
    defMartial: number,
    terrainWidth: number,
    rng?: () => number,
    seasonMod: number = 1.0,
  ): BattleResult {
    const rand = rng ?? Math.random;

    let atkPow = BattleSimulator.computePower(attacker) * (1.0 + (atkMartial - 8) * 0.04);
    let defPow = BattleSimulator.computePower(defender) * (1.0 + (defMartial - 8) * 0.04);

    // Season modifier
    seasonMod = Math.max(0.7, Math.min(1.15, seasonMod));
    atkPow *= seasonMod;
    defPow *= seasonMod * (seasonMod < 0.95 ? 1.05 : 1.0);

    // Terrain compression
    let ratio = (atkPow + 1.0) / (defPow + 1.0);
    ratio = Math.pow(ratio, terrainWidth);
    const noise = 0.85 + (1.15 - 0.85) * rand();
    const score = ratio * noise;

    const attackerWon = score >= 1.0;
    const lossScale = seasonMod < 0.95 ? 1.15 : 1.0;
    let baseLossA = Math.floor(attacker.totalMen() * (0.08 + (0.22 - 0.08) * rand()) * lossScale);
    let baseLossD = Math.floor(defender.totalMen() * (0.08 + (0.22 - 0.08) * rand()) * lossScale);

    if (attackerWon) {
      baseLossD = Math.floor(baseLossD * 1.6);
      baseLossA = Math.floor(baseLossA * 0.7);
    } else {
      baseLossA = Math.floor(baseLossA * 1.6);
      baseLossD = Math.floor(baseLossD * 0.7);
    }

    const aLoss = BattleSimulator.applyLosses(attacker, baseLossA);
    const dLoss = BattleSimulator.applyLosses(defender, baseLossD);
    attacker.status = ArmyStatus.IDLE;
    defender.status = ArmyStatus.IDLE;

    let warscore = Math.max(5, Math.min(40, Math.abs(score - 1.0) * 40 + 10));
    if (!attackerWon) warscore = -warscore;

    const desc = `${attackerWon ? '进攻方' : '防守方'}胜利 (损 ${aLoss}/${dLoss}，比分 ${score.toFixed(2)})`;
    return { attackerWon, attackerLosses: aLoss, defenderLosses: dLoss, warscoreChange: warscore, description: desc };
  }

  private static applyLosses(army: Army, total: number): number {
    let remaining = total;
    let killed = 0;
    const armyTotal = Math.max(1, army.totalMen());
    for (const s of army.stacks) {
      if (remaining <= 0) break;
      const share = Math.max(1, Math.floor(total * (s.men / armyTotal)));
      const k = s.takeCasualties(Math.min(share, remaining));
      killed += k;
      remaining -= k;
    }
    army.morale = Math.max(10, Math.floor(army.morale - 15));
    return killed;
  }

  /** Compute total army combat power. */
  private static computePower(army: Army): number {
    let total = 0;
    for (const s of army.stacks) {
      total += s.combatPower();
    }
    return total;
  }
}
