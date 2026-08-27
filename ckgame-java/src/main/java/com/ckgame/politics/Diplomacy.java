package com.ckgame.politics;

import com.ckgame.core.Constants;
import com.ckgame.core.balance.Balance;
import com.ckgame.core.calendar.GameDate;
import com.ckgame.world.Character;
import com.ckgame.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 外交系统最小实现。
 * <p>
 * 对应 Python 的 DiplomacySystem，提供 AI 导演所需的外交关系、条约、宣称与战争疲劳接口。
 */
public final class Diplomacy {
    private final World world;
    private final Map<RelationKey, DiplomacyFlags> relations = new HashMap<>();
    private final List<Treaty> treaties = new ArrayList<>();
    private final Map<Integer, List<Claim>> claims = new HashMap<>();
    private final Map<RelationKey, Integer> truceUntil = new HashMap<>();
    private final Map<Integer, Double> warExhaustion = new HashMap<>();

    public Diplomacy() {
        this(null);
    }

    public Diplomacy(World world) {
        this.world = world;
    }

    /** 构造时持有的世界引用（可为 null）。 */
    public World world() {
        return world;
    }

    private static RelationKey pairKey(int a, int b) {
        return a <= b ? new RelationKey(a, b) : new RelationKey(b, a);
    }

    /** 获取两个统治者之间的关系标志（只读安全：不存在时返回新对象）。 */
    public DiplomacyFlags flags(int a, int b) {
        return relations.getOrDefault(pairKey(a, b), new DiplomacyFlags());
    }

    /** 获取两个统治者之间的关系标志（不存在时创建并存储）。 */
    public DiplomacyFlags flagsMut(int a, int b) {
        RelationKey key = pairKey(a, b);
        DiplomacyFlags f = relations.get(key);
        if (f == null) {
            f = new DiplomacyFlags();
            relations.put(key, f);
        }
        return f;
    }

    /** 设置双方为宿敌。 */
    public void setRival(int a, int b) {
        flagsMut(a, b).rival = true;
    }

    /** 缔结同盟条约。 */
    public void formAlliance(int a, int b, GameDate date) {
        DiplomacyFlags f = flagsMut(a, b);
        f.allied = true;
        f.nonAggression = true;
        treaties.add(new Treaty(a, b, TreatyKind.ALLIANCE, date, date.year() + 50));
    }

    /** 添加任意条约到内部列表。 */
    public void addTreaty(Treaty treaty) {
        treaties.add(treaty);
    }

    /** 设置双方交战状态；开战时自动解除同盟与互不侵犯。 */
    public void setAtWar(int a, int b, boolean atWar) {
        DiplomacyFlags f = flagsMut(a, b);
        f.atWar = atWar;
        if (atWar) {
            f.allied = false;
            f.nonAggression = false;
        }
    }

    /** 是否结盟。 */
    public boolean areAllied(int a, int b) {
        return flags(a, b).allied;
    }

    /** 返回某统治者的所有同盟对象。 */
    public List<Integer> alliesOf(int who) {
        List<Integer> out = new ArrayList<>();
        for (Treaty t : treaties) {
            if (t.kind != TreatyKind.ALLIANCE) {
                continue;
            }
            if (t.a == who) {
                out.add(t.b);
            } else if (t.b == who) {
                out.add(t.a);
            }
        }
        return out;
    }

    /** 添加战争疲劳（默认值为宣战时常量）。 */
    public void addWarExhaustion(int who) {
        addWarExhaustion(who, Balance.WAR_EXHAUSTION_ON_DECLARE);
    }

    public void addWarExhaustion(int who, double amount) {
        warExhaustion.put(who, warExhaustion.getOrDefault(who, 0.0) + amount);
    }

    /** 检查某对统治者当年是否可以宣战。 */
    public boolean canDeclareWar(int a, int b, int year) {
        if (a == b) {
            return false;
        }
        if (warExhaustion.getOrDefault(a, 0.0) >= Balance.WAR_EXHAUSTION_DECLARE_BLOCK) {
            return false;
        }
        DiplomacyFlags f = flags(a, b);
        if (f.blocksWar() || f.atWar || hasTruce(a, b, year)) {
            return false;
        }
        return true;
    }

    /** 是否存在有效停战。 */
    public boolean hasTruce(int a, int b, int year) {
        return truceUntil.getOrDefault(pairKey(a, b), 0) > year;
    }

