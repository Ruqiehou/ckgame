package com.ckgame.game;

import com.ckgame.ai.AiDirector;
import com.ckgame.core.Constants;
import com.ckgame.core.balance.Balance;
import com.ckgame.core.calendar.Season;
import com.ckgame.core.stats.AttributeSet;
import com.ckgame.events.EventChoice;
import com.ckgame.events.EventEngine;
import com.ckgame.events.EventInstance;
import com.ckgame.events.Storyline;
import com.ckgame.events.StorylineSystem;
import com.ckgame.military.Army;
import com.ckgame.military.ArmyStatus;
import com.ckgame.military.BattleResult;
import com.ckgame.military.BattleSimulator;
import com.ckgame.military.Siege;
import com.ckgame.military.SiegeEvent;
import com.ckgame.military.SiegeManager;
import com.ckgame.military.War;
import com.ckgame.military.WarManager;
import com.ckgame.military.WarParticipant;
import com.ckgame.military.WarResult;
import com.ckgame.politics.CasusBelli;
import com.ckgame.politics.Council;
import com.ckgame.politics.CouncilManager;
import com.ckgame.politics.CouncilMonthlyResult;
import com.ckgame.politics.Diplomacy;
import com.ckgame.politics.Faction;
import com.ckgame.politics.FactionEvent;
import com.ckgame.politics.FactionKind;
import com.ckgame.politics.FactionManager;
import com.ckgame.politics.Laws.RealmLaw;
import com.ckgame.politics.Scheme;
import com.ckgame.politics.SchemeKind;
import com.ckgame.politics.SchemeManager;
import com.ckgame.politics.SchemeOutcome;
import com.ckgame.core.TitleTier;
import com.ckgame.world.Character;
import com.ckgame.world.County;
import com.ckgame.world.MapGraph;
import com.ckgame.world.Title;
import com.ckgame.world.World;
import com.ckgame.world.buildings.BuildingSystem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 游戏主模拟循环。
 * 对应 Python 的 ck_engine.game.simulation.GameSimulation。
 */
public final class GameSimulation {
    public final World world;
    public final WarManager wars;
    public final SiegeManager sieges;
    public final EventEngine events;
    public final FactionManager factions;
    public final SchemeManager schemes;
    public final Diplomacy diplomacy;
    public final CouncilManager councils;
    public final BuildingSystem buildings;
    public final StorylineSystem storylines;
    public final Map<Integer, RealmLaw> realmLaws = new HashMap<>();
    public final Set<Integer> playerIds = new HashSet<>();
    public String scenarioId;

    public GameSimulation() {
        this(null);
    }

    public GameSimulation(String scenario) {
        this.scenarioId = ScenarioLoader.resolve(scenario);
        this.world = ScenarioLoader.loadScenario(this.scenarioId);
        this.wars = new WarManager();
        this.sieges = new SiegeManager();
        this.events = new EventEngine();
        this.factions = new FactionManager(world);
        this.schemes = new SchemeManager(world);
        this.diplomacy = new Diplomacy(world);
        this.councils = new CouncilManager(world);
        this.buildings = new BuildingSystem();
        this.storylines = new StorylineSystem();
        bootstrap();
    }

    /** 从已加载的世界和各管理器重建模拟（供读档使用，跳过 bootstrap）。 */
    public static GameSimulation reconstruct(World world, WarManager wars, SiegeManager sieges,
                                              EventEngine events, FactionManager factions,
                                              SchemeManager schemes, Diplomacy diplomacy,
                                              CouncilManager councils, BuildingSystem buildings,
                                              StorylineSystem storylines,
                                              Map<Integer, RealmLaw> realmLaws,
                                              Set<Integer> playerIds) {
        GameSimulation sim = new GameSimulation();
        // 替换默认初始化的各子系统
        copyWorldState(sim.world, world);
        sim.wars.wars.clear();
        sim.wars.armies.clear();
        sim.wars.wars.putAll(wars.wars);
        sim.wars.armies.putAll(wars.armies);
        sim.wars.nextWar = wars.nextWar;
        sim.wars.nextArmy = wars.nextArmy;
        sim.sieges.sieges.clear();
        sim.sieges.sieges.putAll(sieges.sieges);
        sim.sieges.nextId = sieges.nextId;
        sim.events.pending.clear();
        sim.events.pending.addAll(events.pending);
        sim.factions.loadState(factions.saveState());
        sim.schemes.loadState(schemes.saveState());
        sim.diplomacy.loadState(diplomacy.saveState());
        sim.councils.loadState(councils.saveState());
        sim.buildings.loadState(buildings.saveState());
        sim.storylines.storylines.clear();
        sim.storylines.storylines.addAll(storylines.storylines);
        sim.storylines.activePerCharacter.clear();
        sim.storylines.activePerCharacter.putAll(storylines.activePerCharacter);
        sim.realmLaws.clear();
        sim.realmLaws.putAll(realmLaws);
        sim.playerIds.clear();
        sim.playerIds.addAll(playerIds);
        return sim;
    }

