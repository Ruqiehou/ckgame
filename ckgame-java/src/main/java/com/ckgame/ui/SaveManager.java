package com.ckgame.ui;

import com.ckgame.core.Constants;
import com.ckgame.core.calendar.GameDate;
import com.ckgame.core.TitleTier;
import com.ckgame.core.stats.AttributeSet;
import com.ckgame.core.Gender;
import com.ckgame.events.EventChoice;
import com.ckgame.events.EventInstance;
import com.ckgame.events.Effect;
import com.ckgame.events.Storyline;
import com.ckgame.events.StorylineStage;
import com.ckgame.events.StorylineStatus;
import com.ckgame.game.GameSimulation;
import com.ckgame.military.Army;
import com.ckgame.military.ArmyStatus;
import com.ckgame.military.Siege;
import com.ckgame.military.SiegeManager;
import com.ckgame.military.UnitStack;
import com.ckgame.military.UnitType;
import com.ckgame.military.War;
import com.ckgame.military.WarManager;
import com.ckgame.military.WarParticipant;
import com.ckgame.military.WarResult;
import com.ckgame.politics.CasusBelli;
import com.ckgame.politics.CouncilManager;
import com.ckgame.politics.Diplomacy;
import com.ckgame.politics.FactionManager;
import com.ckgame.politics.Laws.RealmLaw;
import com.ckgame.politics.Laws.SuccessionLaw;
import com.ckgame.politics.Laws.CrownAuthority;
import com.ckgame.politics.Laws.GenderLaw;
import com.ckgame.politics.SchemeManager;
import com.ckgame.world.Character;
import com.ckgame.world.County;
import com.ckgame.world.Dynasty;
import com.ckgame.world.LifeState;
import com.ckgame.world.MapGraph;
import com.ckgame.world.Terrain;
import com.ckgame.world.Title;
import com.ckgame.world.TradeRoute;
import com.ckgame.world.World;
import com.ckgame.world.buildings.BuildingSystem;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 游戏存档管理器。
 * 负责将完整游戏状态序列化/反序列化为 JSON 文件。
 */
public final class SaveManager {
    private static final int VERSION = 1;
    private static final String SAVES_DIR = "saves";
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private SaveManager() {}

    // ─── 公共 API ─────────────────────────────────────────────

    /** 保存游戏到指定槽位（null 或空串 → autosave）。 */
    public static Path save(GameSimulation sim, int playerId, String slotName) throws IOException {
        Path dir = savesDir();
        Files.createDirectories(dir);
        String file = sanitize(slotName);
        Map<String, Object> data = serialize(sim, playerId);
        Path path = dir.resolve(file);
        MAPPER.writeValue(path.toFile(), data);
        return path;
    }

    /** 从指定槽位加载游戏。 */
    public static LoadedGame load(String slotName) throws IOException {
        Path dir = savesDir();
        String file = sanitize(slotName);
        Path path = dir.resolve(file);
        if (!Files.exists(path)) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> data = MAPPER.readValue(path.toFile(), Map.class);
        return deserialize(data);
    }

