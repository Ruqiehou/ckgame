package com.ckgame.politics;

import com.ckgame.world.World;

import java.util.HashMap;
import java.util.Map;

/**
 * 内阁注册表，按统治者管理内阁。
 * 对应 Python 的 CouncilRegistry。
 */
public final class CouncilManager {
    private final World world;
    private final Map<Integer, Council> byRuler = new HashMap<>();

    public CouncilManager(World world) {
        this.world = world;
    }

    public World world() {
        return world;
    }

    /** 获取或创建指定统治者的内阁。 */
    public Council councilFor(int ruler) {
        return byRuler.computeIfAbsent(ruler, k -> Council.empty(ruler));
    }

    /** 获取指定统治者的内阁；若不存在返回 null。 */
    public Council get(int ruler) {
        return byRuler.get(ruler);
    }
}
