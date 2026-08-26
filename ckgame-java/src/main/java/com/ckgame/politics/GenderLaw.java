package com.ckgame.politics;

import com.ckgame.core.Gender;

public enum GenderLaw {
    AGNATIC("男系继承"),
    AGNATIC_COGNATIC("男系优先"),
    ABSOLUTE_COGNATIC("绝对双系"),
    ENATIC("女系继承");

    private final String nameZh;

    GenderLaw(String nameZh) {
        this.nameZh = nameZh;
    }

    public String nameZh() { return nameZh; }

    public boolean allows(Gender gender, boolean hasMaleHeir) {
        return switch (this) {
            case AGNATIC -> gender == Gender.MALE;
            case AGNATIC_COGNATIC -> gender == Gender.MALE || !hasMaleHeir;
            case ENATIC -> gender == Gender.FEMALE;
            case ABSOLUTE_COGNATIC -> true;
        };
    }
}
