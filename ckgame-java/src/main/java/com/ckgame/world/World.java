package com.ckgame.world;

import com.ckgame.core.Constants;
import com.ckgame.core.Gender;
import com.ckgame.core.TitleTier;
import com.ckgame.core.calendar.GameDate;
import com.ckgame.core.stats.AttributeSet;
import com.ckgame.core.traits.Trait;
import com.ckgame.core.traits.Traits;
import com.ckgame.politics.Laws.HeirCandidate;
import com.ckgame.politics.Laws.RealmLaw;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;

/** 世界状态：所有角色、王朝、头衔、地图与贸易的经济逻辑。 */
public final class World {
    public GameDate date;
    public int tick = 0;
    public final Map<Integer, Character> characters = new LinkedHashMap<>();
    public final Map<Integer, Dynasty> dynasties = new LinkedHashMap<>();
    public final Map<Integer, Title> titles = new LinkedHashMap<>();
    public final MapGraph map = new MapGraph();
    public final List<Trait> traits = Traits.builtin();
    public final List<String> log = new ArrayList<>();
    public int nextChar = 1;
    public int nextDynasty = 1;
    public int nextTitle = 1;
    public int nextCounty = 1;

    public List<TradeRoute> tradeRoutes = new ArrayList<>();
    public final Map<String, Double> exchangeRates = new HashMap<>();
    public final List<TradeEvent> tradeEvents = new ArrayList<>();

    private final Random rng;

    public World() {
        this(null, new Random());
    }

    public World(GameDate dateOrNull) {
        this(dateOrNull, new Random());
    }

    public World(long seed) {
        this(null, new Random(seed));
    }

    public World(GameDate dateOrNull, long seed) {
        this(dateOrNull, new Random(seed));
    }

    private World(GameDate dateOrNull, Random rng) {
        this.date = dateOrNull != null ? dateOrNull : new GameDate(1066, 10, 14);
        this.rng = rng;
    }

    /** 返回内部随机数生成器，供模拟器统一使用。 */
    public Random rng() {
        return rng;
    }

    public double nextDouble() {
        return rng.nextDouble();
    }

    public int nextInt(int bound) {
        return rng.nextInt(bound);
    }

    public int nextInt(int origin, int bound) {
        return rng.nextInt(origin, bound);
    }

    public void pushLog(String msg) {
        log.add("[" + date + "] " + msg);
        if (log.size() > 500) {
            log.subList(0, 100).clear();
        }
    }

    // ---------- 创建 ----------
    public int createDynasty(String name) {
        int did = nextDynasty++;
        dynasties.put(did, Dynasty.newDynasty(did, name, Constants.NONE_ID));
        return did;
    }

    public int createCharacter(String name, int dynasty, Gender gender, GameDate birth) {
        int cid = nextChar++;
        Character c = new Character(cid, name, dynasty, gender, birth);
        c.baseAttrs = new AttributeSet(
                rng.nextInt(4, 14),
                rng.nextInt(4, 14),
                rng.nextInt(4, 14),
                rng.nextInt(4, 14),
                rng.nextInt(4, 14),
                rng.nextInt(4, 14)
        );
        if (!traits.isEmpty()) {
            int n = rng.nextInt(1, 3);
            for (int i = 0; i < n; i++) {
                int t = traits.get(rng.nextInt(traits.size())).id();
                if (!c.traits.contains(t)) {
                    c.traits.add(t);
                }
            }
        }
        Dynasty d = dynasties.get(dynasty);
        if (d != null) {
            d.addMember(cid);
            if (d.head == Constants.NONE_ID) {
                d.head = cid;
                d.founder = cid;
            }
        }
        characters.put(cid, c);
        return cid;
    }

    public int createTitle(String name, TitleTier tier) {
        int tid = nextTitle++;
        titles.put(tid, Title.newTitle(tid, name, tier));
        return tid;
    }

    public int createCounty(String name, Terrain terrain) {
        int cid = nextCounty++;
        map.insert(County.newCounty(cid, name, terrain));
        return cid;
    }