    /** 列出所有存档。 */
    public static List<SaveInfo> listSaves() {
        Path dir = savesDir();
        if (!Files.exists(dir)) {
            return List.of();
        }
        File[] files = dir.toFile().listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) {
            return List.of();
        }
        List<SaveInfo> result = new ArrayList<>();
        Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        for (File f : files) {
            SaveInfo info = new SaveInfo();
            info.file = f.getName();
            info.name = f.getName().replace(".json", "");
            info.mtime = f.lastModified();
            info.size = f.length();
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = MAPPER.readValue(f, Map.class);
                List<Object> dateArr = (List<Object>) data.get("date");
                if (dateArr != null && dateArr.size() >= 3) {
                    info.date = ((Number) dateArr.get(0)).intValue() + "-"
                            + String.format("%02d", ((Number) dateArr.get(1)).intValue()) + "-"
                            + String.format("%02d", ((Number) dateArr.get(2)).intValue());
                }
                Object pid = data.get("playerId");
                if (pid instanceof Number) {
                    info.playerId = ((Number) pid).intValue();
                }
            } catch (Exception ignored) {
                // 解析失败不影响列表
            }
            result.add(info);
        }
        return result;
    }

    /** 删除存档（不允许删除 autosave）。 */
    public static boolean deleteSave(String slotName) {
        String file = sanitize(slotName);
        if ("autosave.json".equals(file)) {
            return false;
        }
        Path path = savesDir().resolve(file);
        try {
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            return false;
        }
    }

    // ─── 序列化 ─────────────────────────────────────────────

    private static Map<String, Object> serialize(GameSimulation sim, int playerId) {
        World w = sim.world;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("version", VERSION);
        data.put("date", List.of(w.date.year(), w.date.month(), w.date.day()));
        data.put("tick", w.tick);
        data.put("playerId", playerId);
        data.put("playerIds", new ArrayList<>(sim.playerIds));
        data.put("world", serializeWorld(w));
        data.put("wars", serializeWars(sim.wars));
        data.put("sieges", serializeSieges(sim.sieges));
        data.put("events", serializeEvents(sim.events));
        data.put("diplomacy", sim.diplomacy.saveState());
        data.put("factions", sim.factions.saveState());
        data.put("schemes", sim.schemes.saveState());
        data.put("councils", sim.councils.saveState());
        data.put("buildings", sim.buildings.saveState());
        data.put("storylines", serializeStorylines(sim.storylines));
        data.put("realmLaws", serializeRealmLaws(sim.realmLaws));
        return data;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> serializeWorld(World w) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("nextChar", w.nextChar);
        m.put("nextDynasty", w.nextDynasty);
        m.put("nextTitle", w.nextTitle);
        m.put("nextCounty", w.nextCounty);
        m.put("log", new ArrayList<>(w.log));

        // 角色
        List<Map<String, Object>> chars = new ArrayList<>();
        for (Character c : w.characters.values()) {
            Map<String, Object> cm = new LinkedHashMap<>();
            cm.put("id", c.id);
            cm.put("name", c.name);
            cm.put("dynasty", c.dynasty);
            cm.put("gender", c.gender.name());
            cm.put("birth", dateToList(c.birth));
            cm.put("death", c.death != null ? dateToList(c.death) : null);
            cm.put("life", c.life.name());
            cm.put("baseAttrs", List.of(c.baseAttrs.diplomacy(), c.baseAttrs.martial(),
                    c.baseAttrs.stewardship(), c.baseAttrs.intrigue(),
                    c.baseAttrs.learning(), c.baseAttrs.prowess()));
            cm.put("traits", new ArrayList<>(c.traits));
            cm.put("gold", c.gold);
            cm.put("prestige", c.prestige);
            cm.put("piety", c.piety);
            cm.put("stress", c.stress);
            cm.put("health", c.health);
            cm.put("fertility", c.fertility);
            cm.put("father", c.father);
            cm.put("mother", c.mother);
            cm.put("spouses", new ArrayList<>(c.spouses));
            cm.put("children", new ArrayList<>(c.children));
            cm.put("heldTitles", new ArrayList<>(c.heldTitles));
            cm.put("primaryTitle", c.primaryTitle);
            cm.put("isRuler", c.isRuler);
            cm.put("employer", c.employer);
            cm.put("level", c.level);
            cm.put("xp", c.xp);
            // 意见缓存只保存非零项
            Map<String, Integer> op = new HashMap<>();
            for (var e : c.opinionCache.entrySet()) {
                if (e.getValue() != 0) {
                    op.put(String.valueOf(e.getKey()), e.getValue());
                }
            }
            cm.put("opinionCache", op);
            chars.add(cm);
        }
        m.put("characters", chars);

        // 王朝
        List<Map<String, Object>> dyns = new ArrayList<>();
        for (Dynasty d : w.dynasties.values()) {
            Map<String, Object> dm = new LinkedHashMap<>();
            dm.put("id", d.id);
            dm.put("name", d.name);
            dm.put("head", d.head);
            dm.put("founder", d.founder);
            dm.put("members", new ArrayList<>(d.members));
            dm.put("colorR", d.colorR);
            dm.put("colorG", d.colorG);
            dm.put("colorB", d.colorB);
            dm.put("motto", d.motto);
            dm.put("prestige", d.prestige);
            dyns.add(dm);
        }
        m.put("dynasties", dyns);

        // 头衔
        List<Map<String, Object>> titles = new ArrayList<>();
        for (Title t : w.titles.values()) {
            Map<String, Object> tm = new LinkedHashMap<>();
            tm.put("id", t.id);
            tm.put("name", t.name);
            tm.put("tier", t.tier.name());
            tm.put("holder", t.holder);
            tm.put("deJureLiege", t.deJureLiege);
            tm.put("deFactoLiege", t.deFactoLiege);
            tm.put("deJureVassals", new ArrayList<>(t.deJureVassals));
            tm.put("deFactoVassals", new ArrayList<>(t.deFactoVassals));
            tm.put("capital", t.capital);
            tm.put("counties", new ArrayList<>(t.counties));
            tm.put("creationCost", t.creationCost);
            tm.put("destroyable", t.destroyable);
            if (t.realmLaw != null) {
                tm.put("realmLaw", List.of(
                        t.realmLaw.succession().name(),
                        t.realmLaw.crownAuthority().name(),
                        t.realmLaw.genderLaw().name(),
                        t.realmLaw.partitionEnabled()));
            }
            titles.add(tm);
        }
        m.put("titles", titles);

        // 地图（伯爵领 + 连接）
        List<Map<String, Object>> counties = new ArrayList<>();
        for (County c : w.map.list()) {
            Map<String, Object> cm = new LinkedHashMap<>();
            cm.put("id", c.id);
            cm.put("name", c.name);
            cm.put("terrain", c.terrain.name());
            cm.put("key", c.key);
            cm.put("development", c.development);
            cm.put("control", c.control);
            cm.put("prosperity", c.prosperity);
            cm.put("ownerTitle", c.ownerTitle);
            cm.put("holder", c.holder);
            cm.put("fortLevel", c.fortLevel);
            cm.put("buildings", new ArrayList<>(c.buildings));
            cm.put("levies", c.levies);
            cm.put("tax", c.tax);
            cm.put("neighbors", new ArrayList<>(c.neighbors));
            cm.put("tradeRouteProtected", c.tradeRouteProtected);
            cm.put("tradeRouteProtectionLevel", c.tradeRouteProtectionLevel);
            cm.put("hasPort", c.hasPort);
            cm.put("portLevel", c.portLevel);
            cm.put("portIncome", c.portIncome);
            cm.put("tradeRouteMaintenanceLevel", c.tradeRouteMaintenanceLevel);
            cm.put("tradeRouteUpgradeCost", c.tradeRouteUpgradeCost);
            counties.add(cm);
        }
        m.put("counties", counties);

        // 贸易路线
        List<Map<String, Object>> routes = new ArrayList<>();
        for (TradeRoute r : w.tradeRoutes) {
            routes.add(Map.of("from", r.from(), "to", r.to(),
                    "exchangeRate", r.exchangeRate(), "tradeVolume", r.tradeVolume()));
        }
        m.put("tradeRoutes", routes);
        m.put("exchangeRates", new HashMap<>(w.exchangeRates));

        return m;
    }

    private static Map<String, Object> serializeWars(WarManager wm) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("nextWar", wm.nextWar);
        m.put("nextArmy", wm.nextArmy);
        List<Map<String, Object>> warList = new ArrayList<>();
        for (War w : wm.wars.values()) {
            Map<String, Object> wm2 = new LinkedHashMap<>();
            wm2.put("id", w.id);
            wm2.put("name", w.name);
            wm2.put("cb", w.cb.name());
            wm2.put("attackerPrimary", w.attackerPrimary);
            wm2.put("defenderPrimary", w.defenderPrimary);
            wm2.put("start", dateToList(w.start));
            wm2.put("warscore", w.warscore);
            wm2.put("active", w.active);
            wm2.put("result", w.result.name());
            wm2.put("targetTitle", w.targetTitle);
            List<Map<String, Object>> parts = new ArrayList<>();
            for (WarParticipant p : w.participants) {
                parts.add(Map.of("character", p.character, "isAttacker", p.isAttacker,
                        "joined", dateToList(p.joined)));
            }
            wm2.put("participants", parts);
            warList.add(wm2);
        }
        m.put("wars", warList);
        List<Map<String, Object>> armyList = new ArrayList<>();
        for (Army a : wm.armies.values()) {
            Map<String, Object> am = new LinkedHashMap<>();
            am.put("id", a.id);
            am.put("owner", a.owner);
            am.put("commander", a.commander);
            am.put("name", a.name);
            am.put("location", a.location);
            am.put("status", a.status.name());
            am.put("supply", a.supply);
            am.put("morale", a.morale);
            am.put("path", new ArrayList<>(a.path));
            List<Map<String, Object>> stacks = new ArrayList<>();
            for (UnitStack s : a.stacks) {
                stacks.add(Map.of("type", s.unitType.name(), "men", s.men, "maxMen", s.maxMen));
            }
            am.put("stacks", stacks);
            armyList.add(am);
        }
        m.put("armies", armyList);
        return m;
    }

    private static Map<String, Object> serializeSieges(SiegeManager sm) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("nextId", sm.nextId);
        List<Map<String, Object>> sl = new ArrayList<>();
        for (Siege s : sm.sieges.values()) {
            Map<String, Object> sm2 = new LinkedHashMap<>();
            sm2.put("id", s.id);
            sm2.put("county", s.county);
            sm2.put("attackerArmy", s.attackerArmy);
            sm2.put("attacker", s.attacker);
            sm2.put("defender", s.defender);
            sm2.put("progress", s.progress);
            sm2.put("fortLevel", s.fortLevel);
            sm2.put("garrison", s.garrison);
            sm2.put("started", dateToList(s.started));
            sm2.put("active", s.active);
            sl.add(sm2);
        }
        m.put("sieges", sl);
        return m;
    }

    private static Map<String, Object> serializeEvents(com.ckgame.events.EventEngine ee) {
        Map<String, Object> m = new LinkedHashMap<>();
        List<Map<String, Object>> pending = new ArrayList<>();
        for (EventInstance inst : ee.pending) {
            Map<String, Object> im = new LinkedHashMap<>();
            im.put("eventId", inst.eventId);
            im.put("character", inst.character);
            im.put("title", inst.title);
            im.put("description", inst.description);
            List<Map<String, Object>> choices = new ArrayList<>();
            for (EventChoice ch : inst.choices) {
                Map<String, Object> cm = new LinkedHashMap<>();
                cm.put("id", ch.id);
                cm.put("text", ch.text);
                cm.put("aiWeight", ch.aiWeight);
                List<Map<String, Object>> effects = new ArrayList<>();
                for (Effect ef : ch.effects) {
                    effects.add(Map.of("kind", ef.kind, "amount", ef.amount, "text", ef.text));
                }
                cm.put("effects", effects);
                choices.add(cm);
            }
            im.put("choices", choices);
            pending.add(im);
        }
        m.put("pending", pending);
        m.put("history", new ArrayList<>(ee.history));
        return m;
    }

    private static Map<String, Object> serializeStorylines(com.ckgame.events.StorylineSystem ss) {
        Map<String, Object> m = new LinkedHashMap<>();
        List<Map<String, Object>> sl = new ArrayList<>();
        for (Storyline s : ss.storylines) {
            Map<String, Object> sm = new LinkedHashMap<>();
            sm.put("id", s.id);
            sm.put("title", s.title);
            sm.put("description", s.description);
            sm.put("characterId", s.characterId);
            sm.put("status", s.status.name());
            sm.put("currentStage", s.currentStage);
            sm.put("startYear", s.startYear);
            sm.put("endYear", s.endYear);
            sm.put("tags", new ArrayList<>(s.tags));
            List<Map<String, Object>> stages = new ArrayList<>();
            for (StorylineStage st : s.stages) {
                Map<String, Object> stm = new LinkedHashMap<>();
                stm.put("stageId", st.stageId);
                stm.put("title", st.title);
                stm.put("description", st.description);
                stm.put("eventIds", new ArrayList<>(st.eventIds));
                stm.put("nextStage", st.nextStage);
                stm.put("requiresCondition", st.requiresCondition);
                stages.add(stm);
            }
            sm.put("stages", stages);
            sl.add(sm);
        }
        m.put("storylines", sl);
        Map<String, Object> apc = new HashMap<>();
        for (var e : ss.activePerCharacter.entrySet()) {
            apc.put(String.valueOf(e.getKey()), e.getValue().id);
        }
        m.put("activePerCharacter", apc);
        return m;
    }

    private static Map<String, Object> serializeRealmLaws(Map<Integer, RealmLaw> laws) {
        Map<String, Object> m = new HashMap<>();
        for (var e : laws.entrySet()) {
            RealmLaw l = e.getValue();
            m.put(String.valueOf(e.getKey()), List.of(
                    l.succession().name(), l.crownAuthority().name(),
                    l.genderLaw().name(), l.partitionEnabled()));
        }
        return m;
    }

    // ─── 反序列化 ─────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private static LoadedGame deserialize(Map<String, Object> data) {
        LoadedGame lg = new LoadedGame();

        // 构建 World
        World world = new World();
        Map<String, Object> wm = (Map<String, Object>) data.get("world");
        List<Object> dateArr = (List<Object>) data.get("date");
        world.date = new GameDate(((Number) dateArr.get(0)).intValue(),
                ((Number) dateArr.get(1)).intValue(),
                ((Number) dateArr.get(2)).intValue());
        world.tick = ((Number) data.get("tick")).intValue();
        world.nextChar = ((Number) wm.get("nextChar")).intValue();
        world.nextDynasty = ((Number) wm.get("nextDynasty")).intValue();
        world.nextTitle = ((Number) wm.get("nextTitle")).intValue();
        world.nextCounty = ((Number) wm.get("nextCounty")).intValue();
        world.log.addAll((List<String>) wm.get("log"));

        // 角色
        List<Map<String, Object>> chars = (List<Map<String, Object>>) wm.get("characters");
        for (Map<String, Object> cm : chars) {
            int id = ((Number) cm.get("id")).intValue();
            String name = (String) cm.get("name");
            int dynasty = ((Number) cm.get("dynasty")).intValue();
            Gender gender = Gender.valueOf((String) cm.get("gender"));
            List<Object> birthArr = (List<Object>) cm.get("birth");
            GameDate birth = new GameDate(((Number) birthArr.get(0)).intValue(),
                    ((Number) birthArr.get(1)).intValue(), ((Number) birthArr.get(2)).intValue());
            Character c = new Character(id, name, dynasty, gender, birth);
            List<Object> deathArr = (List<Object>) cm.get("death");
            if (deathArr != null) {
                c.death = new GameDate(((Number) deathArr.get(0)).intValue(),
                        ((Number) deathArr.get(1)).intValue(), ((Number) deathArr.get(2)).intValue());
            }
            c.life = LifeState.valueOf((String) cm.get("life"));
            List<Number> attrs = (List<Number>) cm.get("baseAttrs");
            c.baseAttrs = new AttributeSet(attrs.get(0).intValue(), attrs.get(1).intValue(),
                    attrs.get(2).intValue(), attrs.get(3).intValue(),
                    attrs.get(4).intValue(), attrs.get(5).intValue());
            c.traits.addAll((List<Integer>) cm.get("traits"));
            c.gold = ((Number) cm.get("gold")).doubleValue();
            c.prestige = ((Number) cm.get("prestige")).doubleValue();
            c.piety = ((Number) cm.get("piety")).doubleValue();
            c.stress = ((Number) cm.get("stress")).intValue();
            c.health = ((Number) cm.get("health")).doubleValue();
            c.fertility = ((Number) cm.get("fertility")).doubleValue();
            c.father = ((Number) cm.get("father")).intValue();
            c.mother = ((Number) cm.get("mother")).intValue();
            c.spouses.addAll((List<Integer>) cm.get("spouses"));
            c.children.addAll((List<Integer>) cm.get("children"));
            c.heldTitles.addAll((List<Integer>) cm.get("heldTitles"));
            c.primaryTitle = ((Number) cm.get("primaryTitle")).intValue();
            c.isRuler = Boolean.TRUE.equals(cm.get("isRuler"));
            c.employer = ((Number) cm.get("employer")).intValue();
            c.level = ((Number) cm.get("level")).intValue();
            c.xp = ((Number) cm.get("xp")).intValue();
            Map<String, Object> op = (Map<String, Object>) cm.get("opinionCache");
            if (op != null) {
                for (var e : op.entrySet()) {
                    c.opinionCache.put(Integer.parseInt(e.getKey()), ((Number) e.getValue()).intValue());
                }
            }
            world.characters.put(id, c);
        }

        // 王朝
        List<Map<String, Object>> dyns = (List<Map<String, Object>>) wm.get("dynasties");
        for (Map<String, Object> dm : dyns) {
            int id = ((Number) dm.get("id")).intValue();
            Dynasty d = new Dynasty(id, (String) dm.get("name"));
            d.head = ((Number) dm.get("head")).intValue();
            d.founder = ((Number) dm.get("founder")).intValue();
            d.members.addAll((List<Integer>) dm.get("members"));
            d.colorR = ((Number) dm.get("colorR")).intValue();
            d.colorG = ((Number) dm.get("colorG")).intValue();
            d.colorB = ((Number) dm.get("colorB")).intValue();
            d.motto = (String) dm.getOrDefault("motto", "");
            d.prestige = ((Number) dm.get("prestige")).doubleValue();
            world.dynasties.put(id, d);
        }

        // 伯爵领（先创建，再连接邻居）
        List<Map<String, Object>> counties = (List<Map<String, Object>>) wm.get("counties");
        for (Map<String, Object> cm : counties) {
            int id = ((Number) cm.get("id")).intValue();
            String name = (String) cm.get("name");
            Terrain terrain = Terrain.valueOf((String) cm.get("terrain"));
            County c = new County(id, name, terrain);
            c.key = (String) cm.getOrDefault("key", "");
            c.development = ((Number) cm.get("development")).intValue();
            c.control = ((Number) cm.get("control")).doubleValue();
            c.prosperity = ((Number) cm.getOrDefault("prosperity", 50.0)).doubleValue();
            c.ownerTitle = ((Number) cm.get("ownerTitle")).intValue();
            c.holder = ((Number) cm.get("holder")).intValue();
            c.fortLevel = ((Number) cm.get("fortLevel")).intValue();
            c.buildings.addAll((List<String>) cm.get("buildings"));
            c.levies = ((Number) cm.get("levies")).intValue();
            c.tax = ((Number) cm.get("tax")).doubleValue();
            c.tradeRouteProtected = Boolean.TRUE.equals(cm.get("tradeRouteProtected"));
            c.tradeRouteProtectionLevel = ((Number) cm.getOrDefault("tradeRouteProtectionLevel", 0)).intValue();
            c.hasPort = Boolean.TRUE.equals(cm.get("hasPort"));
            c.portLevel = ((Number) cm.getOrDefault("portLevel", 0)).intValue();
            c.portIncome = ((Number) cm.getOrDefault("portIncome", 0.0)).doubleValue();
            c.tradeRouteMaintenanceLevel = ((Number) cm.getOrDefault("tradeRouteMaintenanceLevel", 0)).intValue();
            c.tradeRouteUpgradeCost = ((Number) cm.getOrDefault("tradeRouteUpgradeCost", 0.0)).doubleValue();
            world.map.insert(c);
        }
        // 连接邻居
        for (Map<String, Object> cm : counties) {
            int id = ((Number) cm.get("id")).intValue();
            List<Number> neighbors = (List<Number>) cm.get("neighbors");
            for (Number n : neighbors) {
                world.map.connect(id, n.intValue());
            }
        }

        // 头衔
        List<Map<String, Object>> titleList = (List<Map<String, Object>>) wm.get("titles");
        for (Map<String, Object> tm : titleList) {
            int id = ((Number) tm.get("id")).intValue();
            Title t = new Title(id, (String) tm.get("name"), TitleTier.valueOf((String) tm.get("tier")));
            t.holder = ((Number) tm.get("holder")).intValue();
            t.deJureLiege = ((Number) tm.get("deJureLiege")).intValue();
            t.deFactoLiege = ((Number) tm.get("deFactoLiege")).intValue();
            t.deJureVassals.addAll((List<Integer>) tm.get("deJureVassals"));
            t.deFactoVassals.addAll((List<Integer>) tm.get("deFactoVassals"));
            t.capital = ((Number) tm.get("capital")).intValue();
            t.counties.addAll((List<Integer>) tm.get("counties"));
            t.creationCost = ((Number) tm.getOrDefault("creationCost", 0.0)).doubleValue();
            t.destroyable = Boolean.TRUE.equals(tm.get("destroyable"));
            List<Object> rl = (List<Object>) tm.get("realmLaw");
            if (rl != null && rl.size() >= 4) {
                t.realmLaw = new RealmLaw(
                        SuccessionLaw.valueOf((String) rl.get(0)),
                        CrownAuthority.valueOf((String) rl.get(1)),
                        GenderLaw.valueOf((String) rl.get(2)),
                        Boolean.TRUE.equals(rl.get(3)));
            }
            world.titles.put(id, t);
        }

        // 贸易路线
        List<Map<String, Object>> routes = (List<Map<String, Object>>) wm.get("tradeRoutes");
        if (routes != null) {
            for (Map<String, Object> rm : routes) {
                world.tradeRoutes.add(new TradeRoute(
                        (String) rm.get("from"), (String) rm.get("to"),
                        ((Number) rm.get("exchangeRate")).doubleValue(),
                        ((Number) rm.get("tradeVolume")).doubleValue()));
            }
        }
        Map<String, Object> er = (Map<String, Object>) wm.get("exchangeRates");
        if (er != null) {
            for (var e : er.entrySet()) {
                world.exchangeRates.put(e.getKey(), ((Number) e.getValue()).doubleValue());
            }
        }

        // 战争管理器
        WarManager wars = new WarManager();
        Map<String, Object> warsData = (Map<String, Object>) data.get("wars");
        wars.nextWar = ((Number) warsData.get("nextWar")).intValue();
        wars.nextArmy = ((Number) warsData.get("nextArmy")).intValue();
        List<Map<String, Object>> warList = (List<Map<String, Object>>) warsData.get("wars");
        for (Map<String, Object> w2 : warList) {
            int wid = ((Number) w2.get("id")).intValue();
            List<Object> startDate = (List<Object>) w2.get("start");
            GameDate wStart = new GameDate(((Number) startDate.get(0)).intValue(),
                    ((Number) startDate.get(1)).intValue(), ((Number) startDate.get(2)).intValue());
            War w = new War(wid, (String) w2.get("name"),
                    CasusBelli.valueOf((String) w2.get("cb")),
                    ((Number) w2.get("attackerPrimary")).intValue(),
                    ((Number) w2.get("defenderPrimary")).intValue(), wStart);
            w.warscore = ((Number) w2.get("warscore")).intValue();
            w.active = Boolean.TRUE.equals(w2.get("active"));
            w.result = WarResult.valueOf((String) w2.get("result"));
            w.targetTitle = ((Number) w2.get("targetTitle")).intValue();
            List<Map<String, Object>> parts = (List<Map<String, Object>>) w2.get("participants");
            for (Map<String, Object> pm : parts) {
                List<Object> jd = (List<Object>) pm.get("joined");
                GameDate joined = new GameDate(((Number) jd.get(0)).intValue(),
                        ((Number) jd.get(1)).intValue(), ((Number) jd.get(2)).intValue());
                WarParticipant p = new WarParticipant(((Number) pm.get("character")).intValue(),
                        Boolean.TRUE.equals(pm.get("isAttacker")), joined);
                w.participants.add(p);
            }
            wars.wars.put(wid, w);
        }
        List<Map<String, Object>> armyList = (List<Map<String, Object>>) warsData.get("armies");
        for (Map<String, Object> am : armyList) {
            int aid = ((Number) am.get("id")).intValue();
            Army a = new Army(aid, ((Number) am.get("owner")).intValue(),
                    (String) am.get("name"), ((Number) am.get("location")).intValue(),
                    ((Number) am.get("commander")).intValue());
            a.status = ArmyStatus.valueOf((String) am.get("status"));
            a.supply = ((Number) am.get("supply")).intValue();
            a.morale = ((Number) am.get("morale")).intValue();
            List<Number> path = (List<Number>) am.get("path");
            if (path != null) {
                for (Number n : path) {
                    a.path.add(n.intValue());
                }
            }
            List<Map<String, Object>> stacks = (List<Map<String, Object>>) am.get("stacks");
            if (stacks != null) {
                for (Map<String, Object> sm : stacks) {
                    a.stacks.add(new UnitStack(UnitType.valueOf((String) sm.get("type")),
                            ((Number) sm.get("men")).intValue(),
                            ((Number) sm.get("maxMen")).intValue()));
                }
            }
            wars.armies.put(aid, a);
        }

        // 围城管理器
        SiegeManager sieges = new SiegeManager();
        Map<String, Object> siegesData = (Map<String, Object>) data.get("sieges");
        sieges.nextId = ((Number) siegesData.get("nextId")).intValue();
        List<Map<String, Object>> siegeList = (List<Map<String, Object>>) siegesData.get("sieges");
        for (Map<String, Object> sm : siegeList) {
            int sid = ((Number) sm.get("id")).intValue();
            Siege s = new Siege(sid, ((Number) sm.get("county")).intValue(),
                    ((Number) sm.get("attackerArmy")).intValue(),
                    ((Number) sm.get("attacker")).intValue(),
                    ((Number) sm.get("defender")).intValue());
            s.progress = ((Number) sm.get("progress")).doubleValue();
            s.fortLevel = ((Number) sm.get("fortLevel")).intValue();
            s.garrison = ((Number) sm.get("garrison")).intValue();
            List<Object> sd = (List<Object>) sm.get("started");
            if (sd != null) {
                s.started = new GameDate(((Number) sd.get(0)).intValue(),
                        ((Number) sd.get(1)).intValue(), ((Number) sd.get(2)).intValue());
            }
            s.active = Boolean.TRUE.equals(sm.get("active"));
            sieges.sieges.put(sid, s);
        }

        // 事件引擎
        com.ckgame.events.EventEngine events = new com.ckgame.events.EventEngine();
        Map<String, Object> eventsData = (Map<String, Object>) data.get("events");
        List<Map<String, Object>> pendingEv = (List<Map<String, Object>>) eventsData.get("pending");
        if (pendingEv != null) {
            for (Map<String, Object> im : pendingEv) {
                List<Map<String, Object>> choiceMaps = (List<Map<String, Object>>) im.get("choices");
                List<EventChoice> choices = new ArrayList<>();
                for (Map<String, Object> chm : choiceMaps) {
                    List<Map<String, Object>> efMaps = (List<Map<String, Object>>) chm.get("effects");
                    List<Effect> effects = new ArrayList<>();
                    for (Map<String, Object> efm : efMaps) {
                        effects.add(new Effect((String) efm.get("kind"),
                                ((Number) efm.get("amount")).doubleValue(),
                                (String) efm.getOrDefault("text", "")));
                    }
                    choices.add(new EventChoice(((Number) chm.get("id")).intValue(),
                            (String) chm.get("text"), effects,
                            ((Number) chm.get("aiWeight")).doubleValue()));
                }
                events.pending.add(new EventInstance(
                        ((Number) im.get("eventId")).intValue(),
                        ((Number) im.get("character")).intValue(),
                        (String) im.get("title"),
                        (String) im.get("description"),
                        choices));
            }
        }
        List<String> history = (List<String>) eventsData.get("history");
        if (history != null) {
            events.history.addAll(history);
        }

        // 外交
        Diplomacy diplomacy = new Diplomacy();
        Map<String, Object> dipData = (Map<String, Object>) data.get("diplomacy");
        if (dipData != null) {
            diplomacy.loadState(dipData);
        }

        // 派系
        FactionManager factions = new FactionManager(null);
        Map<String, Object> facData = (Map<String, Object>) data.get("factions");
        if (facData != null) {
            factions.loadState(facData);
        }

        // 阴谋
        SchemeManager schemes = new SchemeManager(null);
        Map<String, Object> schData = (Map<String, Object>) data.get("schemes");
        if (schData != null) {
            schemes.loadState(schData);
        }

        // 内阁
        CouncilManager councils = new CouncilManager(null);
        Map<String, Object> cncData = (Map<String, Object>) data.get("councils");
        if (cncData != null) {
            councils.loadState(cncData);
        }

        // 建筑
        BuildingSystem buildings = new BuildingSystem();
        Map<String, Object> bldData = (Map<String, Object>) data.get("buildings");
        if (bldData != null) {
            buildings.loadState(bldData);
        }

        // 剧情线
        com.ckgame.events.StorylineSystem storylines = new com.ckgame.events.StorylineSystem();
        Map<String, Object> slData = (Map<String, Object>) data.get("storylines");
        if (slData != null) {
            List<Map<String, Object>> slList = (List<Map<String, Object>>) slData.get("storylines");
            if (slList != null) {
                for (Map<String, Object> sm : slList) {
                    Storyline s = new Storyline(((Number) sm.get("id")).intValue(),
                            (String) sm.get("title"), (String) sm.get("description"),
                            ((Number) sm.get("characterId")).intValue());
                    s.status = StorylineStatus.valueOf((String) sm.get("status"));
                    s.currentStage = ((Number) sm.get("currentStage")).intValue();
                    s.startYear = ((Number) sm.get("startYear")).intValue();
                    s.endYear = ((Number) sm.get("endYear")).intValue();
                    s.tags.addAll((List<String>) sm.get("tags"));
                    List<Map<String, Object>> stages = (List<Map<String, Object>>) sm.get("stages");
                    if (stages != null) {
                        for (Map<String, Object> stm : stages) {
                            Object ns = stm.get("nextStage");
                            s.stages.add(new StorylineStage(
                                    ((Number) stm.get("stageId")).intValue(),
                                    (String) stm.get("title"),
                                    (String) stm.get("description"),
                                    (List<Integer>) stm.get("eventIds"),
                                    ns instanceof Number ? ((Number) ns).intValue() : null,
                                    (String) stm.get("requiresCondition")));
                        }
                    }
                    storylines.storylines.add(s);
                }
            }
            Map<String, Object> apc = (Map<String, Object>) slData.get("activePerCharacter");
            if (apc != null) {
                for (var e : apc.entrySet()) {
                    int charId = Integer.parseInt(e.getKey());
                    int slId = ((Number) e.getValue()).intValue();
                    for (Storyline sl : storylines.storylines) {
                        if (sl.id == slId) {
                            storylines.activePerCharacter.put(charId, sl);
                            break;
                        }
                    }
                }
            }
        }

        // 领域法
        Map<Integer, RealmLaw> realmLaws = new HashMap<>();
        Map<String, Object> rlData = (Map<String, Object>) data.get("realmLaws");
        if (rlData != null) {
            for (var e : rlData.entrySet()) {
                int rid = Integer.parseInt(e.getKey());
                List<Object> rl = (List<Object>) e.getValue();
                realmLaws.put(rid, new RealmLaw(
                        SuccessionLaw.valueOf((String) rl.get(0)),
                        CrownAuthority.valueOf((String) rl.get(1)),
                        GenderLaw.valueOf((String) rl.get(2)),
                        Boolean.TRUE.equals(rl.get(3))));
            }
        }

        // 玩家 ID
        Set<Integer> playerIds = new HashSet<>();
        Object pidObj = data.get("playerId");
        if (pidObj instanceof Number) {
            playerIds.add(((Number) pidObj).intValue());
        }
        List<Number> pids = (List<Number>) data.get("playerIds");
        if (pids != null) {
            for (Number n : pids) {
                playerIds.add(n.intValue());
            }
        }

        // 组装
        GameSimulation sim = GameSimulation.reconstruct(world, wars, sieges, events,
                factions, schemes, diplomacy, councils, buildings, storylines,
                realmLaws, playerIds);
        lg.sim = sim;
        lg.playerId = playerIds.isEmpty() ? Constants.NONE_ID : playerIds.iterator().next();
        return lg;
    }

    // ─── 工具方法 ─────────────────────────────────────────────

    private static Path savesDir() {
        return Paths.get(SAVES_DIR);
    }

    private static String sanitize(String name) {
        if (name == null || name.isBlank()) {
            return "autosave.json";
        }
        String safe = name.replaceAll("[^a-zA-Z0-9_\\u4e00-\\u9fff\\-]", "_")
                .replaceAll("\\s+", "_");
        if (safe.length() > 48) {
            safe = safe.substring(0, 48);
        }
        return safe + ".json";
    }

    private static List<Integer> dateToList(GameDate d) {
        return d != null ? List.of(d.year(), d.month(), d.day()) : List.of(1066, 1, 1);
    }

    // ─── 数据类 ─────────────────────────────────────────────

    public static final class SaveInfo {
        public String name;
        public String file;
        public String date;
        public int playerId = Constants.NONE_ID;
        public long mtime;
        public long size;
    }

    public static final class LoadedGame {
        public GameSimulation sim;
        public int playerId;
    }
}
