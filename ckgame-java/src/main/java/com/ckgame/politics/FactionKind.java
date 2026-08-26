package com.ckgame.politics;

/**
 * 派系类型。
 */
public enum FactionKind {
    INDEPENDENCE("独立派系", "要求独立，建立自己的政权"),
    LOWER_CROWN_AUTHORITY("降低王权派系", "要求限制王权，扩大贵族议会权力"),
    CLAIMANT("拥立派系", "要求废黜当前君主，拥立新君"),
    LIBERTY("自由派系", "要求恢复自由权利，减免赋税"),
    POPULAR("民变派系", "要求改革苛政，减轻百姓负担");

    private final String nameZh;
    private final String ultimatumText;

    FactionKind(String nameZh, String ultimatumText) {
        this.nameZh = nameZh;
        this.ultimatumText = ultimatumText;
    }

    public String nameZh() {
        return nameZh;
    }

    public String ultimatumText() {
        return ultimatumText;
    }
}