    /** 将一个 World 的状态复制到另一个。 */
    private static void copyWorldState(World dst, World src) {
        dst.date = src.date;
        dst.tick = src.tick;
        dst.characters.clear();
        dst.characters.putAll(src.characters);
        dst.dynasties.clear();
        dst.dynasties.putAll(src.dynasties);
        dst.titles.clear();
        dst.titles.putAll(src.titles);
        dst.log.clear();
        dst.log.addAll(src.log);
        dst.nextChar = src.nextChar;
        dst.nextDynasty = src.nextDynasty;
        dst.nextTitle = src.nextTitle;
        dst.nextCounty = src.nextCounty;
        dst.tradeRoutes.clear();
        dst.tradeRoutes.addAll(src.tradeRoutes);
        dst.exchangeRates.clear();
        dst.exchangeRates.putAll(src.exchangeRates);
        dst.tradeEvents.clear();
        dst.tradeEvents.addAll(src.tradeEvents);
        // 重建地图（dst.map 是 final 的，直接插入）
        for (County c : src.map.list()) {
            dst.map.insert(c);
        }
        for (County c : src.map.list()) {
            for (int n : c.neighbors) {
                dst.map.connect(c.id, n);
            }
        }
    }

    /** 初始化：宿敌与宣称、同盟、内阁与法律、剧情线注册。 */
    public void bootstrap() {
        int william = Constants.NONE_ID;
        int harold = Constants.NONE_ID;
        for (Character c : world.aliveCharacters()) {
            if (william == Constants.NONE_ID && c.name.contains("威廉·征服者")) {
                william = c.id;
            }
            if (harold == Constants.NONE_ID && c.name.contains("哈罗德")) {
                harold = c.id;
            }
        }
        if (william != Constants.NONE_ID && harold != Constants.NONE_ID) {
            diplomacy.setRival(william, harold);
            int eng = Constants.NONE_ID;
            for (Title t : world.titles.values()) {
                if (t.name.contains("英格兰")) {
                    eng = t.id;
                    break;
                }
            }
            if (eng != Constants.NONE_ID) {
                diplomacy.addClaim(william, eng, 80);
            }
        }
        int edwin = Constants.NONE_ID;
        int morcar = Constants.NONE_ID;
        for (Character c : world.aliveCharacters()) {
            if (edwin == Constants.NONE_ID && c.name.contains("埃德温")) {
                edwin = c.id;
            }
            if (morcar == Constants.NONE_ID && c.name.contains("莫卡")) {
                morcar = c.id;
            }
        }
        if (edwin != Constants.NONE_ID && morcar != Constants.NONE_ID) {
            diplomacy.formAlliance(edwin, morcar, world.date);
        }
        for (Character r : new ArrayList<>(world.rulers())) {
            ensureCouncil(r.id);
            realmLaws.putIfAbsent(r.id, RealmLaw.feudalDefault());
        }
        for (Storyline storyline : StorylineSystem.builtinStorylines()) {
            storylines.createStoryline(storyline);
        }
    }

    /** 确保统治者有内阁；空缺时按属性自动任命。 */
    public void ensureCouncil(int ruler) {
        List<int[]> candidates = new ArrayList<>();
        for (Character c : world.aliveCharacters()) {
            if (c.id == ruler || !c.isAdult(world.date)) {
                continue;
            }
            AttributeSet a = world.effectiveAttrs(c.id);
            if (a == null) {
                continue;
            }
            candidates.add(new int[]{
                    c.id, a.diplomacy(), a.martial(), a.stewardship(), a.intrigue(), a.learning()
            });
        }
        Council council = councils.councilFor(ruler);
        if (council.members().isEmpty()) {
            council.autoAppoint(candidates);
        }
    }

    /** 推进指定天数。 */
    public void runDays(int days) {
        for (int i = 0; i < days; i++) {
            tickDay();
        }
    }

    /** 单日推进。 */
    public void tickDay() {
        world.date = world.date.advanceOneDay();
        world.tick += 1;
        Season season = world.date.season();
        boolean winter = season == Season.WINTER;

        // 季节与补给影响行军概率
        wars.tickMovement(army -> {
            double chance = 1.0;
            if (season == Season.WINTER) {
                chance *= Balance.MOVE_CHANCE_WINTER;
            } else if (season == Season.AUTUMN) {
                chance *= Balance.MOVE_CHANCE_AUTUMN;
            }
            if (army.supply < Balance.SUPPLY_MOVE_SLOW_THRESHOLD) {
                chance *= Balance.MOVE_CHANCE_LOW_SUPPLY;
            }
            return Math.max(Balance.MOVE_CHANCE_MIN, chance);
        });
        tickArmySupply(winter);
        resolveEncounters();
        tickSieges();
        wars.disbandEmpty();
        events.tickCooldowns();
        if (world.date.isMonthStart()) {
            tickMonth();
        }
        if (world.date.isYearStart()) {
            world.pushLog("—— " + world.date.year() + " 年来临 ——");
            for (String line : diplomacy.expireTreaties(world.date.year(), world)) {
                world.pushLog(line);
            }
        }
    }

