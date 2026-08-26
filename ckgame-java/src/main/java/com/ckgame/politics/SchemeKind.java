package com.ckgame.politics;

/**
 * 阴谋类型。
 */
public enum SchemeKind {
    MURDER("谋杀"),
    ABDUCT("绑架"),
    FABRICATE_HOOK("伪造把柄"),
    SWAY("拉拢"),
    SEDUCE("引诱"),
    CLAIM_FABRICATION("伪造宣称");

    private final String nameZh;

    SchemeKind(String nameZh) {
        this.nameZh = nameZh;
    }

    public String nameZh() {
        return nameZh;
    }

    /** 基础每月进度。 */
    public double baseMonthlyProgress() {
        return switch (this) {
            case MURDER -> 5.0;
            case ABDUCT -> 4.0;
            case FABRICATE_HOOK -> 6.0;
            case SWAY -> 8.0;
            case SEDUCE -> 7.0;
            case CLAIM_FABRICATION -> 3.0;
        };
    }

    /** 基础隐秘值。 */
    public double secrecyBase() {
        return switch (this) {
            case MURDER -> 40.0;
            case ABDUCT -> 35.0;
            case FABRICATE_HOOK -> 50.0;
            case SWAY -> 80.0;
            case SEDUCE -> 60.0;
            case CLAIM_FABRICATION -> 70.0;
        };
    }
}