    /** 设置停战到期年份。 */
    public void setTruce(int a, int b, int untilYear) {
        truceUntil.put(pairKey(a, b), untilYear);
        flagsMut(a, b).atWar = false;
    }

    /** 添加宣称（指定 county）。 */
    public void addClaim(int claimant, int title, Integer county, int strength) {
        claims.computeIfAbsent(claimant, k -> new ArrayList<>())
                .add(new Claim(claimant, title, county, strength));
    }

    /** 添加宣称（不指定 county）。 */
    public void addClaim(int claimant, int title, int strength) {
        addClaim(claimant, title, Constants.NONE_ID, strength);
    }

    /** 返回某角色的全部宣称。 */
    public List<Claim> claimsOf(int who) {
        return claims.getOrDefault(who, Collections.emptyList());
    }

    /** 返回战争疲劳映射（只读视图）。 */
    public Map<Integer, Double> warExhaustion() {
        return Collections.unmodifiableMap(warExhaustion);
    }

    /** 返回条约列表（只读视图）。 */
    public List<Treaty> treaties() {
        return Collections.unmodifiableList(treaties);
    }

    /** 每月衰减战争疲劳，exceptIds 中的角色不衰减。 */
    public void tickWarExhaustion(java.util.Collection<Integer> exceptIds) {
        Set<Integer> skip = exceptIds != null ? new java.util.HashSet<>(exceptIds) : Collections.emptySet();
        List<Integer> keys = new ArrayList<>(warExhaustion.keySet());
        for (int who : keys) {
            if (skip.contains(who)) {
                continue;
            }
            double v = warExhaustion.getOrDefault(who, 0.0) - Balance.WAR_EXHAUSTION_DECAY;
            if (v <= 0.0) {
                warExhaustion.remove(who);
            } else {
                warExhaustion.put(who, v);
            }
        }
    }

    /** 清理过期条约并返回日志行。 */
    public List<String> expireTreaties(int year, World world) {
        List<String> lines = new ArrayList<>();
        Iterator<Treaty> it = treaties.iterator();
        while (it.hasNext()) {
            Treaty t = it.next();
            if (t.expiresYear > year) {
                continue;
            }
            it.remove();
            Character a = world.character(t.a);
            Character b = world.character(t.b);
            String an = a != null ? a.name : "?";
            String bn = b != null ? b.name : "?";
            if (t.kind == TreatyKind.TRUCE) {
                lines.add(an + " 与 " + bn + " 的停战到期");
            } else if (t.kind == TreatyKind.ALLIANCE) {
                DiplomacyFlags f = flagsMut(t.a, t.b);
                f.allied = false;
                lines.add(an + " 与 " + bn + " 的同盟到期");
            }
        }
        return lines;
    }

    /** 计算赠礼带来的好感度收益。 */
    public static int giftOpinionGain(double amount) {
        return (int) Math.max(1.0, Math.min(30.0, amount / 5.0));
    }

    /** 序列化外交状态（供存档使用）。 */
    public Map<String, Object> saveState() {
        Map<String, Object> s = new HashMap<>();
        List<Map<String, Object>> rels = new ArrayList<>();
        for (var e : relations.entrySet()) {
            Map<String, Object> r = new HashMap<>();
            r.put("a", e.getKey().a());
            r.put("b", e.getKey().b());
            DiplomacyFlags f = e.getValue();
            r.put("allied", f.allied);
            r.put("atWar", f.atWar);
            r.put("nonAggression", f.nonAggression);
            r.put("rival", f.rival);
            r.put("marriagePact", f.marriagePact);
            r.put("vassalage", f.vassalage);
            r.put("tradeAgreement", f.tradeAgreement);
            r.put("intelligenceSharing", f.intelligenceSharing);
            rels.add(r);
        }
        s.put("relations", rels);
        List<Map<String, Object>> ts = new ArrayList<>();
        for (Treaty t : treaties) {
            Map<String, Object> tm = new HashMap<>();
            tm.put("a", t.a);
            tm.put("b", t.b);
            tm.put("kind", t.kind.name());
            tm.put("startYear", t.start.year());
            tm.put("startMonth", t.start.month());
            tm.put("startDay", t.start.day());
            tm.put("expiresYear", t.expiresYear);
            ts.add(tm);
        }
        s.put("treaties", ts);
        Map<String, Object> cl = new HashMap<>();
        for (var e : claims.entrySet()) {
            List<Map<String, Object>> clList = new ArrayList<>();
            for (Claim c : e.getValue()) {
                Map<String, Object> cm = new HashMap<>();
                cm.put("claimant", c.claimant);
                cm.put("title", c.title);
                cm.put("county", c.county);
                cm.put("pressed", c.pressed);
                cm.put("strength", c.strength);
                clList.add(cm);
            }
            cl.put(String.valueOf(e.getKey()), clList);
        }
        s.put("claims", cl);
        Map<String, Integer> tr = new HashMap<>();
        for (var e : truceUntil.entrySet()) {
            tr.put(e.getKey().a() + "|" + e.getKey().b(), e.getValue());
        }
        s.put("truceUntil", tr);
        s.put("warExhaustion", new HashMap<>(warExhaustion));
        return s;
    }

