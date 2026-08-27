package com.ckgame.military;

import java.util.Random;

/**
 * 战斗模拟器。
 */
public final class BattleSimulator {

    private BattleSimulator() {}

    /**
     * 结算一场战斗。
     *
     * @param attacker      进攻方军队
     * @param defender      防守方军队
     * @param atkMartial    进攻方指挥官军略
     * @param defMartial    防守方指挥官军略
     * @param terrainWidth  地形战斗宽度压缩系数
     * @param rng           随机数生成器，可为 null
     * @param seasonMod     季节修正
     * @return 战斗结果
     */
    public static BattleResult resolve(Army attacker, Army defender,
                                       int atkMartial, int defMartial,
                                       double terrainWidth,
                                       Random rng, double seasonMod) {
        Random r = rng != null ? rng : new Random();
        double atkPow = computePower(attacker) * (1.0 + (atkMartial - 8) * 0.04);
        double defPow = computePower(defender) * (1.0 + (defMartial - 8) * 0.04);

        // 季节修正：冬季/秋季略降总体战力，冬季防守方略优
        seasonMod = Math.max(0.7, Math.min(1.15, seasonMod));
        atkPow *= seasonMod;
        defPow *= seasonMod * (seasonMod < 0.95 ? 1.05 : 1.0);

        // 地形压缩优势
        double ratio = (atkPow + 1.0) / (defPow + 1.0);
        ratio = Math.pow(ratio, terrainWidth);
        double noise = 0.85 + (1.15 - 0.85) * r.nextDouble();
        double score = ratio * noise;

        boolean attackerWon = score >= 1.0;
        double lossScale = seasonMod < 0.95 ? 1.15 : 1.0; // 恶劣季节伤亡更高
        int baseLossA = (int) (attacker.totalMen() * (0.08 + (0.22 - 0.08) * r.nextDouble()) * lossScale);
        int baseLossD = (int) (defender.totalMen() * (0.08 + (0.22 - 0.08) * r.nextDouble()) * lossScale);
        if (attackerWon) {
            baseLossD = (int) (baseLossD * 1.6);
            baseLossA = (int) (baseLossA * 0.7);
        } else {
            baseLossA = (int) (baseLossA * 1.6);
            baseLossD = (int) (baseLossD * 0.7);
        }

        int aLoss = applyLosses(attacker, baseLossA);
        int dLoss = applyLosses(defender, baseLossD);
        attacker.status = ArmyStatus.IDLE;
        defender.status = ArmyStatus.IDLE;

        int warscore = (int) Math.max(5, Math.min(40, Math.abs(score - 1.0) * 40 + 10));
        if (!attackerWon) {
            warscore = -warscore;
        }
        String desc = String.format("%s胜利 (损 %d/%d，比分 %.2f)",
                attackerWon ? "进攻方" : "防守方", aLoss, dLoss, score);
        return new BattleResult(attackerWon, aLoss, dLoss, warscore, desc);
    }

    public static BattleResult resolve(Army attacker, Army defender,
                                       int atkMartial, int defMartial,
                                       double terrainWidth) {
        return resolve(attacker, defender, atkMartial, defMartial, terrainWidth, null, 1.0);
    }

    public static BattleResult resolve(Army attacker, Army defender,
                                       int atkMartial, int defMartial,
                                       double terrainWidth, double seasonMod) {
        return resolve(attacker, defender, atkMartial, defMartial, terrainWidth, null, seasonMod);
    }

    private static int applyLosses(Army army, int total) {
        int remaining = total;
        int killed = 0;
        for (UnitStack s : army.stacks) {
            if (remaining <= 0) {
                break;
            }
            int share = Math.max(1, (int) (total * ((double) s.men / Math.max(1, army.totalMen()))));
            int k = s.takeCasualties(Math.min(share, remaining));
            killed += k;
            remaining -= k;
        }
        army.morale = Math.max(10, (int)(army.morale - 15.0));
        return killed;
    }

    /** 计算军队总战力（各兵种堆叠战力之和）。 */
    private static double computePower(Army army) {
        double total = 0.0;
        for (UnitStack s : army.stacks) {
            total += s.combatPower();
        }
        return total;
    }
}
