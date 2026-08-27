package com.ckgame.politics;

import com.ckgame.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    /** 返回所有内阁的映射（供存档使用）。 */
    public Map<Integer, Council> allCouncils() {
        return byRuler;
    }

    /** 序列化内阁状态（供存档使用）。 */
    public Map<String, Object> saveState() {
        Map<String, Object> s = new HashMap<>();
        Map<String, Object> cm = new HashMap<>();
        for (var e : byRuler.entrySet()) {
            Council c = e.getValue();
            Map<String, Object> m = new HashMap<>();
            m.put("chancellor", c.chancellor);
            m.put("marshal", c.marshal);
            m.put("steward", c.steward);
            m.put("spymaster", c.spymaster);
            m.put("chaplain", c.chaplain);
            Map<String, String> tasks = new HashMap<>();
            for (var te : c.tasks.entrySet()) {
                tasks.put(te.getKey().name(), te.getValue().name());
            }
            m.put("tasks", tasks);
            cm.put(String.valueOf(e.getKey()), m);
        }
        s.put("councils", cm);
        return s;
    }

    /** 从存档恢复内阁状态。 */
    @SuppressWarnings("unchecked")
    public void loadState(Map<String, Object> s) {
        byRuler.clear();
        Map<String, Object> cm = (Map<String, Object>) s.get("councils");
        if (cm != null) {
            for (var e : cm.entrySet()) {
                int ruler = Integer.parseInt(e.getKey());
                Map<String, Object> m = (Map<String, Object>) e.getValue();
                Council c = Council.empty(ruler);
                c.chancellor = ((Number) m.get("chancellor")).intValue();
                c.marshal = ((Number) m.get("marshal")).intValue();
                c.steward = ((Number) m.get("steward")).intValue();
                c.spymaster = ((Number) m.get("spymaster")).intValue();
                c.chaplain = ((Number) m.get("chaplain")).intValue();
                Map<String, String> tasks = (Map<String, String>) m.get("tasks");
                if (tasks != null) {
                    for (var te : tasks.entrySet()) {
                        c.tasks.put(CouncilPosition.valueOf(te.getKey()),
                                CouncilTask.valueOf(te.getValue()));
                    }
                }
                byRuler.put(ruler, c);
            }
        }
    }
}