    // ---------- 查询 ----------
    public Character character(int cid) {
        return characters.get(cid);
    }

    public Title title(int tid) {
        return titles.get(tid);
    }

    public List<Character> aliveCharacters() {
        List<Character> out = new ArrayList<>();
        for (Character c : characters.values()) {
            if (c.isAlive()) out.add(c);
        }
        return out;
    }

    public List<Character> rulers() {
        List<Character> out = new ArrayList<>();
        for (Character c : characters.values()) {
            if (c.isAlive() && c.isRuler) out.add(c);
        }
        return out;
    }

    public AttributeSet traitBonus(List<Integer> traitIds) {
        AttributeSet total = AttributeSet.zero();
        Map<Integer, Trait> byId = new HashMap<>();
        for (Trait t : traits) byId.put(t.id(), t);
        for (int tid : traitIds) {
            Trait t = byId.get(tid);
            if (t != null) total = total.add(t.attrBonus());
        }
        return total;
    }

    public AttributeSet effectiveAttrs(int cid) {
        Character c = character(cid);
        if (c == null) return null;
        return c.effectiveAttrs(traitBonus(c.traits));
    }

    // ---------- 关系与封建 ----------
    public void setParents(int child, int father, int mother) {
        Character c = character(child);
        if (c == null) return;
        c.father = father;
        c.mother = mother;
        if (father != Constants.NONE_ID) {
            Character p = character(father);
            if (p != null && !p.children.contains(child)) p.children.add(child);
        }
        if (mother != Constants.NONE_ID) {
            Character p = character(mother);
            if (p != null && !p.children.contains(child)) p.children.add(child);
        }
    }

    public boolean marry(int a, int b) {
        Character ca = character(a);
        Character cb = character(b);
        if (ca == null || cb == null || !ca.isAlive() || !cb.isAlive()) return false;
        if (ca.gender == cb.gender) return false;
        if (!ca.spouses.contains(b)) ca.spouses.add(b);
        if (!cb.spouses.contains(a)) cb.spouses.add(a);
        pushLog(ca.name + " 与 " + cb.name + " 成婚");
        return true;
    }

    public boolean grantTitle(int titleId, int holder) {
        Title t = title(titleId);
        Character h = character(holder);
        if (t == null || h == null) return false;
        int old = t.holder;
        if (old != Constants.NONE_ID && old != holder) {
            Character oldC = character(old);
            if (oldC != null) {
                oldC.heldTitles.remove(Integer.valueOf(titleId));
                if (oldC.primaryTitle == titleId) {
                    oldC.primaryTitle = oldC.heldTitles.isEmpty() ? Constants.NONE_ID : oldC.heldTitles.get(0);
                    oldC.isRuler = oldC.primaryTitle != Constants.NONE_ID;
                }
            }
        }
        t.setHolder(holder);
        if (!h.heldTitles.contains(titleId)) h.heldTitles.add(titleId);
        h.isRuler = true;
        boolean shouldUpgrade = h.primaryTitle == Constants.NONE_ID;
        if (!shouldUpgrade) {
            Title cur = title(h.primaryTitle);
            shouldUpgrade = cur == null || t.tier.compareTo(cur.tier) > 0;
        }
        if (shouldUpgrade) h.primaryTitle = titleId;
        for (int cid : t.counties) {
            County county = map.get(cid);
            if (county != null) {
                county.holder = holder;
                county.ownerTitle = titleId;
            }
        }
        pushLog(h.name + " 获得头衔「" + t.name + "」");
        return true;
    }

    public boolean setVassal(int vassalTitle, int liegeTitle) {
        Title v = title(vassalTitle);
        Title l = title(liegeTitle);
        if (v == null || l == null) return false;
        if (v.deFactoLiege != Constants.NONE_ID && v.deFactoLiege != liegeTitle) {
            Title oldLiege = title(v.deFactoLiege);
            if (oldLiege != null) oldLiege.deFactoVassals.remove(Integer.valueOf(vassalTitle));
        }
        v.deFactoLiege = liegeTitle;
        if (!l.deFactoVassals.contains(vassalTitle)) l.deFactoVassals.add(vassalTitle);
        return true;
    }

