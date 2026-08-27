package com.ckgame.politics;

import com.ckgame.core.calendar.GameDate;
import com.ckgame.world.World;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleSupplier;

/**
 * 阴谋管理器。
 * 对应 Python 的 SchemeSystem。
 */
public final class SchemeManager {
    private final World world;
    private final Map<Integer, Scheme> schemes = new HashMap<>();
    private int nextId = 1;

    public SchemeManager(World world) {
        this.world = world;
    }

    public World world() {
        return world;
    }

    /** 返回当前所有阴谋的只读视图（供 AI 等外部模块遍历）。 */
    public Collection<Scheme> schemes() {
        return schemes.values();
    }

    /** 返回阴谋映射（只读视图）。 */
    public Map<Integer, Scheme> schemeMap() {
        return Collections.unmodifiableMap(schemes);
    }

    /** 开启一个阴谋并返回其 ID。 */
    public int start(SchemeKind kind, int owner, int target, GameDate date) {
        int sid = nextId;
        nextId++;
        Scheme s = new Scheme(sid, kind, owner, target);
        s.secrecy = kind.secrecyBase();
        s.started = date;
        schemes.put(sid, s);
        return sid;
    }

    /**
     * 阴谋月度推进。
     *
     * @param intrigueOf 角色 ID 到谋略值的映射
     * @param rngRoll    返回 0~1 随机数的函数
     */
    public List<SchemeOutcome> monthlyTick(Map<Integer, Integer> intrigueOf, DoubleSupplier rngRoll) {
        List<SchemeOutcome> outcomes = new ArrayList<>();
        List<Integer> completed = new ArrayList<>();
        List<Integer> exposed = new ArrayList<>();
        for (Scheme s : schemes.values()) {
            double intrigue = intrigueOf.getOrDefault(s.owner, 8);
            double targetIntrigue = intrigueOf.getOrDefault(s.target, 8);
            double gain = s.kind.baseMonthlyProgress()
                    + intrigue * 0.5
                    + s.agents.size() * 2.0
                    - targetIntrigue * 0.2;
            s.progress = Math.min(100.0, s.progress + Math.max(0.5, gain));
            double discovery = Math.max(0.01, Math.min(0.4, (100.0 - s.secrecy) * 0.01 + targetIntrigue * 0.005));
            if (rngRoll.getAsDouble() < discovery) {
                s.exposed = true;
                exposed.add(s.id);
            }
            SchemeOutcome progressed = new SchemeOutcome("progressed", s.id);
            progressed.progress = s.progress;
            progressed.owner = s.owner;
            progressed.target = s.target;
            outcomes.add(progressed);
            if (s.isComplete()) {
                completed.add(s.id);
            }
        }
        for (int sid : exposed) {
            Scheme s = schemes.get(sid);
            if (s != null) {
                SchemeOutcome o = new SchemeOutcome("exposed", sid);
                o.owner = s.owner;
                o.target = s.target;
                o.schemeKind = s.kind;
                outcomes.add(o);
            }
        }
        for (int sid : completed) {
            Scheme s = schemes.remove(sid);
            if (s != null) {
                SchemeOutcome o = new SchemeOutcome("success", sid);
                o.schemeKind = s.kind;
                o.owner = s.owner;
                o.target = s.target;
                outcomes.add(o);
            }
        }
        return outcomes;
    }

    /** 序列化阴谋状态（供存档使用）。 */
    public Map<String, Object> saveState() {
        Map<String, Object> s = new HashMap<>();
        s.put("nextId", nextId);
        List<Map<String, Object>> sl = new ArrayList<>();
        for (Scheme sc : schemes.values()) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", sc.id);
            m.put("kind", sc.kind.name());
            m.put("owner", sc.owner);
            m.put("target", sc.target);
            m.put("progress", sc.progress);
            m.put("secrecy", sc.secrecy);
            m.put("agents", new ArrayList<>(sc.agents));
            m.put("started", sc.started != null ? List.of(sc.started.year(), sc.started.month(), sc.started.day()) : null);
            m.put("exposed", sc.exposed);
            sl.add(m);
        }
        s.put("schemes", sl);
        return s;
    }

    /** 从存档恢复阴谋状态。 */
    @SuppressWarnings("unchecked")
    public void loadState(Map<String, Object> s) {
        schemes.clear();
        nextId = ((Number) s.getOrDefault("nextId", 1)).intValue();
        List<Map<String, Object>> sl = (List<Map<String, Object>>) s.get("schemes");
        if (sl != null) {
            for (Map<String, Object> m : sl) {
                int id = ((Number) m.get("id")).intValue();
                SchemeKind kind = SchemeKind.valueOf((String) m.get("kind"));
                Scheme sc = new Scheme(id, kind, ((Number) m.get("owner")).intValue(),
                        ((Number) m.get("target")).intValue());
                sc.progress = ((Number) m.get("progress")).doubleValue();
                sc.secrecy = ((Number) m.get("secrecy")).doubleValue();
                sc.agents.addAll((List<Integer>) m.get("agents"));
                List<Object> sd = (List<Object>) m.get("started");
                if (sd != null) {
                    sc.started = new GameDate(((Number) sd.get(0)).intValue(),
                            ((Number) sd.get(1)).intValue(), ((Number) sd.get(2)).intValue());
                }
                sc.exposed = Boolean.TRUE.equals(m.get("exposed"));
                schemes.put(id, sc);
            }
        }
    }
}