    /** 军队每日补给：友境恢复，敌境消耗。 */
    public void tickArmySupply(boolean winter) {
        Map<Integer, Set<Integer>> enemyHolders = new HashMap<>();
        for (War w : wars.activeWars()) {
            Set<Integer> atk = new HashSet<>();
            Set<Integer> dfd = new HashSet<>();
            for (WarParticipant p : w.participants) {
                if (p.isAttacker) {
                    atk.add(p.character);
                } else {
                    dfd.add(p.character);
                }
            }
            for (int a : atk) {
                enemyHolders.computeIfAbsent(a, k -> new HashSet<>()).addAll(dfd);
            }
            for (int d : dfd) {
                enemyHolders.computeIfAbsent(d, k -> new HashSet<>()).addAll(atk);
            }
        }
        for (Army army : wars.armies.values()) {
            if (!army.isActive()) {
                continue;
            }
            County county = world.map.get(army.location);
            if (county == null) {
                continue;
            }
            Set<Integer> enemies = enemyHolders.getOrDefault(army.owner, Collections.emptySet());
            boolean inFriendly = !enemies.contains(county.holder);
            army.applySupplyTick(inFriendly, winter);
        }
    }

    /** 月度推进。 */
    public void tickMonth() {
        world.processHealth();

        // 军队维护费：破产时强制解散部分军团（在收税前用上个月结余支付）
        Map<Integer, List<Army>> byOwner = new LinkedHashMap<>();
        for (Army army : wars.armies.values()) {
            if (army.isActive()) {
                byOwner.computeIfAbsent(army.owner, k -> new ArrayList<>()).add(army);
            }
        }
        for (Map.Entry<Integer, List<Army>> e : byOwner.entrySet()) {
            Character c = world.character(e.getKey());
            if (c == null) {
                continue;
            }
            double cost = 0.0;
            for (Army a : e.getValue()) {
                cost += a.monthlyMaintenance();
            }
            // 维护费略抬高，避免常备军无代价
            cost *= Balance.ARMY_MAINTENANCE_MULT;
            if (c.gold >= cost) {
                c.addGold(-cost);
                continue;
            }
            // 掏空国库并每月最多解散一支最大军团
            c.addGold(-c.gold);
            List<Army> armies = e.getValue();
            armies.sort((a, b) -> Integer.compare(b.totalMen(), a.totalMen()));
            Army dis = armies.get(0);
            dis.status = ArmyStatus.DISBANDED;
            dis.stacks.clear();
            world.pushLog(c.name + " 国库空虚，被迫解散 " + dis.name);
        }

        world.processMonthlyEconomy();
        world.processFertility();

        // 每月给予少量经验值
        for (Character c : world.aliveCharacters()) {
            c.gainXp(5);
        }

        List<Integer> rulerIds = new ArrayList<>();
        for (Character r : world.rulers()) {
            rulerIds.add(r.id);
        }
        for (int rid : rulerIds) {
            RealmLaw law = realmLaws.get(rid);
            if (law != null && law.crownAuthority().taxBonus() > 0) {
                double income = world.monthlyIncomeOf(rid) * law.crownAuthority().taxBonus();
                Character c = world.character(rid);
                if (c != null) {
                    c.addGold(income);
                }
            }
        }

        tickCouncils();
        List<Integer> chars = new ArrayList<>();
        for (Character c : world.aliveCharacters()) {
            if (c.isRuler || c.isAdult(world.date)) {
                chars.add(c.id);
            }
            if (chars.size() >= 50) {
                break;
            }
        }
        events.dailyCheck(world, chars);
        // AI 角色的待处理事件立即自动结算，玩家事件保留
        List<EventInstance> aiPending = new ArrayList<>();
        Iterator<EventInstance> it = events.pending.iterator();
        while (it.hasNext()) {
            EventInstance inst = it.next();
            if (!playerIds.contains(inst.character)) {
                aiPending.add(inst);
                it.remove();
            }
        }
        for (EventInstance inst : aiPending) {
            EventChoice best = Collections.max(inst.choices,
                    Comparator.comparingDouble(ch -> ch.aiWeight));
            events.resolveChoice(world, inst, best.id);
        }
        tickFactions();
        tickSchemes();
        List<com.ckgame.ai.AiAction> actions = AiDirector.monthlyActions(
                world, wars, diplomacy, schemes, new HashSet<>(playerIds));
        AiDirector.applyActions(world, wars, diplomacy, schemes, actions);
        tickWars();
        // 先结算战争疲劳增减，再对非交战者衰减
        Set<Integer> atWarIds = new HashSet<>();
        for (War w : wars.activeWars()) {
            atWarIds.add(w.attackerPrimary);
            atWarIds.add(w.defenderPrimary);
        }
        diplomacy.tickWarExhaustion(atWarIds);
        tryStartSieges();

        for (Character r : new ArrayList<>(world.rulers())) {
            ensureCouncil(r.id);
            realmLaws.putIfAbsent(r.id, RealmLaw.feudalDefault());
        }
    }

