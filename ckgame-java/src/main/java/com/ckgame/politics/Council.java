package com.ckgame.politics;

import com.ckgame.core.Constants;
import com.ckgame.core.stats.AttributeSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 单个统治者的内阁。
 */
public final class Council {
    public final int ruler;
    public int chancellor = Constants.NONE_ID;
    public int marshal = Constants.NONE_ID;
    public int steward = Constants.NONE_ID;
    public int spymaster = Constants.NONE_ID;
    public int chaplain = Constants.NONE_ID;
    public final Map<CouncilPosition, CouncilTask> tasks = new HashMap<>();

    public Council(int ruler) {
        this.ruler = ruler;
    }

    /** 创建空内阁并设置默认任务。 */
    public static Council empty(int ruler) {
        Council c = new Council(ruler);
        c.tasks.put(CouncilPosition.CHANCELLOR, CouncilTask.DOMESTIC_RELATIONS);
        c.tasks.put(CouncilPosition.MARSHAL, CouncilTask.TRAIN_COMMANDERS);
        c.tasks.put(CouncilPosition.STEWARD, CouncilTask.COLLECT_TAXES);
        c.tasks.put(CouncilPosition.SPYMASTER, CouncilTask.DISRUPT_SCHEMES);
        c.tasks.put(CouncilPosition.COURT_CHAPLAIN, CouncilTask.CONVERT_FAITH);
        return c;
    }

    /** 查询某职位的当前任职者。 */
    public int get(CouncilPosition pos) {
        return switch (pos) {
            case CHANCELLOR -> chancellor;
            case MARSHAL -> marshal;
            case STEWARD -> steward;
            case SPYMASTER -> spymaster;
            case COURT_CHAPLAIN -> chaplain;
        };
    }

    /** 任命某职位。 */
    public void set(CouncilPosition pos, int who) {
        switch (pos) {
            case CHANCELLOR -> chancellor = who;
            case MARSHAL -> marshal = who;
            case STEWARD -> steward = who;
            case SPYMASTER -> spymaster = who;
            case COURT_CHAPLAIN -> chaplain = who;
        }
    }

    /** 返回所有已任命的职位与人物 ID。 */
    public List<Map.Entry<CouncilPosition, Integer>> members() {
        List<Map.Entry<CouncilPosition, Integer>> out = new ArrayList<>();
        for (CouncilPosition p : CouncilPosition.all()) {
            int who = get(p);
            if (who != Constants.NONE_ID) {
                out.add(Map.entry(p, who));
            }
        }
        return out;
    }

    /** 返回某职位的当前任务。 */
    public CouncilTask taskOf(CouncilPosition pos) {
        return tasks.getOrDefault(pos, CouncilTask.DOMESTIC_RELATIONS);
    }

    /**
     * 自动任命内阁。
     *
     * @param candidates 候选列表，每个元素为 int[6]：id, dip, mar, ste, int, lea
     */
    public void autoAppoint(List<int[]> candidates) {
        java.util.Set<Integer> used = new java.util.HashSet<>();
        if (ruler != Constants.NONE_ID) {
            used.add(ruler);
        }

        java.util.function.IntUnaryOperator pick = (int scoreIdx) -> {
            int best = Constants.NONE_ID;
            int bestS = -999;
            for (int[] row : candidates) {
                if (used.contains(row[0])) {
                    continue;
                }
                if (row[scoreIdx] > bestS) {
                    bestS = row[scoreIdx];
                    best = row[0];
                }
            }
            if (best != Constants.NONE_ID) {
                used.add(best);
            }
            return best;
        };

        chancellor = pick.applyAsInt(1);
        marshal = pick.applyAsInt(2);
        steward = pick.applyAsInt(3);
        spymaster = pick.applyAsInt(4);
        chaplain = pick.applyAsInt(5);
    }

    /**
     * 计算月度效果。
     *
     * @param skillOf 人物 ID 到属性集合的映射；缺省值为 AttributeSet.defaults()
     */
    public CouncilMonthlyResult monthlyEffect(Map<Integer, AttributeSet> skillOf) {
        CouncilMonthlyResult r = new CouncilMonthlyResult();
        for (Map.Entry<CouncilPosition, Integer> e : members()) {
            CouncilPosition pos = e.getKey();
            int who = e.getValue();
            AttributeSet skills = skillOf.getOrDefault(who, AttributeSet.defaults());
            CouncilTask task = taskOf(pos);
            switch (task) {
                case DOMESTIC_RELATIONS -> r.prestige += skills.diplomacy() * 0.3;
                case FABRICATE_CLAIM -> r.claimProgress += skills.diplomacy() * 0.8;
                case TRAIN_COMMANDERS -> {
                    r.prestige += skills.martial() * 0.15;
                    r.controlGain += skills.martial() * 0.1;
                }
                case INCREASE_CONTROL -> r.controlGain += skills.martial() * 0.4;
                case COLLECT_TAXES -> r.gold += skills.stewardship() * 0.6;
                case DEVELOP_COUNTY -> r.developmentChance += skills.stewardship() * 0.5;
                case DISRUPT_SCHEMES -> {
                    r.prestige += skills.intrigue() * 0.1;
                    r.logs.add("间谍总管破坏敌对阴谋");
                }
                case SUPPORT_MURDER -> r.claimProgress += skills.intrigue() * 0.3;
                case CONVERT_FAITH -> r.piety += skills.learning() * 0.5;
                case FABRICATE_HOOK -> {
                    r.piety += skills.learning() * 0.2;
                    r.claimProgress += skills.learning() * 0.25;
                }
                case RECRUIT_KNIGHTS -> {
                    r.gold -= 15;
                    r.prestige += 5;
                    r.logs.add("骑士招募中");
                }
                case IMPROVE_DIPLOMACY -> {
                    r.prestige += skills.diplomacy() * 0.4;
                    r.logs.add("外交关系改善");
                }
                case SPREAD_CULTURE -> {
                    r.controlGain += skills.learning() * 0.3;
                    r.piety += skills.learning() * 0.2;
                    r.logs.add("文化传播中");
                }
                case ESTABLISH_TRADE -> {
                    r.gold += skills.diplomacy() * 0.5;
                    r.prestige += skills.stewardship() * 0.2;
                    r.logs.add("商路建立中");
                }
                case MAINTAIN_BUILDINGS -> {
                    r.gold -= 5;
                    r.controlGain += skills.stewardship() * 0.2;
                    r.logs.add("建筑维护中");
                }
                case TRAIN_TROOPS -> {
                    r.gold -= 10;
                    r.controlGain += skills.martial() * 0.3;
                    r.logs.add("部队训练中");
                }
                case GATHER_INTEL -> {
                    r.prestige += skills.intrigue() * 0.3;
                    r.claimProgress += skills.intrigue() * 0.2;
                    r.logs.add("情报收集中");
                }
                case PROMOTE_CULTURE -> {
                    r.controlGain += skills.learning() * 0.4;
                    r.piety += skills.learning() * 0.1;
                    r.logs.add("文化推广中");
                }
                default -> {
                    // 职位默认产出，避免空任务白占席位
                    switch (pos) {
                        case CHANCELLOR -> r.prestige += skills.diplomacy() * 0.15;
                        case MARSHAL -> r.controlGain += skills.martial() * 0.1;
                        case STEWARD -> r.gold += skills.stewardship() * 0.2;
                        case SPYMASTER -> r.prestige += skills.intrigue() * 0.1;
                        case COURT_CHAPLAIN -> r.piety += skills.learning() * 0.2;
                    }
                }
            }
        }
        return r;
    }
}
