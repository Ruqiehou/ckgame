package com.ckgame.politics;

public enum CrownAuthority {
    AUTONOMOUS(0, "自治王权", 0, 0.0),
    LIMITED(1, "有限王权", -5, 0.05),
    HIGH(2, "高度王权", -15, 0.15),
    ABSOLUTE(3, "绝对王权", -30, 0.25);

    private final int value;
    private final String nameZh;
    private final int vassalOpinionPenalty;
    private final double taxBonus;

    CrownAuthority(int value, String nameZh, int vassalOpinionPenalty, double taxBonus) {
        this.value = value;
        this.nameZh = nameZh;
        this.vassalOpinionPenalty = vassalOpinionPenalty;
        this.taxBonus = taxBonus;
    }

    public int value() { return value; }
    public String nameZh() { return nameZh; }
    public int vassalOpinionPenalty() { return vassalOpinionPenalty; }
    public double taxBonus() { return taxBonus; }
}