    /** 从存档恢复外交状态。 */
    @SuppressWarnings("unchecked")
    public void loadState(Map<String, Object> s) {
        relations.clear();
        treaties.clear();
        claims.clear();
        truceUntil.clear();
        warExhaustion.clear();
        List<Map<String, Object>> rels = (List<Map<String, Object>>) s.get("relations");
        if (rels != null) {
            for (Map<String, Object> r : rels) {
                int a = ((Number) r.get("a")).intValue();
                int b = ((Number) r.get("b")).intValue();
                DiplomacyFlags f = flagsMut(a, b);
                f.allied = Boolean.TRUE.equals(r.get("allied"));
                f.atWar = Boolean.TRUE.equals(r.get("atWar"));
                f.nonAggression = Boolean.TRUE.equals(r.get("nonAggression"));
                f.rival = Boolean.TRUE.equals(r.get("rival"));
                f.marriagePact = Boolean.TRUE.equals(r.get("marriagePact"));
                f.vassalage = Boolean.TRUE.equals(r.get("vassalage"));
                f.tradeAgreement = Boolean.TRUE.equals(r.get("tradeAgreement"));
                f.intelligenceSharing = Boolean.TRUE.equals(r.get("intelligenceSharing"));
            }
        }
        List<Map<String, Object>> ts = (List<Map<String, Object>>) s.get("treaties");
        if (ts != null) {
            for (Map<String, Object> tm : ts) {
                int a = ((Number) tm.get("a")).intValue();
                int b = ((Number) tm.get("b")).intValue();
                TreatyKind kind = TreatyKind.valueOf((String) tm.get("kind"));
                GameDate start = new GameDate(
                        ((Number) tm.get("startYear")).intValue(),
                        ((Number) tm.get("startMonth")).intValue(),
                        ((Number) tm.get("startDay")).intValue());
                int exp = ((Number) tm.get("expiresYear")).intValue();
                treaties.add(new Treaty(a, b, kind, start, exp));
            }
        }
        Map<String, Object> cl = (Map<String, Object>) s.get("claims");
        if (cl != null) {
            for (var e : cl.entrySet()) {
                int claimant = Integer.parseInt(e.getKey());
                List<Map<String, Object>> clList = (List<Map<String, Object>>) e.getValue();
                for (Map<String, Object> cm : clList) {
                    int title = ((Number) cm.get("title")).intValue();
                    Object countyObj = cm.get("county");
                    Integer county = countyObj instanceof Number ? ((Number) countyObj).intValue() : null;
                    int strength = ((Number) cm.get("strength")).intValue();
                    addClaim(claimant, title, county, strength);
                }
            }
        }
        Map<String, Object> tr = (Map<String, Object>) s.get("truceUntil");
        if (tr != null) {
            for (var e : tr.entrySet()) {
                String[] parts = e.getKey().split("\\|");
                int a = Integer.parseInt(parts[0]);
                int b = Integer.parseInt(parts[1]);
                truceUntil.put(pairKey(a, b), ((Number) e.getValue()).intValue());
            }
        }
        Map<String, Object> we = (Map<String, Object>) s.get("warExhaustion");
        if (we != null) {
            for (var e : we.entrySet()) {
                warExhaustion.put(Integer.parseInt(e.getKey()), ((Number) e.getValue()).doubleValue());
            }
        }
    }

    private record RelationKey(int a, int b) {
    }
}
