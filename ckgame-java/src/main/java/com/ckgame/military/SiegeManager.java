package com.ckgame.military;

import com.ckgame.core.calendar.GameDate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 围城管理器。
 */
public final class SiegeManager {
    public final Map<Integer, Siege> sieges = new HashMap<>();
    public int nextId = 1;

    /**
     * 在指定省份开始围城；若该省份已有活跃围城则返回其 id。
     */
    public int start(int county, int attackerArmy, int attacker, int defender,
                     int fortLevel, int garrison, GameDate date) {
        for (Siege s : sieges.values()) {
            if (s.active && s.county == county) {
                return s.id;
            }
        }
        int sid = nextId++;
        Siege siege = new Siege(sid, county, attackerArmy, attacker, defender);
        siege.fortLevel = fortLevel;
        siege.garrison = garrison;
        siege.started = date;
        sieges.put(sid, siege);
        return sid;
    }

    /** 查询指定省份的活跃围城。 */
    public Siege activeAt(int county) {
        for (Siege s : sieges.values()) {
            if (s.active && s.county == county) {
                return s;
            }
        }
        return null;
    }

    /** 所有活跃围城。 */
    public List<Siege> activeSieges() {
        List<Siege> out = new ArrayList<>();
        for (Siege s : sieges.values()) {
            if (s.active) {
                out.add(s);
            }
        }
        return out;
    }

    /**
     * 每日推进所有围城。
     *
     * @param armyMen      军队 id -> 当前兵力
     * @param armyMartial  军队 id -> 指挥官军略
     * @param armyLocation 军队 id -> 所在省份
     * @return 当日围城事件列表
     */
    public List<SiegeEvent> tickDay(Map<Integer, Integer> armyMen,
                                    Map<Integer, Integer> armyMartial,
                                    Map<Integer, Integer> armyLocation) {
        List<SiegeEvent> events = new ArrayList<>();
        List<Integer> completed = new ArrayList<>();
        for (Siege s : sieges.values()) {
            if (!s.active) {
                continue;
            }
            Integer loc = armyLocation.get(s.attackerArmy);
            int men = armyMen.getOrDefault(s.attackerArmy, 0);
            if (loc == null || loc != s.county || men <= 0) {
                s.active = false;
                events.add(new SiegeEvent("lifted", s.id, s.county, "攻城部队离开或溃散"));
                continue;
            }
            int martial = armyMartial.getOrDefault(s.attackerArmy, 8);
            s.dailyTick(men, martial);
            events.add(new SiegeEvent("progressed", s.id, s.county, s.progress, s.requiredProgress()));
            if (s.isComplete()) {
                completed.add(s.id);
            }
        }
        for (int sid : completed) {
            Siege s = sieges.get(sid);
            if (s != null) {
                s.active = false;
                events.add(new SiegeEvent("captured", sid, s.county, s.attacker, s.defender));
            }
        }
        return events;
    }
}
