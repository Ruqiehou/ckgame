package com.ckgame.core.traits;

import com.ckgame.core.stats.AttributeSet;

import java.util.List;

/**
 * 内置特质表。
 */
public final class Traits {
    private Traits() {}

    public static List<Trait> builtin() {
        return List.of(
                new Trait(1, "勇敢", TraitKind.PERSONALITY,
                        new AttributeSet(0, 1, 0, 0, 0, 2), 0, 5, 0.0, 0.0, ""),
                new Trait(2, "狡诈", TraitKind.PERSONALITY,
                        new AttributeSet(-1, 0, 0, 3, 0, 0), 0, -5, 0.0, 0.0, ""),
                new Trait(3, "公正", TraitKind.PERSONALITY,
                        new AttributeSet(2, 0, 1, 0, 0, 0), 0, 10, 0.0, 0.0, ""),
                new Trait(4, "贪婪", TraitKind.PERSONALITY,
                        new AttributeSet(-1, 0, 2, 0, 0, 0), 0, -8, 0.0, 0.0, ""),
                new Trait(5, "慷慨", TraitKind.PERSONALITY,
                        new AttributeSet(2, 0, -1, 0, 0, 0), 0, 8, 0.0, 0.0, ""),
                new Trait(6, "军事天才", TraitKind.COMMANDER,
                        new AttributeSet(0, 4, 0, 0, 0, 2), 0, 5, 0.0, 0.0, ""),
                new Trait(7, "病弱", TraitKind.HEALTH,
                        new AttributeSet(0, 0, 0, 0, 0, -2), 0, 0, -0.1, -0.5, ""),
                new Trait(8, "博学", TraitKind.EDUCATION,
                        new AttributeSet(0, 0, 0, 0, 3, 0), 0, 3, 0.0, 0.0, ""),
                new Trait(9, "野心勃勃", TraitKind.PERSONALITY,
                        new AttributeSet(0, 1, 1, 1, 0, 0), 0, -3, 0.0, 0.0, ""),
                new Trait(10, "忠诚", TraitKind.PERSONALITY,
                        new AttributeSet(1, 0, 0, 0, 0, 0), 0, 12, 0.0, 0.0, ""),
                new Trait(11, "残忍", TraitKind.PERSONALITY,
                        new AttributeSet(-2, 0, 0, 2, 0, 0), 0, -15, 0.0, 0.0, ""),
                new Trait(12, "魅力四射", TraitKind.PERSONALITY,
                        new AttributeSet(3, 0, 0, 0, 0, 0), 0, 10, 0.0, 0.0, "")
        );
    }
}
