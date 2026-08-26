package com.ckgame.ai;

/**
 * AI 性格维度集合。
 */
public final class PersonalityProfile {
    public double aggression = 0.5;
    public double greed = 0.5;
    public double honor = 0.5;
    public double zeal = 0.5;
    public double boldness = 0.5;
    public double compassion = 0.5;
    public double sociability = 0.5;
    public double vengeance = 0.5;

    /** 好战倾向综合分。 */
    public double warlikeScore() {
        return Math.max(0.0, Math.min(1.5,
                aggression * 0.4
                        + boldness * 0.3
                        + vengeance * 0.2
                        - compassion * 0.1));
    }

    /** 阴谋倾向综合分。 */
    public double intrigueScore() {
        return Math.max(0.0, Math.min(1.5,
                (1.0 - honor) * 0.5
                        + greed * 0.2
                        + vengeance * 0.3));
    }

    /** 外交倾向综合分。 */
    public double diplomatScore() {
        return Math.max(0.0, Math.min(1.5,
                compassion * 0.3
                        + sociability * 0.4
                        + honor * 0.3));
    }
}
