package com.ckgame.core.stats;

/**
 * 角色六维属性集合。不可变，所有修改操作返回新对象。
 */
public final class AttributeSet {
    private final int diplomacy;
    private final int martial;
    private final int stewardship;
    private final int intrigue;
    private final int learning;
    private final int prowess;

    public AttributeSet(int diplomacy, int martial, int stewardship, int intrigue, int learning, int prowess) {
        this.diplomacy = diplomacy;
        this.martial = martial;
        this.stewardship = stewardship;
        this.intrigue = intrigue;
        this.learning = learning;
        this.prowess = prowess;
    }

    public static AttributeSet zero() {
        return new AttributeSet(0, 0, 0, 0, 0, 0);
    }

    public static AttributeSet defaults() {
        return new AttributeSet(8, 8, 8, 8, 8, 8);
    }

    public int diplomacy() { return diplomacy; }
    public int martial() { return martial; }
    public int stewardship() { return stewardship; }
    public int intrigue() { return intrigue; }
    public int learning() { return learning; }
    public int prowess() { return prowess; }

    public AttributeSet add(AttributeSet other) {
        return new AttributeSet(
                this.diplomacy + other.diplomacy,
                this.martial + other.martial,
                this.stewardship + other.stewardship,
                this.intrigue + other.intrigue,
                this.learning + other.learning,
                this.prowess + other.prowess
        );
    }

    public AttributeSet clamp(int lo, int hi) {
        return new AttributeSet(
                clamp(diplomacy, lo, hi),
                clamp(martial, lo, hi),
                clamp(stewardship, lo, hi),
                clamp(intrigue, lo, hi),
                clamp(learning, lo, hi),
                clamp(prowess, lo, hi)
        );
    }

    private static int clamp(int value, int lo, int hi) {
        return Math.max(lo, Math.min(hi, value));
    }

    public int total() {
        return diplomacy + martial + stewardship + intrigue + learning + prowess;
    }

    @Override
    public String toString() {
        return String.format("AttributeSet[dip=%d, mar=%d, stew=%d, int=%d, lear=%d, prow=%d]",
                diplomacy, martial, stewardship, intrigue, learning, prowess);
    }
}