    public void clearVassalLink(int titleId) {
        Title t = title(titleId);
        if (t == null) return;
        if (t.deFactoLiege != Constants.NONE_ID) {
            Title oldLiege = title(t.deFactoLiege);
            if (oldLiege != null) oldLiege.deFactoVassals.remove(Integer.valueOf(titleId));
        }
        t.deFactoLiege = Constants.NONE_ID;
    }

    public boolean attachCountyToTitle(int countyId, int titleId) {
        Title t = title(titleId);
        County c = map.get(countyId);
        if (t == null || c == null) return false;
        if (!t.counties.contains(countyId)) t.counties.add(countyId);
        if (t.capital == Constants.NONE_ID) t.capital = countyId;
        c.ownerTitle = titleId;
        c.holder = t.holder;
        return true;
    }

    private Map<Integer, Trait> traitById() {
        Map<Integer, Trait> byId = new HashMap<>();
        for (Trait t : traits) byId.put(t.id(), t);
        return byId;
    }

    public int opinion(int from, int to) {
        if (from == to) return 100;
        Character a = character(from);
        Character b = character(to);
        if (a == null || b == null) return 0;
        int op = 0;
        if (a.dynasty != Constants.NONE_ID && a.dynasty == b.dynasty) op += 15;
        op += (a.culture == b.culture) ? 10 : -5;
        op += (a.faith == b.faith) ? 10 : -20;
        if (a.father == to || a.mother == to || b.father == from || b.mother == from) op += 30;
        if (a.spouses.contains(to)) op += 25;
        if (a.children.contains(to) || b.children.contains(from)) op += 20;
        Map<Integer, Trait> byId = traitById();
        for (int tid : a.traits) {
            Trait t = byId.get(tid);
            if (t != null) op += t.opinionSelf();
        }
        for (int tid : b.traits) {
            Trait t = byId.get(tid);
            if (t != null) op += t.opinionOthers() / 2;
        }
        AttributeSet attrs = effectiveAttrs(from);
        if (attrs != null) op += (attrs.diplomacy() - 8) * 2;
        op += a.opinionCache.getOrDefault(to, 0);
        return Math.max(-100, Math.min(100, op));
    }

    public void modifyOpinion(int from, int to, int delta) {
        Character c = character(from);
        if (c == null) return;
        int cur = c.opinionCache.getOrDefault(to, 0) + delta;
        cur = Math.max(-100, Math.min(100, cur));
        if (cur == 0) c.opinionCache.remove(to);
        else c.opinionCache.put(to, cur);
        if (c.opinionCache.size() > 100) {
            Map<Integer, Integer> trimmed = new LinkedHashMap<>();
            int start = Math.max(0, c.opinionCache.size() - 50);
            int i = 0;
            for (Map.Entry<Integer, Integer> e : c.opinionCache.entrySet()) {
                if (i++ >= start) trimmed.put(e.getKey(), e.getValue());
            }
            c.opinionCache.clear();
            c.opinionCache.putAll(trimmed);
        }
    }

    // ---------- 继承 ----------
    private List<HeirCandidate> heirRows(List<Integer> ids) {
        List<HeirCandidate> rows = new ArrayList<>();
        for (int cid : ids) {
            Character ch = character(cid);
            if (ch == null) continue;
            rows.add(new HeirCandidate(ch.id, ch.gender, ch.birth.toOrdinal(), ch.isAlive()));
        }
        return rows;
    }