    /** 内阁月度结算。 */
    public void tickCouncils() {
        Map<Integer, AttributeSet> skillMap = new HashMap<>();
        for (Character c : world.aliveCharacters()) {
            AttributeSet a = world.effectiveAttrs(c.id);
            if (a != null) {
                skillMap.put(c.id, a);
            }
        }
        List<Integer> rulerIds = new ArrayList<>();
        for (Character r : world.rulers()) {
            rulerIds.add(r.id);
        }
        for (int rid : rulerIds) {
            Council council = councils.councilFor(rid);
            CouncilMonthlyResult effect = council.monthlyEffect(skillMap);
            Character c = world.character(rid);
            if (c != null) {
                c.addGold(effect.gold);
                c.addPrestige(effect.prestige);
                c.piety = Math.max(0.0, c.piety + effect.piety);
            }
            if (effect.controlGain > 0 && c != null) {
                for (int tid : new ArrayList<>(c.heldTitles)) {
                    Title t = world.title(tid);
                    if (t == null) {
                        continue;
                    }
                    for (int cid : t.counties) {
                        County county = world.map.get(cid);
                        if (county != null) {
                            county.control = Math.min(100.0, county.control + effect.controlGain * 0.2);
                        }
                    }
                }
            }
            if (effect.developmentChance > 0 && world.nextDouble() < 0.05 && c != null) {
                if (!c.heldTitles.isEmpty()) {
                    Title t = world.title(c.heldTitles.get(0));
                    if (t != null && !t.counties.isEmpty()) {
                        County county = world.map.get(t.counties.get(0));
                        if (county != null && county.development < county.terrain.developmentCap()) {
                            county.development += 1;
                        }
                    }
                }
            }
            if (effect.claimProgress >= 20 && world.nextDouble() < 0.15 && c != null) {
                Set<Integer> owned = new HashSet<>();
                for (int tid : c.heldTitles) {
                    Title t = world.title(tid);
                    if (t != null) {
                        owned.addAll(t.counties);
                    }
                }
                County target = null;
                for (County co : world.map.list()) {
                    if (!owned.contains(co.id)) {
                        target = co;
                        break;
                    }
                }
                if (target != null) {
                    diplomacy.addClaim(rid, target.ownerTitle, target.id, 50);
                    world.pushLog(c.name + " 伪造了对 " + target.name + " 的宣称");
                }
            }
            // 破坏阴谋：压低以本君主为目标的活跃阴谋进度
            boolean disrupt = false;
            for (String line : effect.logs) {
                if (line.contains("破坏敌对阴谋")) {
                    disrupt = true;
                    break;
                }
            }
            if (disrupt) {
                int spy = council.spymaster;
                AttributeSet spyAttrs = skillMap.get(spy);
                int spySkill = spyAttrs != null ? spyAttrs.intrigue() : 8;
                for (Scheme scheme : new ArrayList<>(schemes.schemes())) {
                    if (!scheme.exposed && !scheme.isComplete() && scheme.target == rid) {
                        scheme.progress = Math.max(0.0, scheme.progress - spySkill * 0.4);
                        scheme.secrecy = Math.max(0.0, scheme.secrecy - spySkill * 0.2);
                    }
                }
            }
        }
    }

    /** 派系月度推进。 */
    public void tickFactions() {
        List<int[]> vassalPairs = new ArrayList<>();
        for (Title t : world.titles.values()) {
            if (t.holder == Constants.NONE_ID || t.deFactoLiege == Constants.NONE_ID) {
                continue;
            }
            Title lt = world.title(t.deFactoLiege);
            if (lt == null || lt.holder == Constants.NONE_ID || lt.holder == t.holder) {
                continue;
            }
            int op = world.opinion(t.holder, lt.holder);
            RealmLaw law = realmLaws.get(lt.holder);
            if (law != null) {
                op += law.crownAuthority().vassalOpinionPenalty();
            }
            vassalPairs.add(new int[]{t.holder, lt.holder, op});
        }

        Map<Integer, Double> mil = new HashMap<>();
        Map<Integer, Double> liegePow = new HashMap<>();
        for (Character r : world.rulers()) {
            double p = estimatePower(world, r.id);
            mil.put(r.id, p);
            liegePow.put(r.id, p);
        }
        for (int[] row : vassalPairs) {
            mil.computeIfAbsent(row[0], v -> (double) estimatePower(world, row[0]));
        }

        factions.recomputePower(mil, liegePow);
        Map<Integer, Integer> opinions = new HashMap<>();
        for (int[] row : vassalPairs) {
            opinions.put(row[0], row[2]);
        }
        factions.tickDiscontent(opinions);
        List<FactionEvent> evs = factions.monthlyAi(vassalPairs, world::nextDouble);
        for (FactionEvent ev : evs) {
            switch (ev.kind) {
                case "formed" -> {
                    Character founder = world.character(ev.founder);
                    Character liege = world.character(ev.liege);
                    String fk = ev.factionKind != null ? ev.factionKind.nameZh() : "派系";
                    if (founder != null && liege != null) {
                        world.pushLog(founder.name + " 针对 " + liege.name + " 组建了" + fk);
                    }
                }
                case "joined" -> {
                    Character who = world.character(ev.who);
                    if (who != null) {
                        world.pushLog(who.name + " 加入了派系");
                    }
                }
                case "ultimatum" -> {
                    Character liege = world.character(ev.liege);
                    String text = ev.factionKind != null ? ev.factionKind.ultimatumText() : "要求";
                    if (liege != null) {
                        world.pushLog("派系向 " + liege.name + " 发出最后通牒：" + text
                                + "（" + ev.members.size() + " 人）");
                    }
                    Faction f = factions.factions().get(ev.factionId);
                    if (f != null) {
                        f.discontent = Math.min(100.0, f.discontent + 20);
                        // 最后通牒已发出，等待玩家或AI决策
                    }
                }
                case "revolt" -> {
                    Character liege = world.character(ev.liege);
                    String fk = ev.factionKind != null ? ev.factionKind.nameZh() : "叛乱";
                    if (liege != null) {
                        world.pushLog("叛乱爆发！" + fk + " vs " + liege.name);
                    }
                    if (!ev.members.isEmpty()) {
                        int leader = ev.members.get(0);
                        CasusBelli cb = (ev.factionKind == FactionKind.CLAIMANT
                                || ev.factionKind == FactionKind.POPULAR)
                                ? CasusBelli.DEPOSE_LIEGE
                                : CasusBelli.INDEPENDENCE;
                        if (diplomacy.canDeclareWar(leader, ev.liege, world.date.year())) {
                            wars.declareWar(cb, leader, ev.liege, world.date, fk + "叛乱");
                            diplomacy.setAtWar(leader, ev.liege, true);
                        }
                    }
                    factions.dissolve(ev.factionId);
                }
                case "dissolved" -> world.pushLog("派系解散：" + ev.reason);
                default -> {
                    // 未知事件类型，忽略
                }
            }
        }
    }

