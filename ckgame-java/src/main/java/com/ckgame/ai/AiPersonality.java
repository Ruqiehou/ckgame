package com.ckgame.ai;

import com.ckgame.core.calendar.GameDate;
import com.ckgame.core.stats.AttributeSet;
import com.ckgame.world.Character;
import com.ckgame.world.World;

/**
 * 根据角色特质、属性与年龄生成 AI 性格画像。
 */
public final class AiPersonality {

    /** 为指定角色生成性格画像。 */
    public static PersonalityProfile profileOf(World world, int who) {
        PersonalityProfile p = new PersonalityProfile();
        Character c = world.character(who);
        if (c == null) {
            return p;
        }
        for (int tid : c.traits) {
            switch (tid) {
                case 1 -> {
                    p.boldness += 0.3;
                    p.aggression += 0.1;
                }
                case 2 -> {
                    p.honor -= 0.25;
                    p.greed += 0.1;
                    p.vengeance += 0.1;
                }
                case 3 -> {
                    p.honor += 0.3;
                    p.compassion += 0.15;
                }
                case 4 -> {
                    p.greed += 0.4;
                    p.compassion -= 0.1;
                }
                case 5 -> {
                    p.compassion += 0.25;
                    p.greed -= 0.15;
                    p.sociability += 0.15;
                }
                case 6 -> {
                    p.aggression += 0.2;
                    p.boldness += 0.25;
                }
                case 7 -> {
                    p.boldness -= 0.2;
                    p.aggression -= 0.1;
                }
                case 8 -> {
                    p.zeal += 0.15;
                    p.sociability += 0.1;
                    p.aggression -= 0.05;
                }
                case 9 -> {
                    p.aggression += 0.3;
                    p.greed += 0.2;
                    p.boldness += 0.2;
                    p.vengeance += 0.1;
                }
                case 10 -> {
                    p.honor += 0.35;
                    p.aggression -= 0.2;
                    p.sociability += 0.1;
                }
                case 11 -> {
                    p.compassion -= 0.3;
                    p.vengeance += 0.3;
                    p.aggression += 0.15;
                }
                case 12 -> {
                    p.sociability += 0.35;
                    p.compassion += 0.1;
                }
                default -> {
                    // 未识别的特质不产生影响
                }
            }
        }
        AttributeSet attrs = world.effectiveAttrs(who);
        if (attrs != null) {
            p.aggression += (attrs.martial() - 8) * 0.03;
            p.greed += (attrs.stewardship() - 8) * 0.02;
            p.zeal += (attrs.learning() - 8) * 0.02;
            p.sociability += (attrs.diplomacy() - 8) * 0.03;
            p.vengeance += (attrs.intrigue() - 8) * 0.02;
            p.boldness -= (c.stress / 400.0) * 0.2;
            p.compassion -= (c.stress / 400.0) * 0.1;
        }
        GameDate date = world.date;
        int age = c.ageAt(date);
        if (age < 25) {
            p.boldness += 0.1;
            p.aggression += 0.05;
        } else if (age > 55) {
            p.boldness -= 0.1;
            p.aggression -= 0.05;
            p.honor += 0.05;
        }
        p.aggression = clamp(p.aggression);
        p.greed = clamp(p.greed);
        p.honor = clamp(p.honor);
        p.zeal = clamp(p.zeal);
        p.boldness = clamp(p.boldness);
        p.compassion = clamp(p.compassion);
        p.sociability = clamp(p.sociability);
        p.vengeance = clamp(p.vengeance);
        return p;
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(1.5, v));
    }

    /** 根据画像返回简短性格描述。 */
    public static String describe(PersonalityProfile p) {
        if (p.warlikeScore() > 0.8) {
            return "好战";
        }
        if (p.intrigueScore() > 0.8) {
            return "阴险";
        }
        if (p.diplomatScore() > 0.8) {
            return "圆滑";
        }
        if (p.greed > 0.9) {
            return "贪婪";
        }
        if (p.zeal > 0.9) {
            return "虔诚";
        }
        return "务实";
    }
}