    public Integer findHeir(int ruler, RealmLaw law) {
        Character c = character(ruler);
        if (c == null) return null;
        List<HeirCandidate> children = heirRows(new ArrayList<>(c.children));
        List<Integer> dynastyIds = new ArrayList<>();
        Dynasty d = dynasties.get(c.dynasty);
        if (d != null) {
            for (int m : d.members) if (m != ruler) dynastyIds.add(m);
        }
        List<HeirCandidate> dynastyMembers = heirRows(dynastyIds);

        if (law != null) {
            HeirCandidate heir = law.pickHeir(children, dynastyMembers);
            if (heir != null) return heir.id();
        }

        // 默认：男长嗣 → 女长嗣 → 王朝成员
        List<HeirCandidate> sons = children.stream()
                .filter(r -> r.alive() && r.gender() == Gender.MALE)
                .sorted((x, y) -> Integer.compare(x.birthOrdinal(), y.birthOrdinal()))
                .toList();
        if (!sons.isEmpty()) return sons.get(0).id();
        List<HeirCandidate> daughters = children.stream()
                .filter(r -> r.alive() && r.gender() == Gender.FEMALE)
                .sorted((x, y) -> Integer.compare(x.birthOrdinal(), y.birthOrdinal()))
                .toList();
        if (!daughters.isEmpty()) return daughters.get(0).id();
        for (HeirCandidate r : dynastyMembers) {
            if (r.alive()) return r.id();
        }
        return null;
    }

    public void onDeath(int who, RealmLaw law) {
        Character c = character(who);
        if (c == null || !c.isAlive()) return;
        String name = c.name;
        List<Integer> titles = new ArrayList<>(c.heldTitles);
        Integer heir = findHeir(who, law);
        int age = c.ageAt(date);
        c.kill(date);
        pushLog(name + " 去世，享年 " + age);
        if (heir != null) {
            Character heirC = character(heir);
            String heirName = heirC != null ? heirC.name : "?";
            if (law != null && law.partitionEnabled()) {
                List<Integer> livingKids = new ArrayList<>();
                for (int cid : c.children) {
                    Character k = character(cid);
                    if (k != null && k.isAlive()) livingKids.add(cid);
                }
                List<Integer> heirs = new ArrayList<>();
                heirs.add(heir);
                for (int k : livingKids) {
                    if (k != heir && heirs.size() < 4) heirs.add(k);
                }
                for (RealmLaw.TitledHeir part : law.partitionTitles(titles, heirs)) {
                    for (int tid : part.titles()) grantTitle(tid, part.heirId());
                }
            } else {
                for (int tid : titles) grantTitle(tid, heir);
            }
            pushLog(heirName + " 继承了 " + name + " 的遗产");
        } else {
            for (int tid : titles) {
                Title t = title(tid);
                if (t != null) {
                    t.clearHolder();
                    for (int cid : t.counties) {
                        County county = map.get(cid);
                        if (county != null) county.holder = Constants.NONE_ID;
                    }
                }
            }
            pushLog(name + " 绝嗣，头衔悬空");
        }
    }

    public boolean occupyCounty(int countyId, int newHolder) {
        County county = map.get(countyId);
        if (county == null) return false;
        county.control = 30.0;
        county.holder = newHolder;
        int tid = county.ownerTitle;
        if (tid == Constants.NONE_ID) return true;
        Title t = title(tid);
        if (t == null || t.tier != TitleTier.COUNTY) return true;
        grantTitle(tid, newHolder);
        Character conqueror = character(newHolder);
        if (conqueror == null) return true;
        Title primary = title(conqueror.primaryTitle);
        if (primary != null && primary.id != tid && primary.tier.compareTo(t.tier) > 0) {
            setVassal(tid, primary.id);
        } else {
            clearVassalLink(tid);
        }
        return true;
    }

    // ---------- 贸易系统 ----------
    public void loadTradeRoutes(List<TradeRoute> routes) {
        this.tradeRoutes = routes;
        for (TradeRoute route : routes) {
            exchangeRates.put(route.key(), route.exchangeRate());
        }
    }