    /** 阴谋月度推进。 */
    public void tickSchemes() {
        Map<Integer, Integer> intrigue = new HashMap<>();
        for (Character c : world.aliveCharacters()) {
            AttributeSet a = world.effectiveAttrs(c.id);
            if (a != null) {
                intrigue.put(c.id, a.intrigue());
            }
        }
        for (Council council : councils.allCouncils().values()) {
            if (council.spymaster != Constants.NONE_ID) {
                intrigue.put(council.ruler, intrigue.getOrDefault(council.ruler, 8) + 2);
            }
        }
        List<SchemeOutcome> outcomes = schemes.monthlyTick(intrigue, world::nextDouble);
        for (SchemeOutcome o : outcomes) {
            if ("success".equals(o.kind) && o.schemeKind != null) {
                Character owner = world.character(o.owner);
                Character target = world.character(o.target);
                String on = owner != null ? owner.name : "?";
                String tn = target != null ? target.name : "?";
                SchemeKind kind = o.schemeKind;
                if (kind == SchemeKind.MURDER) {
                    world.pushLog("阴谋成功：" + on + " 暗杀了 " + tn + "！");
                    target = world.character(o.target);
                    RealmLaw law = null;
                    if (target != null && target.primaryTitle != Constants.NONE_ID) {
                        Title t = world.title(target.primaryTitle);
                        if (t != null) {
                            law = t.realmLaw;
                        }
                    }
                    world.onDeath(o.target, law);
                    if (owner != null) {
                        owner.addStress(20);
                        owner.addPrestige(-15);
                    }
                } else if (kind == SchemeKind.SWAY) {
                    world.modifyOpinion(o.target, o.owner, 25);
                    world.pushLog(on + " 成功拉拢了 " + tn);
                } else if (kind == SchemeKind.FABRICATE_HOOK) {
                    world.modifyOpinion(o.target, o.owner, -10);
                    world.pushLog(on + " 掌握了 " + tn + " 的把柄");
                    if (owner != null) {
                        owner.addPrestige(10);
                    }
                } else if (kind == SchemeKind.ABDUCT) {
                    world.pushLog(on + " 绑架了 " + tn);
                    if (target != null) {
                        target.addStress(30);
                    }
                } else if (kind == SchemeKind.SEDUCE) {
                    world.modifyOpinion(o.target, o.owner, 30);
                    world.pushLog(on + " 与 " + tn + " 产生私情");
                } else if (kind == SchemeKind.CLAIM_FABRICATION) {
                    world.pushLog(on + " 完成对 " + tn + " 相关宣称的伪造");
                }
            } else if ("exposed".equals(o.kind)) {
                Character owner = world.character(o.owner);
                Character target = world.character(o.target);
                String on = owner != null ? owner.name : "?";
                String tn = target != null ? target.name : "?";
                world.pushLog("阴谋败露！" + on + " 对 " + tn + " 的密谋被发现");
                world.modifyOpinion(o.target, o.owner, -40);
                if (owner != null) {
                    owner.addPrestige(-25);
                    owner.addStress(15);
                }
            }
        }
    }

