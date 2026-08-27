package com.ckgame.world.buildings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 建筑系统：管理省份建筑与升级。 */
public final class BuildingSystem {
    private final Map<Integer, List<CountyBuilding>> buildings = new HashMap<>();

    public List<CountyBuilding> getBuildings(int countyId) {
        return buildings.getOrDefault(countyId, List.of());
    }

    public CountyBuilding getBuilding(int countyId, BuildingKind kind) {
        for (CountyBuilding b : getBuildings(countyId)) {
            if (b.kind == kind) return b;
        }
        return null;
    }

    public CountyBuilding addBuilding(int countyId, BuildingKind kind) {
        CountyBuilding b = new CountyBuilding(kind);
        buildings.computeIfAbsent(countyId, k -> new ArrayList<>()).add(b);
        return b;
    }

    public boolean upgrade(int countyId, BuildingKind kind) {
        CountyBuilding b = getBuilding(countyId, kind);
        if (b != null && b.canUpgrade()) {
            b.level += 1;
            return true;
        }
        return false;
    }

    /** 获取省份所有建筑的效果总和。 */
    public Map<String, Double> getEffects(int countyId) {
        Map<String, Double> effects = new HashMap<>();
        effects.put("tax_income", 0.0);
        effects.put("levy_max", 0.0);
        effects.put("garrison", 0.0);
        effects.put("defense", 0.0);
        effects.put("stability", 0.0);
        effects.put("vision", 0.0);
        effects.put("cavalry_bonus", 0.0);
        effects.put("production", 0.0);

        for (CountyBuilding b : getBuildings(countyId)) {
            switch (b.kind) {
                case FARM -> add(effects, "tax_income", b.kind.effectValue(b.level));
                case BARRACKS -> add(effects, "levy_max", b.kind.effectValue(b.level));
                case WALLS -> {
                    add(effects, "garrison", b.kind.effectValue(b.level));
                    add(effects, "defense", b.kind.effectValue(b.level) * 0.5);
                }
                case MARKET -> add(effects, "tax_income", b.kind.effectValue(b.level));
                case CHURCH -> add(effects, "stability", b.kind.effectValue(b.level));
                case WATCH_TOWER -> add(effects, "vision", b.kind.effectValue(b.level));
                case STABLE -> add(effects, "cavalry_bonus", b.kind.effectValue(b.level));
                case WORKSHOP -> add(effects, "production", b.kind.effectValue(b.level));
            }
        }
        return effects;
    }

    private static void add(Map<String, Double> m, String key, double v) {
        m.put(key, m.get(key) + v);
    }

    /** 序列化建筑状态（供存档使用）。 */
    public Map<String, Object> saveState() {
        Map<String, Object> s = new HashMap<>();
        for (var e : buildings.entrySet()) {
            List<Map<String, Object>> bl = new ArrayList<>();
            for (CountyBuilding b : e.getValue()) {
                Map<String, Object> bm = new HashMap<>();
                bm.put("kind", b.kind.name());
                bm.put("level", b.level);
                bl.add(bm);
            }
            s.put(String.valueOf(e.getKey()), bl);
        }
        return s;
    }

    /** 从存档恢复建筑状态。 */
    @SuppressWarnings("unchecked")
    public void loadState(Map<String, Object> s) {
        buildings.clear();
        for (var e : s.entrySet()) {
            int countyId = Integer.parseInt(e.getKey());
            List<Map<String, Object>> bl = (List<Map<String, Object>>) e.getValue();
            List<CountyBuilding> list = new ArrayList<>();
            for (Map<String, Object> bm : bl) {
                BuildingKind kind = BuildingKind.valueOf((String) bm.get("kind"));
                int level = ((Number) bm.get("level")).intValue();
                list.add(new CountyBuilding(kind, level));
            }
            buildings.put(countyId, list);
        }
    }
}