    public double calculateTradeIncome(int ruler) {
        Character c = character(ruler);
        if (c == null) return 0.0;
        double income = 0.0;
        for (int tid : c.heldTitles) {
            Title t = title(tid);
            if (t == null) continue;
            for (int cid : t.counties) {
                County county = map.get(cid);
                if (county == null) continue;
                for (TradeRoute route : tradeRoutes) {
                    if (route.from().equals(county.key) || route.to().equals(county.key)) {
                        double exchangeRate = exchangeRates.getOrDefault(route.key(), 1.0);
                        double portBonus = 1.0;
                        if (county.hasPort) portBonus *= (1.0 + county.portLevel * 0.2);
                        income += route.tradeVolume() * exchangeRate * 0.1 * portBonus;
                        if (county.hasPort) income += county.portIncome * 0.5;
                    }
                }
            }
        }
        return income;
    }

    // ---------- 经济 ----------
    public double monthlyIncomeOf(int ruler) {
        Character c = character(ruler);
        if (c == null) return 0.0;
        double income = 0.0;
        for (int tid : c.heldTitles) {
            Title t = title(tid);
            if (t == null) continue;
            for (int cid : t.counties) {
                County county = map.get(cid);
                if (county != null) income += county.monthlyTax();
            }
        }
        income += calculateTradeIncome(ruler);
        AttributeSet attrs = effectiveAttrs(ruler);
        if (attrs != null) income *= 1.0 + (attrs.stewardship() - 8) * 0.03;
        Title title = title(c.primaryTitle);
        if (title != null) income *= 1.0 + title.realmLaw.crownAuthority().taxBonus();
        return income;
    }

    public void processMonthlyEconomy() {
        Map<Integer, Double> net = new HashMap<>();
        for (Character c : aliveCharacters()) {
            double income = monthlyIncomeOf(c.id);

            for (TradeRoute route : tradeRoutes) {
                if (route.from().equals(String.valueOf(c.primaryTitle)) ||
                        route.to().equals(String.valueOf(c.primaryTitle))) {
                    if (rng.nextDouble() < 0.05) triggerTradeEvent(route);
                }
            }
            for (TradeRoute route : tradeRoutes) {
                if (route.from().equals(String.valueOf(c.primaryTitle)) ||
                        route.to().equals(String.valueOf(c.primaryTitle))) {
                    double maintenanceCost = route.tradeVolume() * 0.05;
                    income -= maintenanceCost;
                    pushLog(c.name + " 支付贸易路线维护成本：" + String.format("%.1f", maintenanceCost) + " 金");
                }
            }
            for (int tid : c.heldTitles) {
                Title t = title(tid);
                if (t == null) continue;
                for (int cid : t.counties) {
                    County county = map.get(cid);
                    if (county != null && county.hasPort) {
                        double portMaintenanceCost = county.portIncome * 0.2;
                        income -= portMaintenanceCost;
                        pushLog(c.name + " 支付港口维护成本：" + String.format("%.1f", portMaintenanceCost) + " 金");
                    }
                }
            }
            for (int tid : c.heldTitles) {
                Title t = title(tid);
                if (t == null) continue;
                for (int cid : t.counties) {
                    County county = map.get(cid);
                    if (county != null) income -= county.upkeep();
                }
            }
            net.put(c.id, income);
        }

        // 封臣缴税：按 de facto 层级上缴 10%
        for (Title t : titles.values()) {
            if (t.deFactoLiege != Constants.NONE_ID && t.holder != Constants.NONE_ID) {
                Title liegeT = title(t.deFactoLiege);
                if (liegeT != null && liegeT.holder != Constants.NONE_ID) {
                    double tax = net.getOrDefault(t.holder, 0.0) * 0.1;
                    net.put(liegeT.holder, net.getOrDefault(liegeT.holder, 0.0) + tax);
                    net.put(t.holder, net.getOrDefault(t.holder, 0.0) - tax);
                }
            }
        }

        for (Map.Entry<Integer, Double> e : net.entrySet()) {
            Character c = character(e.getKey());
            if (c == null) continue;
            c.addGold(e.getValue());
            if (e.getValue() > 0) c.addPrestige(1.0);
        }

        updateExchangeRates();
    }