    /** 军队遭遇：同省敌对军队交战。 */
    public void resolveEncounters() {
        // 按位置分组军队，O(n)
        Map<Integer, List<int[]>> byLoc = new HashMap<>();
        for (Army a : wars.armies.values()) {
            if (a.isActive()) {
                byLoc.computeIfAbsent(a.location, k -> new ArrayList<>())
                        .add(new int[]{a.id, a.owner, a.totalMen()});
            }
        }

        for (Map.Entry<Integer, List<int[]>> entry : byLoc.entrySet()) {
            List<int[]> locArmies = entry.getValue();
            if (locArmies.size() < 2) {
                continue;
            }
            // 按所有者分组：owner -> [(army_id, men), ...]
            Map<Integer, List<int[]>> byOwner = new LinkedHashMap<>();
            for (int[] row : locArmies) {
                byOwner.computeIfAbsent(row[1], k -> new ArrayList<>())
                        .add(new int[]{row[0], row[2]});
            }

            List<Integer> owners = new ArrayList<>(byOwner.keySet());
            for (int i = 0; i < owners.size(); i++) {
                for (int j = i + 1; j < owners.size(); j++) {
                    int oa = owners.get(i);
                    int ob = owners.get(j);
                    boolean enemies = false;
                    for (War w : wars.activeWars()) {
                        if ((w.isAttacker(oa) && !w.isAttacker(ob) && w.involves(ob))
                                || (w.isAttacker(ob) && !w.isAttacker(oa) && w.involves(oa))) {
                            enemies = true;
                            break;
                        }
                    }
                    if (!enemies) {
                        continue;
                    }
                    // 每对阵营只打最大规模的一对军队
                    int aId = Collections.max(byOwner.get(oa),
                            Comparator.comparingInt(row -> row[1]))[0];
                    int bId = Collections.max(byOwner.get(ob),
                            Comparator.comparingInt(row -> row[1]))[0];
                    Army armyA = wars.armies.get(aId);
                    Army armyB = wars.armies.get(bId);
                    if (armyA == null || armyB == null) {
                        continue;
                    }
                    resolveBattlePair(armyA, armyB);
                }
            }
        }
    }

    /** 结算一对军队的战斗。 */
    public void resolveBattlePair(Army armyA, Army armyB) {
        AttributeSet atkAttrs = world.effectiveAttrs(armyA.commander);
        AttributeSet defAttrs = world.effectiveAttrs(armyB.commander);
        int atkM = atkAttrs != null ? atkAttrs.martial() : 8;
        int defM = defAttrs != null ? defAttrs.martial() : 8;
        County county = world.map.get(armyA.location);
        double width = county != null ? county.terrain.combatWidth() : 1.0;

        Season season = world.date.season();
        double seasonMod = switch (season) {
            case SPRING -> Balance.SEASON_COMBAT_SPRING;
            case SUMMER -> Balance.SEASON_COMBAT_SUMMER;
            case AUTUMN -> Balance.SEASON_COMBAT_AUTUMN;
            case WINTER -> Balance.SEASON_COMBAT_WINTER;
        };
        BattleResult result = BattleSimulator.resolve(armyA, armyB, atkM, defM, width, seasonMod);
        Character an = world.character(armyA.owner);
        Character bn = world.character(armyB.owner);
        world.pushLog("战斗！" + (an != null ? an.name : "?") + " vs "
                + (bn != null ? bn.name : "?") + " — " + result.description);
        for (War w : new ArrayList<>(wars.activeWars())) {
            if (w.involves(armyA.owner) && w.involves(armyB.owner)) {
                if (w.isAttacker(armyA.owner)) {
                    w.applyWarscore(result.warscoreChange);
                } else {
                    w.applyWarscore(-result.warscoreChange);
                }
            }
        }
        Army loser = result.attackerWon ? armyB : armyA;
        retreatArmy(loser);
    }

    /** 败军撤退：优先友方领地，其次中立，避开敌方。 */
    public void retreatArmy(Army army) {
        County county = world.map.get(army.location);
        if (county == null) {
            army.status = ArmyStatus.RETREATING;
            return;
        }
        if (county.neighbors.isEmpty()) {
            // 孤立省份：尝试寻找任意可达省份
            List<Integer> allCounties = new ArrayList<>(world.map.counties().keySet());
            Collections.shuffle(allCounties, world.rng());
            for (int cid : allCounties) {
                if (cid == army.location) {
                    continue;
                }
                List<Integer> path = world.map.path(army.location, cid);
                if (path != null) {
                    army.setPath(path);
                    army.status = ArmyStatus.RETREATING;
                    return;
                }
            }
            army.status = ArmyStatus.RETREATING;
            return;
        }
        Set<Integer> enemyHolders = new HashSet<>();
        for (War w : wars.activeWars()) {
            if (!w.involves(army.owner)) {
                continue;
            }
            for (WarParticipant p : w.participants) {
                if (w.isAttacker(p.character) != w.isAttacker(army.owner)) {
                    enemyHolders.add(p.character);
                }
            }
        }
        List<Integer> friendly = new ArrayList<>();
        List<Integer> neutral = new ArrayList<>();
        List<Integer> hostile = new ArrayList<>();
        for (int nid : county.neighbors) {
            County n = world.map.get(nid);
            if (n == null) {
                continue;
            }
            if (n.holder == army.owner) {
                friendly.add(nid);
            } else if (enemyHolders.contains(n.holder)) {
                hostile.add(nid);
            } else {
                neutral.add(nid);
            }
        }
        List<Integer> pool = !friendly.isEmpty() ? friendly
                : !neutral.isEmpty() ? neutral
                : !hostile.isEmpty() ? hostile
                : county.neighbors;
        int dest = pool.get(0);
        army.location = dest;
        army.path.clear();
        army.status = ArmyStatus.RETREATING;
    }

