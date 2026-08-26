package com.ckgame.politics;

/**
 * 战争借口（最小化实现，对应 Python diplomacy.CasusBelli）。
 */
public enum CasusBelli {
    CLAIM,
    CONQUEST,
    INDEPENDENCE,
    DEPOSE_LIEGE,
    HOLY_WAR,
    DE_JURE,
    RIVALRY,
    SUBJUGATION,
    TRADE_WAR;

    /** 战争目标分数。 */
    public int warscoreGoal() {
        return this == CONQUEST ? 80 : 100;
    }

    /** 中文名称（供日志使用）。 */
    public String nameZh() {
        return switch (this) {
            case CLAIM -> "宣称战争";
            case CONQUEST -> "征服";
            case INDEPENDENCE -> "独立战争";
            case DEPOSE_LIEGE -> "废黜领主";
            case HOLY_WAR -> "圣战";
            case DE_JURE -> "法理战争";
            case RIVALRY -> "世仇战争";
            case SUBJUGATION -> "臣服战争";
            case TRADE_WAR -> "贸易战争";
        };
    }

    /** 宣战时消耗的威望。 */
    public double prestigeCost() {
        return switch (this) {
            case CLAIM -> 50.0;
            case CONQUEST -> 100.0;
            case INDEPENDENCE -> 0.0;
            case DEPOSE_LIEGE -> 150.0;
            case HOLY_WAR -> 0.0;
            case DE_JURE -> 75.0;
            case RIVALRY -> 25.0;
            case SUBJUGATION -> 200.0;
            case TRADE_WAR -> 50.0;
        };
    }

    /** 胜利时攻击者获得的威望。 */
    public double attackerPrestigeOnWin() {
        return switch (this) {
            case CLAIM -> 50.0;
            case CONQUEST -> 80.0;
            case INDEPENDENCE -> 100.0;
            case DEPOSE_LIEGE -> 120.0;
            case HOLY_WAR -> 150.0;
            case DE_JURE -> 60.0;
            case RIVALRY -> 40.0;
            case SUBJUGATION -> 200.0;
            case TRADE_WAR -> 60.0;
        };
    }
}
