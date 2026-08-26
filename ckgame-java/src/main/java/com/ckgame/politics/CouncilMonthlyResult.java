package com.ckgame.politics;

import java.util.ArrayList;
import java.util.List;

/**
 * 内阁月度产出结果。
 */
public final class CouncilMonthlyResult {
    public double gold = 0.0;
    public double prestige = 0.0;
    public double piety = 0.0;
    public double controlGain = 0.0;
    public double developmentChance = 0.0;
    public double claimProgress = 0.0;
    public final List<String> logs = new ArrayList<>();
}