    private void triggerTradeEvent(TradeRoute route) {
        String[] types = {"blockade", "boom", "crisis", "new_route", "trade_route_protected"};
        String eventType = types[rng.nextInt(types.length)];
        String eventDesc;

        County fromCounty = map.getByKey(route.from());
        County toCounty = map.getByKey(route.to());
        double protectionBonus = 1.0;
        if (fromCounty != null && fromCounty.tradeRouteProtected) protectionBonus *= 1.3;
        if (toCounty != null && toCounty.tradeRouteProtected) protectionBonus *= 1.3;
        final double protectMult = protectionBonus;

        switch (eventType) {
            case "blockade" -> {
                if (protectionBonus > 1.0 && rng.nextDouble() < 0.5) {
                    eventType = "protected_from_blockade";
                    eventDesc = "贸易路线 " + route.from() + " → " + route.to() + " 遭遇封锁，但商路保护减少了损失";
                    exchangeRates.computeIfPresent(route.key(), (k, v) -> v * 0.9);
                } else {
                    eventDesc = "贸易路线 " + route.from() + " → " + route.to() + " 被封锁，收入减少";
                    exchangeRates.computeIfPresent(route.key(), (k, v) -> v * 0.8);
                }
            }
            case "boom" -> {
                eventDesc = "贸易路线 " + route.from() + " → " + route.to() + " 繁荣，收入增加";
                exchangeRates.computeIfPresent(route.key(), (k, v) -> v * 1.2 * protectMult);
            }
            case "crisis" -> {
                eventDesc = "贸易路线 " + route.from() + " → " + route.to() + " 遭遇危机，收入大幅减少";
                exchangeRates.computeIfPresent(route.key(), (k, v) -> v * 0.6);
            }
            case "new_route" -> {
                eventDesc = "新的贸易路线 " + route.from() + " → " + route.to() + " 建立，收入增加";
                exchangeRates.computeIfPresent(route.key(), (k, v) -> v * 1.1 * protectMult);
            }
            default -> {
                eventDesc = "商路保护加强：贸易路线 " + route.from() + " → " + route.to() + " 得到保护，收入增加";
                exchangeRates.computeIfPresent(route.key(), (k, v) -> v * 1.15);
            }
        }

        pushLog(eventDesc);
        tradeEvents.add(new TradeEvent(date.toString(), route, eventType, eventDesc));
    }

    public void updateExchangeRates() {
        // 用 ListIterator 原位替换：贸易量下降时不能边遍历边增删，否则抛 ConcurrentModificationException
        for (ListIterator<TradeRoute> it = tradeRoutes.listIterator(); it.hasNext(); ) {
            TradeRoute route = it.next();
            String key = route.key();
            if (exchangeRates.containsKey(key)) {
                double fluctuation = 0.9 + (rng.nextDouble() * 0.2);
                double rate = exchangeRates.get(key) * fluctuation;
                rate = Math.max(0.5, Math.min(2.0, rate));
                exchangeRates.put(key, rate);

                double maintenanceCost = route.tradeVolume() * 0.05;
                County fromCounty = map.getByKey(route.from());
                County toCounty = map.getByKey(route.to());
                double maintenanceBonus = 1.0;
                if (fromCounty != null) maintenanceBonus *= fromCounty.getTradeRouteMaintenanceBonus();
                if (toCounty != null) maintenanceBonus *= toCounty.getTradeRouteMaintenanceBonus();
                maintenanceCost *= maintenanceBonus;

                if (rng.nextDouble() < 0.1) {
                    // 维护不足，贸易量下降
                    int newVolume = (int) Math.round(route.tradeVolume() * 0.9);
                    it.set(new TradeRoute(route.from(), route.to(), route.exchangeRate(), newVolume));
                    pushLog("贸易路线 " + route.from() + " → " + route.to() + " 维护不足，贸易量下降");
                }
            }
        }

        for (County county : map.list()) {
            if (county.control > 80 && county.development < county.terrain.developmentCap()) {
                if (rng.nextDouble() < 0.05) {
                    county.development = Math.min(county.terrain.developmentCap(), county.development + 1);
                }
            }
            if (county.control < 100) {
                county.control = Math.min(100.0, county.control + 1.0);
            }
        }
    }