    /** 尝试为停留省份的闲置军队发起围城。 */
    public void tryStartSieges() {
        List<int[]> snapshots = new ArrayList<>();
        for (Army a : wars.armies.values()) {
            if (a.isActive() && a.status == ArmyStatus.IDLE) {
                snapshots.add(new int[]{a.id, a.owner, a.location});
            }
        }
        for (int[] row : snapshots) {
            int aid = row[0];
            int owner = row[1];
            int loc = row[2];
            County county = world.map.get(loc);
            if (county == null || county.holder == Constants.NONE_ID || county.holder == owner) {
                continue;
            }
            boolean enemies = false;
            for (War w : wars.activeWars()) {
                if (w.involves(owner) && w.involves(county.holder)
                        && w.isAttacker(owner) != w.isAttacker(county.holder)) {
                    enemies = true;
                    break;
                }
            }
            if (!enemies || sieges.activeAt(loc) != null) {
                continue;
            }
            int sid = sieges.start(
                    loc,
                    aid,
                    owner,
                    county.holder,
                    county.fortLevel,
                    Math.max(50, county.levies / 4),
                    world.date);
            Army army = wars.army(aid);
            if (army != null) {
                army.status = ArmyStatus.SIEGING;
            }
            Character o = world.character(owner);
            world.pushLog((o != null ? o.name : "?") + " 开始围攻 " + county.name
                    + " (围城 #" + sid + ")");
        }
    }

    /** 围城每日推进。 */
    public void tickSieges() {
        Map<Integer, Integer> men = new HashMap<>();
        Map<Integer, Integer> martial = new HashMap<>();
        Map<Integer, Integer> locs = new HashMap<>();
        for (Army a : wars.armies.values()) {
            men.put(a.id, a.totalMen());
            locs.put(a.id, a.location);
            AttributeSet attrs = world.effectiveAttrs(a.commander);
            martial.put(a.id, attrs != null ? attrs.martial() : 8);
        }
        for (SiegeEvent ev : sieges.tickDay(men, martial, locs)) {
            if ("captured".equals(ev.kind)) {
                County county = world.map.get(ev.county);
                Character attacker = world.character(ev.attacker);
                String cn = county != null ? county.name : "?";
                String anName = attacker != null ? attacker.name : "?";
                world.pushLog(anName + " 攻陷了 " + cn + "！");
                world.occupyCounty(ev.county, ev.attacker);
                for (War w : new ArrayList<>(wars.activeWars())) {
                    if (w.involves(ev.attacker) && w.involves(ev.defender)) {
                        if (w.isAttacker(ev.attacker)) {
                            w.applyWarscore(25);
                        } else {
                            w.applyWarscore(-25);
                        }
                    }
                }
                Siege s = sieges.sieges.get(ev.siegeId);
                if (s != null) {
                    Army army = wars.army(s.attackerArmy);
                    if (army != null && army.status == ArmyStatus.SIEGING) {
                        army.status = ArmyStatus.IDLE;
                    }
                }
                if (attacker != null) {
                    attacker.addPrestige(10);
                    attacker.addGold(5);
                }
            } else if ("lifted".equals(ev.kind)) {
                County county = world.map.get(ev.county);
                String cn = county != null ? county.name : "?";
                world.pushLog("围攻 " + cn + " 解除：" + ev.reason);
                Siege s = sieges.sieges.get(ev.siegeId);
                if (s != null) {
                    Army army = wars.army(s.attackerArmy);
                    if (army != null && army.status == ArmyStatus.SIEGING) {
                        army.status = ArmyStatus.IDLE;
                    }
                }
            }
        }
    }

