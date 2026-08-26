package com.ckgame.politics;

import java.util.List;

/**
 * 内阁职位。
 */
public enum CouncilPosition {
    CHANCELLOR("首相"),
    MARSHAL("元帅"),
    STEWARD("总管"),
    SPYMASTER("间谍总管"),
    COURT_CHAPLAIN("宫廷神甫");

    private final String nameZh;

    CouncilPosition(String nameZh) {
        this.nameZh = nameZh;
    }

    public String nameZh() {
        return nameZh;
    }

    /** 返回所有职位。 */
    public static List<CouncilPosition> all() {
        return List.of(values());
    }
}
