package com.ckgame.politics;

public enum SuccessionLaw {
    PRIMOGENITURE("长子继承制"),
    CONFEDERATE_PARTITION("联邦分割继承"),
    ELECTIVE("选举君主制"),
    HOUSE_SENIORITY("家族长老制"),
    ULTIMOGENITURE("幼子继承制");

    private final String nameZh;

    SuccessionLaw(String nameZh) {
        this.nameZh = nameZh;
    }

    public String nameZh() { return nameZh; }
}