    /** 战争月度推进：疲劳、分数回归与终战判定。 */
    public void tickWars() {
        List<Integer> warIds = new ArrayList<>();
        for (War w : wars.activeWars()) {
            warIds.add(w.id);
        }
        for (int wid : warIds) {
            War w = wars.war(wid);
            if (w == null) {
                continue;
            }
            diplomacy.addWarExhaustion(w.attackerPrimary, Balance.WAR_EXHAUSTION_MONTHLY_ATK);
            diplomacy.addWarExhaustion(w.defenderPrimary, Balance.WAR_EXHAUSTION_MONTHLY_DEF);
            if (w.warscore > 0) {
                w.applyWarscore(-1);
            } else if (w.warscore < 0) {
                w.applyWarscore(1);
            }
            if (w.canEnforce() || w.warscore >= 100) {
                wars.endWar(wid, WarResult.ATTACKER_VICTORY);
                diplomacy.setAtWar(w.attackerPrimary, w.defenderPrimary, false);
                diplomacy.setTruce(w.attackerPrimary, w.defenderPrimary,
                        world.date.year() + Balance.VICTORY_TRUCE_YEARS);
                Character an = world.character(w.attackerPrimary);
                Character dn = world.character(w.defenderPrimary);
                world.pushLog("战争结束：" + (an != null ? an.name : "?") + " 战胜 "
                        + (dn != null ? dn.name : "?") + "，强制执行和约");
                // 领土变更：攻击者获得部分省份
                transferTerritory(w.attackerPrimary, w.defenderPrimary);
                if (an != null) {
                    an.addPrestige(w.cb.attackerPrestigeOnWin());
                    an.addGold(30);
                }
                if (dn != null) {
                    dn.addPrestige(-30);
                    dn.addGold(-20);
                }
                if (w.cb == CasusBelli.CONQUEST || w.cb == CasusBelli.CLAIM
                        || w.cb == CasusBelli.DE_JURE) {
                    transferOneCounty(w.defenderPrimary, w.attackerPrimary);
                }
            } else if (w.canSurrender() || w.warscore <= -100) {
                wars.endWar(wid, WarResult.DEFENDER_VICTORY);
                diplomacy.setAtWar(w.attackerPrimary, w.defenderPrimary, false);
                diplomacy.setTruce(w.attackerPrimary, w.defenderPrimary,
                        world.date.year() + Balance.VICTORY_TRUCE_YEARS);
                Character an = world.character(w.attackerPrimary);
                Character dn = world.character(w.defenderPrimary);
                world.pushLog("战争结束：" + (dn != null ? dn.name : "?") + " 击退 "
                        + (an != null ? an.name : "?"));
                // 防御者获胜，攻击者失去部分省份
                transferTerritory(w.defenderPrimary, w.attackerPrimary);
                if (dn != null) {
                    dn.addPrestige(40);
                }
                if (an != null) {
                    an.addPrestige(-20);
                }
            } else {
                double atkExh = diplomacy.warExhaustion().getOrDefault(w.attackerPrimary, 0.0);
                double defExh = diplomacy.warExhaustion().getOrDefault(w.defenderPrimary, 0.0);
                if (w.canWhitePeace(world.date, atkExh, defExh)) {
                    wars.endWar(wid, WarResult.WHITE_PEACE);
                    diplomacy.setAtWar(w.attackerPrimary, w.defenderPrimary, false);
                    diplomacy.setTruce(w.attackerPrimary, w.defenderPrimary,
                            world.date.year() + Balance.WHITE_PEACE_TRUCE_YEARS);
                    Character an = world.character(w.attackerPrimary);
                    Character dn = world.character(w.defenderPrimary);
                    world.pushLog("白和：" + (an != null ? an.name : "?") + " 与 "
                            + (dn != null ? dn.name : "?") + " 停战");
                    if (an != null) {
                        an.addPrestige(-5);
                    }
                    if (dn != null) {
                        dn.addPrestige(5);
                    }
                }
            }
        }
    }

    /** 战争胜利后转移领土（随机 1-2 个省份）。 */
    public void transferTerritory(int winner, int loser) {
        Character loserChar = world.character(loser);
        if (loserChar == null) {
            return;
        }
        List<Integer> countiesToTransfer = new ArrayList<>();
        for (int tid : new ArrayList<>(loserChar.heldTitles)) {
            Title t = world.title(tid);
            if (t != null && t.tier == TitleTier.COUNTY && !t.counties.isEmpty()) {
                countiesToTransfer.addAll(t.counties);
            }
        }

        if (!countiesToTransfer.isEmpty()) {
            Collections.shuffle(countiesToTransfer, world.rng());
            int transferCount = Math.min(countiesToTransfer.size(), world.nextInt(1, 3));
            for (int i = 0; i < transferCount; i++) {
                int cid = countiesToTransfer.get(i);
                world.occupyCounty(cid, winner);
                County county = world.map.get(cid);
                if (county != null) {
                    world.pushLog("领土变更：" + county.name + " 被转移");
                }
            }
        }
    }

    /** 和约割让：将败者的第一个伯爵领头衔整体割让。 */
    public void transferOneCounty(int frm, int to) {
        Character loser = world.character(frm);
        if (loser == null) {
            return;
        }
        for (int tid : new ArrayList<>(loser.heldTitles)) {
            Title t = world.title(tid);
            if (t != null && t.tier == TitleTier.COUNTY && !t.counties.isEmpty()) {
                // 走占领路径，确保 holder + 封臣链同步
                for (int cid : new ArrayList<>(t.counties)) {
                    world.occupyCounty(cid, to);
                }
                world.pushLog("和约割让：" + t.name);
                return;
            }
        }
    }

    /** 简化状态打印（TUI 自行做展示）。 */
    public void printStatus() {
        System.out.println("日期: " + world.date);
        System.out.println("人物: " + world.aliveCharacters().size() + " 存活 / "
                + world.characters.size() + " 总计");
        System.out.println("统治者: " + world.rulers().size());
        System.out.println("伯爵领: " + world.map.counties().size());
        System.out.println("进行中战争: " + wars.activeWars().size());
        System.out.println("进行中围城: " + sieges.activeSieges().size());
        System.out.println("活跃派系: " + factions.factions().size());
        System.out.println("进行中阴谋: " + schemes.schemeMap().size());
    }

    /** 估算角色军事实力：所有领地月度征召兵力之和（下限 50）。 */
    public static int estimatePower(World world, int who) {
        Character c = world.character(who);
        if (c == null) {
            return 50;
        }
        Set<Integer> seen = new HashSet<>();
        int total = 0;
        for (int tid : c.heldTitles) {
            Title t = world.title(tid);
            if (t == null) {
                continue;
            }
            for (int cid : t.counties) {
                if (!seen.add(cid)) {
                    continue;
                }
                County county = world.map.get(cid);
                if (county != null) {
                    total += county.monthlyLevies();
                }
            }
        }
        return Math.max(50, total);
    }
}
