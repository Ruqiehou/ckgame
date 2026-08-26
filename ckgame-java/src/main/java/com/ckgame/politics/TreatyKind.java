package com.ckgame.politics;

/**
 * 条约类型。
 */
public enum TreatyKind {
    ALLIANCE("同盟"),
    NON_AGGRESSION("互不侵犯"),
    MARRIAGE_PACT("联姻协定"),
    TRUCE("停战"),
    VASSALAGE("附庸关系"),
    TRADE_AGREEMENT("贸易协定"),
    INTELLIGENCE_SHARING("情报共享");

    private final String nameZh;

    TreatyKind(String nameZh) {
        this.nameZh = nameZh;
    }

    public String nameZh() {
        return nameZh;
    }
}
