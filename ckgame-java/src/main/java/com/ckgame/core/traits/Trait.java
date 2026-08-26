package com.ckgame.core.traits;

import com.ckgame.core.stats.AttributeSet;

/**
 * 角色特质。
 */
public final class Trait {
    private final int id;
    private final String name;
    private final TraitKind kind;
    private final AttributeSet attrBonus;
    private final int opinionSelf;
    private final int opinionOthers;
    private final double fertilityMod;
    private final double healthMod;
    private final String description;

    public Trait(int id, String name, TraitKind kind,
                 AttributeSet attrBonus, int opinionSelf, int opinionOthers,
                 double fertilityMod, double healthMod, String description) {
        this.id = id;
        this.name = name;
        this.kind = kind;
        this.attrBonus = attrBonus != null ? attrBonus : AttributeSet.zero();
        this.opinionSelf = opinionSelf;
        this.opinionOthers = opinionOthers;
        this.fertilityMod = fertilityMod;
        this.healthMod = healthMod;
        this.description = description != null ? description : "";
    }

    public Trait(int id, String name, TraitKind kind) {
        this(id, name, kind, AttributeSet.zero(), 0, 0, 0.0, 0.0, "");
    }

    public int id() { return id; }
    public String name() { return name; }
    public TraitKind kind() { return kind; }
    public AttributeSet attrBonus() { return attrBonus; }
    public int opinionSelf() { return opinionSelf; }
    public int opinionOthers() { return opinionOthers; }
    public double fertilityMod() { return fertilityMod; }
    public double healthMod() { return healthMod; }
    public String description() { return description; }
}
