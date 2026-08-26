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
}
