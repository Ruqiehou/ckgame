package com.ckgame.politics;

/**
 * 内阁任务。
 */
public enum CouncilTask {
    DOMESTIC_RELATIONS("内政外交"),
    FABRICATE_CLAIM("伪造宣称"),
    TRAIN_COMMANDERS("训练将领"),
    INCREASE_CONTROL("强化控制"),
    COLLECT_TAXES("催收税赋"),
    DEVELOP_COUNTY("发展领地"),
    DISRUPT_SCHEMES("破坏阴谋"),
    SUPPORT_MURDER("协助密谋"),
    CONVERT_FAITH("传播信仰"),
    FABRICATE_HOOK("神权施压"),
    RECRUIT_KNIGHTS("招募骑士"),
    IMPROVE_DIPLOMACY("改善外交"),
    SPREAD_CULTURE("传播文化"),
    ESTABLISH_TRADE("建立商路"),
    MAINTAIN_BUILDINGS("维护建筑"),
    TRAIN_TROOPS("训练部队"),
    GATHER_INTEL("收集情报"),
    PROMOTE_CULTURE("推广文化");

    private final String nameZh;

    CouncilTask(String nameZh) {
        this.nameZh = nameZh;
    }

    public String nameZh() {
        return nameZh;
    }
}