    public void processHealth() {
        List<Integer> deaths = new ArrayList<>();
        for (Character c : aliveCharacters()) {
            int age = c.ageAt(date);
            if (age > 45) c.health -= 0.002 * (age - 45);
            if (age > 60) c.health -= 0.005;
            for (int tid : c.traits) {
                for (Trait t : traits) {
                    if (t.id() == tid) {
                        c.health += t.healthMod();
                        break;
                    }
                }
            }
            if (rng.nextDouble() < 0.001) {
                c.health -= 0.5;
                c.addStress(10);
            }
            if (c.health <= 0) deaths.add(c.id);
        }
        for (int did : deaths) {
            Character c = character(did);
            RealmLaw law = null;
            if (c != null && c.primaryTitle != Constants.NONE_ID) {
                Title t = title(c.primaryTitle);
                if (t != null) law = t.realmLaw;
            }
            onDeath(did, law);
        }
    }

    public void processFertility() {
        List<int[]> couples = new ArrayList<>();
        for (Character c : aliveCharacters()) {
            if (c.gender == Gender.MALE && c.isMarried() && c.isAdult(date)) {
                for (int s : c.spouses) couples.add(new int[]{c.id, s});
            }
        }
        String[] maleNames = {"艾德温", "哈罗德", "威廉", "罗伯特", "亨利"};
        String[] femaleNames = {"玛蒂尔达", "爱丽丝", "埃莉诺", "伊莎贝拉", "阿黛拉"};
        for (int[] pair : couples) {
            int fatherId = pair[0], motherId = pair[1];
            Character f = character(fatherId);
            Character m = character(motherId);
            if (f == null || m == null || !m.isAlive() || m.gender != Gender.FEMALE) continue;
            if (!m.isAdult(date) || m.ageAt(date) > 45) continue;
            double chance = Math.max(0.0, Math.min(0.15, f.fertility * m.fertility * 0.02));
            if (rng.nextDouble() >= chance) continue;
            Gender gender = rng.nextDouble() < 0.5 ? Gender.MALE : Gender.FEMALE;
            Dynasty dname = dynasties.get(f.dynasty);
            String dlabel = dname != null ? dname.name : "无名";
            String first = gender == Gender.MALE
                    ? maleNames[rng.nextInt(maleNames.length)]
                    : femaleNames[rng.nextInt(femaleNames.length)];
            String childName = first + "·" + dlabel;
            int child = createCharacter(childName, f.dynasty, gender, date);
            setParents(child, fatherId, motherId);
            AttributeSet fa = effectiveAttrs(fatherId);
            AttributeSet ma = effectiveAttrs(motherId);
            Character ch = character(child);
            if (fa != null && ma != null && ch != null) {
                ch.baseAttrs = new AttributeSet(
                        Math.max(1, (fa.diplomacy() + ma.diplomacy()) / 2 + rng.nextInt(-2, 3)),
                        Math.max(1, (fa.martial() + ma.martial()) / 2 + rng.nextInt(-2, 3)),
                        Math.max(1, (fa.stewardship() + ma.stewardship()) / 2 + rng.nextInt(-2, 3)),
                        Math.max(1, (fa.intrigue() + ma.intrigue()) / 2 + rng.nextInt(-2, 3)),
                        Math.max(1, (fa.learning() + ma.learning()) / 2 + rng.nextInt(-2, 3)),
                        Math.max(1, (fa.prowess() + ma.prowess()) / 2 + rng.nextInt(-2, 3))
                );
                ch.culture = f.culture;
                ch.faith = f.faith;
            }
            pushLog("新生儿：" + childName);
        }
    }

    /** 贸易事件记录。 */
    public record TradeEvent(String date, TradeRoute route, String type, String description) {}
}
