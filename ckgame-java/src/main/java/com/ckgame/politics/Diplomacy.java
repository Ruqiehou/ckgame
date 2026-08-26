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

    private record RelationKey(int a, int b) {
    }
}
