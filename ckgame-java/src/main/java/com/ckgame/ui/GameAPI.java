package com.ckgame.ui;

import com.ckgame.core.Constants;
import com.ckgame.core.balance.Balance;
import com.ckgame.core.stats.AttributeSet;
import com.ckgame.events.EventChoice;
import com.ckgame.events.EventInstance;
import com.ckgame.events.Storyline;
import com.ckgame.events.StorylineStage;
import com.ckgame.game.GameSimulation;
import com.ckgame.military.Army;
import com.ckgame.military.ArmyStatus;
import com.ckgame.military.Siege;
import com.ckgame.military.UnitType;
import com.ckgame.military.War;
import com.ckgame.military.WarParticipant;
import com.ckgame.military.WarResult;
import com.ckgame.politics.CasusBelli;
import com.ckgame.politics.Claim;
import com.ckgame.politics.Council;
import com.ckgame.politics.CouncilPosition;
import com.ckgame.politics.CouncilTask;
import com.ckgame.politics.DiplomacyFlags;
import com.ckgame.politics.Faction;
import com.ckgame.politics.Laws.CrownAuthority;
import com.ckgame.politics.Laws.GenderLaw;
import com.ckgame.politics.Laws.RealmLaw;
import com.ckgame.politics.Laws.SuccessionLaw;
import com.ckgame.politics.Scheme;
import com.ckgame.politics.SchemeKind;
import com.ckgame.politics.Treaty;
import com.ckgame.politics.TreatyKind;
import com.ckgame.world.Character;
import com.ckgame.world.County;
import com.ckgame.world.Dynasty;
import com.ckgame.world.Title;
import com.ckgame.world.World;
import com.ckgame.world.buildings.BuildingKind;
import com.ckgame.world.buildings.CountyBuilding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * 游戏 API 门面：在模拟引擎与 UI 之间提供 snapshot / action 接口。
 * 对应 Python 的 ck_engine.ui.api.GameAPI。
 */
public final class GameAPI {

    private static final double CHEAT_GOLD = 99999.0;
    private static final double CHEAT_PRESTIGE = 99999.0;
    private static final double CHEAT_PIETY = 99999.0;

    private GameSimulation sim;
    private int playerId;
    private int selectedCounty = -1;
    private int selectedArmy = -1;
    private final List<String> messages = new ArrayList<>();
    private boolean cheatMode = false;
    private boolean infiniteGoldMode = false;

    // ─────────────────── 构造 ───────────────────

    /** 新开局（默认场景）。 */
    public GameAPI() {
        this(null);
    }

    /** 新开局，指定场景 id 或场景文件路径。 */
    public GameAPI(String scenario) {
        this.sim = new GameSimulation(scenario);
        this.playerId = defaultPlayer();
        syncPlayer();
        messages.add("欢迎。点击地图省份查看详情，使用侧栏下达指令。");
    }

    /** 从已加载的模拟恢复（存档）。 */
    public GameAPI(GameSimulation sim, int playerId) {
        this.sim = sim;
        this.playerId = playerId;
        syncPlayer();
        messages.add("读档完成。");
    }

    // ─────────────────── 访问器 ───────────────────

    public GameSimulation simulation() { return sim; }
    public int playerId() { return playerId; }
    public void setPlayerId(int id) { this.playerId = id; }

    public List<String> getMessages() {
        int from = Math.max(0, messages.size() - 12);
        return new ArrayList<>(messages.subList(from, messages.size()));
    }

    // ─────────────────── snapshot ───────────────────

    public Map<String, Object> snapshot() {
        World w = sim.world;
        Character player = w.character(playerId);

        // ── counties ──
        List<Map<String, Object>> counties = new ArrayList<>();
        for (County county : w.map.list()) {
            Character holder = w.character(county.holder);
            String color = holderColor(county.holder);
            List<Integer> armiesHere = new ArrayList<>();
            for (Army a : sim.wars.armies.values()) {
                if (a.isActive() && a.location == county.id) {
                    armiesHere.add(a.id);
                }
            }
            Siege siege = sim.sieges.activeAt(county.id);
            Map<String, Object> cm = new LinkedHashMap<>();
            cm.put("id", county.id);
            cm.put("name", county.name);
            cm.put("terrain", county.terrain.name());
            cm.put("development", county.development);
            cm.put("devCap", county.terrain.developmentCap());
            cm.put("control", round1(county.control));
            cm.put("levies", county.monthlyLevies());
            cm.put("tax", round2(county.monthlyTax()));
            cm.put("fort", county.fortLevel);
            cm.put("buildings", countyBuildings(county.id));
            cm.put("holderId", county.holder == Constants.NONE_ID ? null : county.holder);
            cm.put("holderName", holder != null ? holder.name : "无主");
            cm.put("color", color);
            cm.put("neighbors", new ArrayList<>(county.neighbors));
            cm.put("armies", armiesHere);
            cm.put("siege", siege != null
                    ? Map.of("progress", round1(siege.progress),
                             "required", round1(siege.requiredProgress()),
                             "attacker", siege.attacker)
                    : null);
            cm.put("isPlayer", holder != null && holder.id == playerId);
            cm.put("hasPort", county.hasPort);
            cm.put("portLevel", county.portLevel);
            cm.put("portIncome", round1(county.portIncome));
            cm.put("tradeRouteProtected", county.tradeRouteProtected);
            cm.put("tradeRouteProtectionLevel", county.tradeRouteProtectionLevel);
            cm.put("tradeRouteMaintenanceLevel", county.tradeRouteMaintenanceLevel);
            cm.put("tradeRouteUpgradeCost", round1(county.tradeRouteUpgradeCost));
            counties.add(cm);
        }

        // ── armies ──
        List<Map<String, Object>> armies = new ArrayList<>();
        for (Army a : sim.wars.armies.values()) {
            if (!a.isActive()) continue;
            County loc = w.map.get(a.location);
            Character owner = w.character(a.owner);
            boolean inEnemy = false;
            if (loc != null) {
                for (War war : sim.wars.activeWars()) {
                    if (!war.involves(a.owner)) continue;
                    for (WarParticipant p : war.participants) {
                        if (war.isAttacker(p.character) != war.isAttacker(a.owner)
                                && p.character == loc.holder) {
                            inEnemy = true;
                            break;
                        }
                    }
                    if (inEnemy) break;
                }
            }
            Map<String, Object> am = new LinkedHashMap<>();
            am.put("id", a.id);
            am.put("name", a.name);
            am.put("ownerId", a.owner);
            am.put("ownerName", owner != null ? owner.name : "?");
            am.put("location", a.location);
            am.put("locationName", loc != null ? loc.name : "?");
            am.put("men", a.totalMen());
            am.put("status", a.status.name());
            am.put("morale", round1(a.morale));
            am.put("supply", round1(a.supply));
            am.put("supplyLow", a.supply < Balance.SUPPLY_LOW_THRESHOLD);
            am.put("inEnemy", inEnemy);
            am.put("isPlayer", a.owner == playerId);
            am.put("path", new ArrayList<>(a.path));
            armies.add(am);
        }

        // ── wars ──
        List<Map<String, Object>> wars = new ArrayList<>();
        for (War war : sim.wars.wars.values()) {
            Character atk = w.character(war.attackerPrimary);
            Character def = w.character(war.defenderPrimary);
            int months = war.active ? war.monthsElapsed(w.date) : 0;
            double atkExh = sim.diplomacy.warExhaustion().getOrDefault(war.attackerPrimary, 0.0);
            double defExh = sim.diplomacy.warExhaustion().getOrDefault(war.defenderPrimary, 0.0);
            boolean canWp = false;
            if (war.active && war.involves(playerId)) {
                canWp = war.canWhitePeace(w.date, atkExh, defExh)
                        || (months >= 12 || (months >= 6 && Math.abs(war.warscore) <= 40));
            }
            Map<String, Object> wm = new LinkedHashMap<>();
            wm.put("id", war.id);
            wm.put("name", war.name);
            wm.put("active", war.active);
            wm.put("warscore", war.warscore);
            wm.put("cb", war.cb.nameZh());
            wm.put("attacker", atk != null ? atk.name : "?");
            wm.put("defender", def != null ? def.name : "?");
            wm.put("involvesPlayer", war.involves(playerId));
            wm.put("months", months);
            wm.put("canWhitePeace", canWp);
            wars.add(wm);
        }

        // ── rulers ──
        List<Map<String, Object>> rulers = new ArrayList<>();
        for (Character r : w.rulers()) {
            AttributeSet attrs = w.effectiveAttrs(r.id);
            Title title = w.title(r.primaryTitle);
            Map<String, Object> rm = new LinkedHashMap<>();
            rm.put("id", r.id);
            rm.put("name", r.name);
            rm.put("title", title != null ? title.name : "无");
            rm.put("gold", round1(r.gold));
            rm.put("prestige", round1(r.prestige));
            rm.put("martial", attrs != null ? attrs.martial() : 0);
            rm.put("income", round1(w.monthlyIncomeOf(r.id)));
            rm.put("men", sim.wars.totalMenOf(r.id));
            rm.put("isPlayer", r.id == playerId);
            rulers.add(rm);
        }

        // ── factions targeting player ──
        List<Map<String, Object>> factions = new ArrayList<>();
        for (Faction f : sim.factions.factions().values()) {
            if (f.targetLiege != playerId) continue;
            Map<String, Object> fm = new LinkedHashMap<>();
            fm.put("id", f.id);
            fm.put("kind", f.kind.nameZh());
            fm.put("members", f.members.size());
            fm.put("power", round1(f.power));
            fm.put("discontent", round1(f.discontent));
            fm.put("ultimatum", f.ultimatumSent);
            factions.add(fm);
        }

        // ── player info ──
        Map<String, Object> playerInfo = null;
        if (player != null) {
            Title title = w.title(player.primaryTitle);
            AttributeSet attrs = w.effectiveAttrs(player.id);
            playerInfo = new LinkedHashMap<>();
            playerInfo.put("id", player.id);
            playerInfo.put("name", player.name);
            playerInfo.put("title", title != null ? title.name : "无");
            playerInfo.put("gold", round1(player.gold));
            playerInfo.put("prestige", round1(player.prestige));
            playerInfo.put("piety", round1(player.piety));
            playerInfo.put("stress", player.stress);
            playerInfo.put("health", round2(player.health));
            Map<String, Object> attrsMap = new LinkedHashMap<>();
            attrsMap.put("diplomacy", attrs != null ? attrs.diplomacy() : 0);
            attrsMap.put("martial", attrs != null ? attrs.martial() : 0);
            attrsMap.put("stewardship", attrs != null ? attrs.stewardship() : 0);
            attrsMap.put("intrigue", attrs != null ? attrs.intrigue() : 0);
            attrsMap.put("learning", attrs != null ? attrs.learning() : 0);
            attrsMap.put("prowess", attrs != null ? attrs.prowess() : 0);
            playerInfo.put("attrs", attrsMap);
            playerInfo.put("income", round1(w.monthlyIncomeOf(player.id)));
            playerInfo.put("men", sim.wars.totalMenOf(player.id));
            playerInfo.put("laws", playerLaws());
            playerInfo.put("level", player.level);
            playerInfo.put("xp", player.xp);
            playerInfo.put("xpToNext", player.xpToNextLevel());
        }

        // ── playable ──
        List<Map<String, Object>> playable = new ArrayList<>();
        for (Character r : w.rulers()) {
            Title t = w.title(r.primaryTitle);
            Map<String, Object> pm = new LinkedHashMap<>();
            pm.put("id", r.id);
            pm.put("name", r.name);
            pm.put("title", t != null ? t.name : "");
            playable.add(pm);
        }

        // ── pending events ──
        List<Map<String, Object>> pendingEvents = new ArrayList<>();
        for (EventInstance inst : sim.events.pending) {
            if (inst.character != playerId) continue;
            Map<String, Object> em = new LinkedHashMap<>();
            em.put("eventId", inst.eventId);
            em.put("title", inst.title);
            em.put("description", inst.description);
            List<Map<String, Object>> choices = new ArrayList<>();
            for (EventChoice ch : inst.choices) {
                Map<String, Object> chm = new LinkedHashMap<>();
                chm.put("id", ch.id);
                chm.put("text", ch.text);
                chm.put("aiWeight", ch.aiWeight);
                choices.add(chm);
            }
            em.put("choices", choices);
            pendingEvents.add(em);
        }

        // ── log ──
        List<String> log = w.log.size() > 30
                ? new ArrayList<>(w.log.subList(w.log.size() - 30, w.log.size()))
                : new ArrayList<>(w.log);

        // ── assemble ──
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("date", w.date.toString());
        snap.put("season", w.date.season().name());
        snap.put("tick", w.tick);
        snap.put("player", playerInfo);
        snap.put("player_war_exhaustion", round1(sim.diplomacy.warExhaustion().getOrDefault(playerId, 0.0)));
        snap.put("playable", playable);
        snap.put("counties", counties);
        snap.put("armies", armies);
        snap.put("wars", wars);
        snap.put("factions", factions);
        snap.put("rulers", rulers);
        snap.put("log", log);
        snap.put("messages", getMessages());
        snap.put("selected_county", selectedCounty);
        snap.put("selected_army", selectedArmy);
        snap.put("saves", listSaves());
        snap.put("cheat_mode", cheatMode);
        snap.put("infinite_gold_mode", infiniteGoldMode);
        snap.put("pending_events", pendingEvents);
        snap.put("player_schemes", playerSchemes());
        snap.put("player_council", playerCouncil());
        snap.put("player_claims", playerClaims());
        snap.put("treaties", playerTreaties());
        snap.put("characters", allCharacters());
        snap.put("scheme_types", schemeTypes());
        snap.put("council_positions", councilPositions());
        snap.put("council_tasks", councilTasks());
        snap.put("storylines", playerStorylines());
        snap.put("trade_routes", serializeTradeRoutes());
        snap.put("exchange_rates", new HashMap<>(w.exchangeRates));
        return snap;
    }

    // ─────────────────── action ───────────────────

    public Map<String, Object> action(Map<String, Object> payload) {
        String kind = (String) payload.get("action");
        try {
            switch (kind) {
                case "select_county" -> {
                    selectedCounty = intVal(payload, "county_id");
                    selectedArmy = -1;
                }
                case "select_army" -> {
                    selectedArmy = intVal(payload, "army_id");
                    selectedCounty = -1;
                }
                case "set_player" -> {
                    playerId = intVal(payload, "character_id");
                    syncPlayer();
                    selectedArmy = -1;
                    notify("切换玩家为 " + name(playerId));
                }
                case "advance" -> {
                    int days = intValOrDefault(payload, "days", 1);
                    days = Math.max(1, Math.min(365, days));
                    int prevChunk = sim.world.tick / 30;
                    syncPlayer();
                    sim.runDays(days);
                    int newChunk = sim.world.tick / 30;
                    notify("时间推进 " + days + " 天 -> " + sim.world.date);
                    if (prevChunk != newChunk) {
                        try { doSave(null); } catch (Exception ignored) { }
                    }
                }
                case "raise_army" -> doRaiseArmy(intVal(payload, "county_id"));
                case "move_army" -> doMoveArmy(intVal(payload, "army_id"), intVal(payload, "county_id"));
                case "disband_army" -> doDisbandArmy(intVal(payload, "army_id"));
                case "set_commander" -> doSetCommander(intVal(payload, "army_id"), intVal(payload, "character_id"));
                case "recruit_knights" -> doRecruitKnights();
                case "declare_war" -> doDeclareWar(intVal(payload, "target_id"));
                case "white_peace" -> doWhitePeace(intVal(payload, "war_id"));
                case "improve_relations" -> doImproveRelations(intVal(payload, "target_id"));
                case "form_alliance" -> doFormAlliance(intVal(payload, "target_id"));
                case "form_non_aggression" -> doFormNonAggression(intVal(payload, "target_id"));
                case "form_vassalage" -> doFormVassalage(intVal(payload, "target_id"));
                case "form_trade_agreement" -> doFormTradeAgreement(intVal(payload, "target_id"));
                case "form_intelligence_sharing" -> doFormIntelSharing(intVal(payload, "target_id"));
                case "arrange_marriage" -> doArrangeMarriage(intVal(payload, "target_id"));
                case "send_gift" -> doSendGift(intVal(payload, "target_id"), doubleValOrDefault(payload, "amount", 50));
                case "set_rival" -> doSetRival(intVal(payload, "target_id"));
                case "invite_to_court" -> doInviteToCourt(intVal(payload, "target_id"));
                case "host_feast_for" -> doHostFeastFor(intVal(payload, "target_id"));
                case "duel" -> doDuel(intVal(payload, "target_id"));
                case "start_scheme" -> doStartScheme((String) payload.get("scheme_kind"), intVal(payload, "target_id"));
                case "appoint_council" -> doAppointCouncil((String) payload.get("position"), intValOrDefault(payload, "character_id", Constants.NONE_ID));
                case "assign_council_task" -> doAssignCouncilTask((String) payload.get("position"), (String) payload.get("task"));
                case "grant_title" -> doGrantTitle(intVal(payload, "title_id"), intVal(payload, "target_id"));
                case "hold_feast" -> doHoldFeast();
                case "appease_faction" -> doAppeaseFaction(intVal(payload, "faction_id"));
                case "develop_county" -> doDevelopCounty(intVal(payload, "county_id"));
                case "upgrade_building" -> doUpgradeBuilding(intVal(payload, "county_id"), (String) payload.get("building_kind"));
                case "upgrade_port" -> doUpgradePort(intVal(payload, "county_id"));
                case "upgrade_trade_route_maintenance" -> doUpgradeTradeRouteMaintenance(intVal(payload, "county_id"));
                case "fabricate_claim" -> doFabricateClaim(intVal(payload, "county_id"));
                case "set_succession_law" -> doSetSuccessionLaw((String) payload.get("law"));
                case "set_crown_authority" -> doSetCrownAuthority(intValOrDefault(payload, "level", 0));
                case "set_gender_law" -> doSetGenderLaw((String) payload.get("law"));
                case "resolve_event" -> doResolveEvent(intVal(payload, "event_id"), intVal(payload, "choice_id"));
                case "save" -> doSave((String) payload.get("name"));
                case "load" -> doLoad((String) payload.get("name"));
                case "delete_save" -> doDeleteSave((String) payload.get("name"));
                case "new_game" -> doNewGame(payload.get("scenario"));
                case "toggle_cheat" -> {
                    cheatMode = !cheatMode;
                    if (cheatMode) {
                        applyCheat();
                        notify("作弊模式已开启：无限金钱/威望/虔诚");
                    } else {
                        notify("作弊模式已关闭");
                    }
                }
                case "toggle_infinite_gold" -> {
                    infiniteGoldMode = !infiniteGoldMode;
                    if (infiniteGoldMode) {
                        applyInfiniteGold();
                        notify("无限金钱模式已开启");
                    } else {
                        notify("无限金钱模式已关闭");
                    }
                }
                case "cheat_add_gold" -> {
                    double amount = doubleValOrDefault(payload, "amount", 1000);
                    Character p = sim.world.character(playerId);
                    if (p != null) {
                        p.addGold(amount);
                        notify("作弊：+" + (int) amount + " 金");
                    }
                }
                default -> notify("未知操作: " + kind);
            }
        } catch (Exception e) {
            notify("操作失败: " + e.getMessage());
        }
        applyCheat();
        applyInfiniteGold();
        return snapshot();
    }

    // ─────────────────── 行动实现 ───────────────────

    private void doRaiseArmy(int countyId) {
        World w = sim.world;
        County county = w.map.get(countyId);
        if (county == null) throw new RuntimeException("省份不存在");
        if (county.holder != playerId) {
            List<Integer> owned = new ArrayList<>();
            for (County c : w.map.list()) {
                if (c.holder == playerId) owned.add(c.id);
            }
            if (owned.isEmpty()) throw new RuntimeException("没有可征召的领地");
            if (!owned.contains(countyId)) {
                countyId = owned.get(0);
                county = w.map.get(countyId);
            }
        }
        if (!sim.wars.armiesOf(playerId).isEmpty()) {
            throw new RuntimeException("已有野战军，请先解散或用现有军团");
        }
        int total = 0;
        for (County c : w.map.list()) {
            if (c.holder == playerId) total += c.monthlyLevies();
        }
        int levies = Math.max(200, total);
        int aid = sim.wars.raiseArmy(playerId, countyId, levies, name(playerId) + "的军团");
        Army army = sim.wars.army(aid);
        if (army != null) {
            army.addMen(UnitType.HEAVY_INFANTRY, levies / 10);
            army.addMen(UnitType.ARCHERS, levies / 12);
            army.addMen(UnitType.LIGHT_CAVALRY, levies / 20);
        }
        selectedArmy = aid;
        w.pushLog(name(playerId) + " 在 " + county.name + " 征召 " + levies + " 人");
        notify("征召成功：" + levies + " 人 @ " + county.name);
    }

    private void doMoveArmy(int armyId, int countyId) {
        Army army = sim.wars.army(armyId);
        if (army == null || !army.isActive()) throw new RuntimeException("军团不存在");
        if (army.owner != playerId) throw new RuntimeException("只能调动自己的军团");
        List<Integer> path = sim.world.map.path(army.location, countyId);
        if (path == null || path.isEmpty()) throw new RuntimeException("无法到达该省份");
        army.setPath(path);
        County dest = sim.world.map.get(countyId);
        String dname = dest != null ? dest.name : "?";
        sim.world.pushLog(army.name + " 向 " + dname + " 进军");
        notify("下令进军：" + army.name + " -> " + dname + "（" + (path.size() - 1) + " 步）");
        selectedArmy = armyId;
    }

    private void doDisbandArmy(int armyId) {
        Army army = sim.wars.army(armyId);
        if (army == null || army.owner != playerId) throw new RuntimeException("无法解散该军团");
        army.status = ArmyStatus.DISBANDED;
        army.stacks.clear();
        selectedArmy = -1;
        notify("已解散 " + army.name);
    }

    private void doSetCommander(int armyId, int characterId) {
        Army army = sim.wars.army(armyId);
        if (army == null || army.owner != playerId) throw new RuntimeException("无法指挥该军团");
        Character target = sim.world.character(characterId);
        if (target == null || !target.isAlive()) throw new RuntimeException("人选无效");
        if (!target.isAdult(sim.world.date)) throw new RuntimeException("未成年不能指挥");
        army.commander = characterId;
        notify("任命 " + target.name + " 为 " + army.name + " 指挥官");
    }

    private void doRecruitKnights() {
        Character player = sim.world.character(playerId);
        if (player == null || player.gold < 25) throw new RuntimeException("金币不足（需要 25）");
        List<Integer> armyIds = sim.wars.armiesOf(playerId);
        if (armyIds.isEmpty()) throw new RuntimeException("无野战军，请先征召");
        Army army = sim.wars.army(armyIds.get(0));
        player.addGold(-25);
        army.addMen(UnitType.HEAVY_CAVALRY, 40);
        army.addMen(UnitType.HEAVY_INFANTRY, 80);
        notify("招募精锐：重骑兵+40 重步兵+80（" + army.name + "）");
    }

    private void doDeclareWar(int targetId) {
        if (targetId == playerId) throw new RuntimeException("不能对自己宣战");
        World w = sim.world;
        if (sim.diplomacy.areAllied(playerId, targetId)) throw new RuntimeException("同盟无法宣战");
        if (!sim.diplomacy.canDeclareWar(playerId, targetId, w.date.year()))
            throw new RuntimeException("外交上无法宣战（同盟/停战/已交战）");
        for (War wa : sim.wars.activeWars()) {
            if (wa.involves(playerId) || wa.involves(targetId))
                throw new RuntimeException("一方已在战争中");
        }
        Character player = w.character(playerId);
        Character target = w.character(targetId);
        if (player == null || target == null) throw new RuntimeException("目标无效");
        CasusBelli cb = sim.diplomacy.flags(playerId, targetId).rival
                ? CasusBelli.RIVALRY : CasusBelli.CONQUEST;
        double cost = cb.prestigeCost();
        if (player.prestige < cost) throw new RuntimeException("威望不足（需要 " + (int) cost + "）");
        player.addPrestige(-cost);
        String warName = player.name + " 对 " + target.name + " 的" + cb.nameZh();
        int wid = sim.wars.declareWar(cb, playerId, targetId, w.date, warName);
        sim.diplomacy.setAtWar(playerId, targetId, true);
        War war = sim.wars.war(wid);
        if (war != null) {
            for (int ally : sim.diplomacy.alliesOf(playerId)) {
                if (ally != targetId && !war.involves(ally)) {
                    war.participants.add(new WarParticipant(ally, true, w.date));
                    sim.diplomacy.setAtWar(ally, targetId, true);
                }
            }
        }
        w.pushLog("宣战！" + warName + " (#" + wid + ")");
        sim.diplomacy.addWarExhaustion(playerId);
        sim.diplomacy.addWarExhaustion(targetId);
        notify("已对 " + target.name + " 宣战");
        grantXp(50);
    }

    private void doWhitePeace(int warId) {
        War w = sim.wars.war(warId);
        if (w == null || !w.active) throw new RuntimeException("战争不存在或已结束");
        if (!w.involves(playerId)) throw new RuntimeException("只能提议自己参与的战争白和");
        double atkExh = sim.diplomacy.warExhaustion().getOrDefault(w.attackerPrimary, 0.0);
        double defExh = sim.diplomacy.warExhaustion().getOrDefault(w.defenderPrimary, 0.0);
        int months = w.monthsElapsed(sim.world.date);
        if (months < 6 && Math.abs(w.warscore) > 40) throw new RuntimeException("战况未僵持，无法白和");
        if (!w.canWhitePeace(sim.world.date, atkExh, defExh) && months < 12)
            throw new RuntimeException("战争时间太短或条件不足");
        sim.wars.endWar(warId, WarResult.WHITE_PEACE);
        sim.diplomacy.setAtWar(w.attackerPrimary, w.defenderPrimary, false);
        sim.diplomacy.setTruce(w.attackerPrimary, w.defenderPrimary, sim.world.date.year() + 3);
        Character an = sim.world.character(w.attackerPrimary);
        Character dn = sim.world.character(w.defenderPrimary);
        sim.world.pushLog("白和：" + (an != null ? an.name : "?") + " 与 " + (dn != null ? dn.name : "?") + " 停战");
        notify("已达成白和");
    }

    private void doImproveRelations(int targetId) {
        Character player = sim.world.character(playerId);
        if (player == null || player.gold < 10) throw new RuntimeException("金币不足（需要 10）");
        player.addGold(-10);
        sim.world.modifyOpinion(targetId, playerId, 15);
        sim.world.modifyOpinion(playerId, targetId, 5);
        notify("改善与 " + name(targetId) + " 的关系");
        grantXp(20);
    }

    private void doFormAlliance(int targetId) {
        if (targetId == playerId) throw new RuntimeException("不能与自己结盟");
        if (sim.diplomacy.areAllied(playerId, targetId)) throw new RuntimeException("已是同盟");
        if (sim.diplomacy.flags(playerId, targetId).atWar) throw new RuntimeException("交战中无法结盟");
        Character target = sim.world.character(targetId);
        if (target == null) throw new RuntimeException("目标无效");
        int op = sim.world.opinion(targetId, playerId);
        boolean hasMarriage = sim.diplomacy.flags(playerId, targetId).marriagePact;
        if (op < 30 && !hasMarriage) throw new RuntimeException("好感不足（需 30，当前 " + op + "）或需先联姻");
        sim.diplomacy.formAlliance(playerId, targetId, sim.world.date);
        sim.world.pushLog(name(playerId) + " 与 " + target.name + " 缔结同盟");
        notify("已与 " + target.name + " 缔结同盟");
    }

    private void doFormNonAggression(int targetId) {
        if (targetId == playerId) throw new RuntimeException("无效目标");
        if (sim.diplomacy.flags(playerId, targetId).nonAggression) throw new RuntimeException("已有互不侵犯条约");
        if (sim.diplomacy.flags(playerId, targetId).atWar) throw new RuntimeException("交战中无法签订");
        Character target = sim.world.character(targetId);
        if (target == null) throw new RuntimeException("目标无效");
        sim.diplomacy.flagsMut(playerId, targetId).nonAggression = true;
        addTreaty(targetId, TreatyKind.NON_AGGRESSION, 20);
        sim.world.pushLog(name(playerId) + " 与 " + target.name + " 签订互不侵犯条约");
        notify("已与 " + target.name + " 签订互不侵犯条约");
    }

    private void doFormVassalage(int targetId) {
        if (targetId == playerId) throw new RuntimeException("无效目标");
        if (sim.diplomacy.flags(playerId, targetId).vassalage) throw new RuntimeException("已是附庸关系");
        Character target = sim.world.character(targetId);
        if (target == null) throw new RuntimeException("目标无效");
        sim.diplomacy.flagsMut(playerId, targetId).vassalage = true;
        addTreaty(targetId, TreatyKind.VASSALAGE, 50);
        sim.world.pushLog(name(playerId) + " 成为 " + target.name + " 的附庸");
        notify("已成为 " + target.name + " 的附庸");
    }

    private void doFormTradeAgreement(int targetId) {
        if (targetId == playerId) throw new RuntimeException("无效目标");
        if (sim.diplomacy.flags(playerId, targetId).tradeAgreement) throw new RuntimeException("已有贸易协定");
        Character target = sim.world.character(targetId);
        if (target == null) throw new RuntimeException("目标无效");
        sim.diplomacy.flagsMut(playerId, targetId).tradeAgreement = true;
        addTreaty(targetId, TreatyKind.TRADE_AGREEMENT, 20);
        sim.world.pushLog(name(playerId) + " 与 " + target.name + " 签订贸易协定");
        notify("已与 " + target.name + " 签订贸易协定");
    }

    private void doFormIntelSharing(int targetId) {
        if (targetId == playerId) throw new RuntimeException("无效目标");
        if (sim.diplomacy.flags(playerId, targetId).intelligenceSharing) throw new RuntimeException("已有情报共享");
        Character target = sim.world.character(targetId);
        if (target == null) throw new RuntimeException("目标无效");
        sim.diplomacy.flagsMut(playerId, targetId).intelligenceSharing = true;
        addTreaty(targetId, TreatyKind.INTELLIGENCE_SHARING, 20);
        sim.world.pushLog(name(playerId) + " 与 " + target.name + " 建立情报共享");
        notify("已与 " + target.name + " 建立情报共享");
    }

    private void doArrangeMarriage(int targetId) {
        World w = sim.world;
        Character player = w.character(playerId);
        Character target = w.character(targetId);
        if (player == null || target == null) throw new RuntimeException("目标无效");
        if (!target.isAlive()) throw new RuntimeException("目标已故");
        if (player.gender == target.gender) throw new RuntimeException("同性无法成婚");
        if (player.isMarried()) throw new RuntimeException("玩家已有配偶");
        if (target.isMarried()) throw new RuntimeException("目标已有配偶");
        if (!target.isAdult(w.date)) throw new RuntimeException("目标未成年");
        int op = w.opinion(targetId, playerId);
        if (op < 0) throw new RuntimeException("对方好感不足（当前 " + op + "）");
        boolean ok = w.marry(playerId, targetId);
        if (!ok) throw new RuntimeException("婚姻失败");
        sim.diplomacy.flagsMut(playerId, targetId).marriagePact = true;
        addTreaty(targetId, TreatyKind.MARRIAGE_PACT, 50);
        notify("已与 " + target.name + " 成婚（联姻协定生效）");
    }

    private void doSendGift(int targetId, double amount) {
        if (targetId == playerId) throw new RuntimeException("不能给自己送礼");
        amount = Math.max(1.0, Math.min(500.0, amount));
        Character player = sim.world.character(playerId);
        if (player == null || player.gold < amount) throw new RuntimeException("金币不足（需要 " + (int) amount + "）");
        Character target = sim.world.character(targetId);
        if (target == null) throw new RuntimeException("目标无效");
        player.addGold(-amount);
        target.addGold(amount);
        int gain = com.ckgame.politics.Diplomacy.giftOpinionGain(amount);
        sim.world.modifyOpinion(targetId, playerId, gain);
        sim.world.modifyOpinion(playerId, targetId, gain / 3);
        notify("向 " + target.name + " 赠送 " + (int) amount + " 金（好感 +" + gain + "）");
    }

    private void doSetRival(int targetId) {
        if (targetId == playerId) throw new RuntimeException("无效目标");
        if (sim.diplomacy.flags(playerId, targetId).rival) throw new RuntimeException("已是宿敌");
        Character target = sim.world.character(targetId);
        if (target == null) throw new RuntimeException("目标无效");
        sim.diplomacy.setRival(playerId, targetId);
        sim.diplomacy.setRival(targetId, playerId);
        sim.world.modifyOpinion(playerId, targetId, -30);
        sim.world.modifyOpinion(targetId, playerId, -30);
        sim.world.pushLog(name(playerId) + " 视 " + target.name + " 为宿敌");
        notify("已将 " + target.name + " 设为宿敌");
    }

    private void doInviteToCourt(int targetId) {
        if (targetId == playerId) throw new RuntimeException("不能邀请自己");
        Character target = sim.world.character(targetId);
        if (target == null || !target.isAlive()) throw new RuntimeException("目标无效");
        if (!target.isAdult(sim.world.date)) throw new RuntimeException("目标未成年");
        Character player = sim.world.character(playerId);
        if (player == null) throw new RuntimeException("玩家无效");
        if (player.gold < 30) throw new RuntimeException("金币不足（需要 30）");
        player.addGold(-30);
        sim.world.modifyOpinion(targetId, playerId, 15);
        sim.world.pushLog(player.name + " 邀请 " + target.name + " 访问宫廷");
        notify("已邀请 " + target.name + " 访问宫廷（花费 30 金）");
    }

    private void doHostFeastFor(int targetId) {
        if (targetId == playerId) throw new RuntimeException("不能宴请自己");
        Character target = sim.world.character(targetId);
        if (target == null || !target.isAlive()) throw new RuntimeException("目标无效");
        Character player = sim.world.character(playerId);
        if (player == null) throw new RuntimeException("玩家无效");
        if (player.gold < 40) throw new RuntimeException("金币不足（需要 40）");
        player.addGold(-40);
        sim.world.modifyOpinion(targetId, playerId, 20);
        player.addPrestige(10);
        player.addStress(-8);
        sim.world.pushLog(player.name + " 为 " + target.name + " 举办宴会");
        notify("已为 " + target.name + " 举办宴会（好感 +20，威望 +10）");
    }

    private void doDuel(int targetId) {
        if (targetId == playerId) throw new RuntimeException("不能与自己决斗");
        Character target = sim.world.character(targetId);
        if (target == null || !target.isAlive()) throw new RuntimeException("目标无效");
        if (!target.isAdult(sim.world.date)) throw new RuntimeException("目标未成年");
        Character player = sim.world.character(playerId);
        if (player == null) throw new RuntimeException("玩家无效");
        AttributeSet pAttrs = sim.world.effectiveAttrs(playerId);
        AttributeSet tAttrs = sim.world.effectiveAttrs(targetId);
        Random rng = sim.world.rng();
        double playerScore = (pAttrs != null ? pAttrs.prowess() : 0) + rng.nextDouble() * 20;
        double targetScore = (tAttrs != null ? tAttrs.prowess() : 0) + rng.nextDouble() * 20;
        if (playerScore > targetScore) {
            sim.world.modifyOpinion(targetId, playerId, -10);
            sim.world.modifyOpinion(playerId, targetId, -15);
            player.addPrestige(15);
            target.addStress(10);
            sim.world.pushLog(player.name + " 在决斗中击败了 " + target.name);
            notify("决斗胜利！威望 +15");
        } else if (playerScore < targetScore) {
            sim.world.modifyOpinion(targetId, playerId, 5);
            sim.world.modifyOpinion(playerId, targetId, -20);
            player.addStress(15);
            player.health -= 0.1;
            sim.world.pushLog(player.name + " 在决斗中被 " + target.name + " 击败");
            notify("决斗失败，受伤了");
        } else {
            sim.world.pushLog(player.name + " 与 " + target.name + " 的决斗不分胜负");
            notify("决斗平局");
        }
    }

    private void doStartScheme(String schemeKindName, int targetId) {
        if (schemeKindName == null) throw new RuntimeException("缺少 scheme_kind");
        SchemeKind kind;
        try {
            kind = SchemeKind.valueOf(schemeKindName);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("未知阴谋类型: " + schemeKindName);
        }
        if (targetId == playerId) throw new RuntimeException("不能对自己发起阴谋");
        Character target = sim.world.character(targetId);
        if (target == null || !target.isAlive()) throw new RuntimeException("目标无效");
        for (Scheme s : sim.schemes.schemes()) {
            if (s.owner == playerId && s.target == targetId && !s.exposed && !s.isComplete())
                throw new RuntimeException("已有针对该目标的进行中阴谋");
        }
        int sid = sim.schemes.start(kind, playerId, targetId, sim.world.date);
        sim.world.pushLog(name(playerId) + " 开始策划" + kind.nameZh() + "（目标：" + target.name + "）");
        notify("已发起" + kind.nameZh() + " -> " + target.name + "（#" + sid + "）");
    }

    private void doAppointCouncil(String positionName, int characterId) {
        if (positionName == null) throw new RuntimeException("缺少 position");
        CouncilPosition pos;
        try {
            pos = CouncilPosition.valueOf(positionName);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("未知职位: " + positionName);
        }
        Character target = sim.world.character(characterId);
        if (target == null || !target.isAlive()) throw new RuntimeException("人选无效");
        if (!target.isAdult(sim.world.date)) throw new RuntimeException("未成年不能入阁");
        Council council = sim.councils.councilFor(playerId);
        for (CouncilPosition p : CouncilPosition.all()) {
            if (council.get(p) == characterId) council.set(p, Constants.NONE_ID);
        }
        council.set(pos, characterId);
        notify("任命 " + target.name + " 为" + pos.nameZh());
    }

    private void doAssignCouncilTask(String positionName, String taskName) {
        if (positionName == null || taskName == null) throw new RuntimeException("缺少 position 或 task");
        CouncilPosition pos;
        CouncilTask task;
        try {
            pos = CouncilPosition.valueOf(positionName);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("未知职位: " + positionName);
        }
        try {
            task = CouncilTask.valueOf(taskName);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("未知任务: " + taskName);
        }
        Council council = sim.councils.councilFor(playerId);
        council.tasks.put(pos, task);
        notify(pos.nameZh() + " 任务改为：" + task.nameZh());
    }

    private void doGrantTitle(int titleId, int targetId) {
        World w = sim.world;
        Character player = w.character(playerId);
        Character target = w.character(targetId);
        if (player == null || target == null) throw new RuntimeException("目标无效");
        Title title = w.title(titleId);
        if (title == null) throw new RuntimeException("头衔不存在");
        if (title.holder != playerId) throw new RuntimeException("该头衔不属于你");
        if (titleId == player.primaryTitle) throw new RuntimeException("不能授予主头衔");
        boolean ok = w.grantTitle(titleId, targetId);
        if (!ok) throw new RuntimeException("授予失败");
        if (player.primaryTitle != Constants.NONE_ID) {
            w.setVassal(titleId, player.primaryTitle);
        }
        notify("将「" + title.name + "」授予 " + target.name);
    }

    private void doHoldFeast() {
        Character player = sim.world.character(playerId);
        if (player == null || player.gold < 20) throw new RuntimeException("金币不足（需要 20）");
        player.addGold(-20);
        player.addPrestige(15);
        player.addStress(-10);
        for (Faction f : new ArrayList<>(sim.factions.factions().values())) {
            if (f.targetLiege == playerId) {
                for (int mid : new ArrayList<>(f.members)) {
                    sim.world.modifyOpinion(mid, playerId, 8);
                }
                sim.factions.appease(f.id, 10.0);
            }
        }
        sim.world.pushLog(player.name + " 举办了宴会");
        notify("举办宴会：威望+15，压力-10，派系不满下降");
    }

    private void doAppeaseFaction(int factionId) {
        Faction f = sim.factions.factions().get(factionId);
        if (f == null) throw new RuntimeException("派系不存在");
        if (f.targetLiege != playerId) throw new RuntimeException("只能安抚针对自己的派系");
        Character player = sim.world.character(playerId);
        int cost = 25;
        if (player == null || player.gold < cost) throw new RuntimeException("金币不足（需要 " + cost + "）");
        player.addGold(-cost);
        for (int mid : f.members) {
            sim.world.modifyOpinion(mid, playerId, 12);
        }
        boolean ok = sim.factions.appease(factionId, 30.0);
        if (!ok || !sim.factions.factions().containsKey(factionId)) {
            notify("派系已解散");
            sim.world.pushLog(player.name + " 成功安抚并解散了一个派系");
        } else {
            Faction left = sim.factions.factions().get(factionId);
            notify("派系不满降至 " + (int) left.discontent);
            sim.world.pushLog(player.name + " 安抚了派系，不满下降");
        }
    }

    private void doDevelopCounty(int countyId) {
        County county = sim.world.map.get(countyId);
        if (county == null) throw new RuntimeException("省份不存在");
        if (county.holder != playerId) throw new RuntimeException("不是己方领地");
        Character player = sim.world.character(playerId);
        if (player == null || player.gold < 10) throw new RuntimeException("金币不足（需要 10）");
        int cap = county.terrain.developmentCap();
        if (county.development >= cap) throw new RuntimeException("已达发展上限（" + cap + "）");
        player.addGold(-10);
        county.development = Math.min(cap, county.development + 1);
        notify(county.name + " 发展度 +1（-> " + county.development + "）");
    }

    private void doUpgradeBuilding(int countyId, String buildingKindName) {
        County county = sim.world.map.get(countyId);
        if (county == null) throw new RuntimeException("省份不存在");
        if (county.holder != playerId) throw new RuntimeException("不是己方领地");
        if (buildingKindName == null) throw new RuntimeException("缺少建筑类型");
        BuildingKind kind;
        try {
            kind = BuildingKind.valueOf(buildingKindName);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("未知建筑类型: " + buildingKindName);
        }
        Character player = sim.world.character(playerId);
        if (player == null) throw new RuntimeException("玩家无效");
        CountyBuilding b = sim.buildings.getBuilding(countyId, kind);
        if (b == null) {
            int cost = kind.upgradeCost(0);
            if (player.gold < cost) throw new RuntimeException("金币不足（需要 " + cost + "）");
            player.addGold(-cost);
            sim.buildings.addBuilding(countyId, kind);
            sim.world.pushLog(player.name + " 在 " + county.name + " 建造了 " + kind.nameZh());
            notify("建造 " + kind.nameZh() + "（花费 " + cost + " 金）");
            grantXp(20);
        } else {
            if (!b.canUpgrade()) throw new RuntimeException(kind.nameZh() + " 已达最高级");
            int cost = b.upgradeCost();
            if (player.gold < cost) throw new RuntimeException("金币不足（需要 " + cost + "）");
            player.addGold(-cost);
            b.level += 1;
            sim.world.pushLog(player.name + " 将 " + county.name + " 的 " + kind.nameZh() + " 升级到 " + b.level + " 级");
            notify(kind.nameZh() + " 升级到 " + b.level + " 级（花费 " + cost + " 金）");
            grantXp(20);
        }
    }

    private void doUpgradePort(int countyId) {
        County county = sim.world.map.get(countyId);
        if (county == null) throw new RuntimeException("省份不存在");
        if (county.holder != playerId) throw new RuntimeException("不是己方领地");
        if (!county.hasPort) throw new RuntimeException("该省份没有港口");
        Character player = sim.world.character(playerId);
        double upgradeCost = county.upgradePort();
        if (upgradeCost <= 0) throw new RuntimeException("港口已达最高级");
        if (player.gold < upgradeCost) throw new RuntimeException("金币不足（需要 " + (int) upgradeCost + "）");
        player.addGold(-upgradeCost);
        notify(county.name + " 港口升级到 " + county.portLevel + " 级（花费 " + (int) upgradeCost + " 金）");
    }

    private void doUpgradeTradeRouteMaintenance(int countyId) {
        County county = sim.world.map.get(countyId);
        if (county == null) throw new RuntimeException("省份不存在");
        if (county.holder != playerId) throw new RuntimeException("不是己方领地");
        Character player = sim.world.character(playerId);
        double upgradeCost = county.upgradeTradeRouteMaintenance();
        if (upgradeCost <= 0) throw new RuntimeException("贸易路线维护已达最高级");
        if (player.gold < upgradeCost) throw new RuntimeException("金币不足（需要 " + (int) upgradeCost + "）");
        player.addGold(-upgradeCost);
        notify(county.name + " 贸易路线维护升级到 " + county.tradeRouteMaintenanceLevel + " 级（花费 " + (int) upgradeCost + " 金）");
    }

    private void doFabricateClaim(int countyId) {
        County county = sim.world.map.get(countyId);
        if (county == null) throw new RuntimeException("省份不存在");
        if (county.holder == playerId) throw new RuntimeException("已是己方领地");
        Character player = sim.world.character(playerId);
        if (player == null || player.gold < 50) throw new RuntimeException("金币不足（需要 50）");
        player.addGold(-50);
        int titleId = county.ownerTitle;
        if (titleId == Constants.NONE_ID) {
            sim.diplomacy.addClaim(playerId, Constants.NONE_ID, countyId, 60);
        } else {
            sim.diplomacy.addClaim(playerId, titleId, countyId, 60);
        }
        sim.world.pushLog(player.name + " 伪造了对 " + county.name + " 的宣称");
        notify("已伪造对 " + county.name + " 的宣称（花费 50 金）");
        grantXp(40);
    }

    private void doSetSuccessionLaw(String lawName) {
        if (lawName == null) throw new RuntimeException("缺少 law");
        SuccessionLaw newLaw;
        try {
            newLaw = SuccessionLaw.valueOf(lawName);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("未知继承法: " + lawName);
        }
        RealmLaw old = getEffectiveLaw();
        RealmLaw updated = new RealmLaw(newLaw, old.crownAuthority(), old.genderLaw(), old.partitionEnabled());
        sim.realmLaws.put(playerId, updated);
        Title title = playerTitle();
        if (title != null) title.realmLaw = updated;
        notify("继承法已改为：" + newLaw.nameZh());
    }

    private void doSetCrownAuthority(int level) {
        CrownAuthority ca;
        try {
            ca = CrownAuthority.values()[level];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new RuntimeException("未知王权等级: " + level);
        }
        RealmLaw old = getEffectiveLaw();
        RealmLaw updated = new RealmLaw(old.succession(), ca, old.genderLaw(), old.partitionEnabled());
        sim.realmLaws.put(playerId, updated);
        Title title = playerTitle();
        if (title != null) title.realmLaw = updated;
        notify("王权已改为：" + ca.nameZh());
    }

    private void doSetGenderLaw(String lawName) {
        if (lawName == null) throw new RuntimeException("缺少 law");
        GenderLaw newLaw;
        try {
            newLaw = GenderLaw.valueOf(lawName);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("未知性别法: " + lawName);
        }
        RealmLaw old = getEffectiveLaw();
        RealmLaw updated = new RealmLaw(old.succession(), old.crownAuthority(), newLaw, old.partitionEnabled());
        sim.realmLaws.put(playerId, updated);
        Title title = playerTitle();
        if (title != null) title.realmLaw = updated;
        notify("性别法已改为：" + newLaw.nameZh());
    }

    private void doResolveEvent(int eventId, int choiceId) {
        EventInstance inst = null;
        for (EventInstance e : sim.events.pending) {
            if (e.eventId == eventId && e.character == playerId) {
                inst = e;
                break;
            }
        }
        if (inst == null) throw new RuntimeException("事件不存在");
        sim.events.resolveChoice(sim.world, inst, choiceId);
        sim.events.pending.remove(inst);
        notify("已选择事件选项：" + inst.title);
        grantXp(30);
    }

    private void doSave(String slotName) {
        try {
            SaveManager.SaveInfo info = new SaveManager.SaveInfo();
            java.nio.file.Path path = SaveManager.save(sim, playerId, slotName);
            notify("已存档 -> " + path.getFileName());
        } catch (Exception e) {
            throw new RuntimeException("存档失败: " + e.getMessage());
        }
    }

    private void doLoad(String slotName) {
        try {
            SaveManager.LoadedGame lg = SaveManager.load(slotName);
            if (lg == null) throw new RuntimeException("没有存档");
            this.sim = lg.sim;
            this.playerId = lg.playerId;
            syncPlayer();
            selectedCounty = -1;
            selectedArmy = -1;
            notify("已读档");
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("读档失败: " + e.getMessage());
        }
    }

    private void doDeleteSave(String slotName) {
        if (slotName == null) throw new RuntimeException("缺少存档名");
        boolean ok = SaveManager.deleteSave(slotName);
        if (!ok) throw new RuntimeException("无法删除存档（可能为自动存档或不存在）");
        notify("已删除存档 " + slotName);
    }

    private void doNewGame(Object scenario) {
        String sc = scenario != null ? scenario.toString() : this.sim.scenarioId;
        this.sim = new GameSimulation(sc);
        this.playerId = defaultPlayer();
        syncPlayer();
        selectedCounty = -1;
        selectedArmy = -1;
        messages.clear();
        messages.add("新局开始（场景：" + this.sim.scenarioId + "）。");
    }

    // ─────────────────── snapshot 辅助 ───────────────────

    private int defaultPlayer() {
        for (Character c : sim.world.aliveCharacters()) {
            if (c.name.contains("哈罗德")) return c.id;
        }
        List<Character> rulers = sim.world.rulers();
        return rulers.isEmpty() ? 1 : rulers.get(0).id;
    }

    private void syncPlayer() {
        sim.playerIds.clear();
        sim.playerIds.add(playerId);
    }

    private void notify(String msg) {
        messages.add(msg);
        if (messages.size() > 80) {
            messages.subList(0, messages.size() - 60).clear();
        }
    }

    private String name(int charId) {
        Character c = sim.world.character(charId);
        return c != null ? c.name : "?";
    }

    private String holderColor(int holderId) {
        if (holderId == Constants.NONE_ID) return "#4a5568";
        Character c = sim.world.character(holderId);
        if (c == null) return "#4a5568";
        Dynasty d = sim.world.dynasties.get(c.dynasty);
        if (d != null && (d.colorR != 128 || d.colorG != 128 || d.colorB != 128)) {
            return "rgb(" + d.colorR + "," + d.colorG + "," + d.colorB + ")";
        }
        int hue = (holderId * 47) % 360;
        return "hsl(" + hue + " 55% 42%)";
    }

    private Title playerTitle() {
        Character player = sim.world.character(playerId);
        if (player == null || player.primaryTitle == Constants.NONE_ID) return null;
        return sim.world.title(player.primaryTitle);
    }

    private RealmLaw getEffectiveLaw() {
        RealmLaw law = sim.realmLaws.get(playerId);
        if (law != null) return law;
        Title title = playerTitle();
        if (title != null && title.realmLaw != null) return title.realmLaw;
        return RealmLaw.feudalDefault();
    }

    private Map<String, Object> playerLaws() {
        RealmLaw law = getEffectiveLaw();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("succession", law.succession().name());
        m.put("crown_authority", law.crownAuthority().value());
        m.put("gender_law", law.genderLaw().name());
        return m;
    }

    private void applyCheat() {
        if (!cheatMode) return;
        Character player = sim.world.character(playerId);
        if (player == null) return;
        player.gold = CHEAT_GOLD;
        player.prestige = CHEAT_PRESTIGE;
        player.piety = CHEAT_PIETY;
        player.stress = 0;
    }

    private void applyInfiniteGold() {
        if (!infiniteGoldMode) return;
        Character player = sim.world.character(playerId);
        if (player == null) return;
        player.gold = CHEAT_GOLD;
    }

    private void grantXp(int amount) {
        Character player = sim.world.character(playerId);
        if (player == null) return;
        boolean leveled = player.gainXp(amount);
        if (leveled) {
            notify("角色升级！当前等级：" + player.level);
        }
    }

    private List<Map<String, Object>> countyBuildings(int countyId) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (CountyBuilding b : sim.buildings.getBuildings(countyId)) {
            Map<String, Object> bm = new LinkedHashMap<>();
            bm.put("kind", b.kind.name());
            bm.put("name", b.kind.nameZh());
            bm.put("level", b.level);
            bm.put("maxLevel", b.kind.maxLevel());
            bm.put("canUpgrade", b.canUpgrade());
            bm.put("upgradeCost", b.upgradeCost());
            bm.put("description", b.kind.description());
            out.add(bm);
        }
        return out;
    }

    private List<Map<String, Object>> playerSchemes() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Scheme s : sim.schemes.schemes()) {
            if (s.owner != playerId || s.exposed || s.isComplete()) continue;
            Character target = sim.world.character(s.target);
            Map<String, Object> sm = new LinkedHashMap<>();
            sm.put("id", s.id);
            sm.put("kind", s.kind.name());
            sm.put("kindZh", s.kind.nameZh());
            sm.put("targetId", s.target);
            sm.put("targetName", target != null ? target.name : "?");
            sm.put("progress", round1(s.progress));
            sm.put("secrecy", round1(s.secrecy));
            out.add(sm);
        }
        return out;
    }

    private Map<String, Object> playerCouncil() {
        Council council = sim.councils.get(playerId);
        if (council == null) return null;
        World w = sim.world;
        List<Map<String, Object>> members = new ArrayList<>();
        for (CouncilPosition pos : CouncilPosition.all()) {
            int who = council.get(pos);
            CouncilTask task = council.taskOf(pos);
            Character ch = who != Constants.NONE_ID ? w.character(who) : null;
            Map<String, Object> mm = new LinkedHashMap<>();
            mm.put("position", pos.name());
            mm.put("positionZh", pos.nameZh());
            mm.put("holderId", who != Constants.NONE_ID ? who : null);
            mm.put("holderName", ch != null ? ch.name : "（空缺）");
            mm.put("task", task.name());
            mm.put("taskZh", task.nameZh());
            members.add(mm);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("members", members);
        return result;
    }

    private List<Map<String, Object>> playerClaims() {
        List<Map<String, Object>> out = new ArrayList<>();
        World w = sim.world;
        for (Claim claim : sim.diplomacy.claimsOf(playerId)) {
            Title title = claim.title != Constants.NONE_ID ? w.title(claim.title) : null;
            County county = claim.county != Constants.NONE_ID ? w.map.get(claim.county) : null;
            Map<String, Object> cm = new LinkedHashMap<>();
            cm.put("titleId", claim.title != Constants.NONE_ID ? claim.title : null);
            cm.put("titleName", title != null ? title.name : null);
            cm.put("countyId", claim.county);
            cm.put("countyName", county != null ? county.name : null);
            cm.put("strength", claim.strength);
            cm.put("pressed", claim.pressed);
            out.add(cm);
        }
        return out;
    }

    private List<Map<String, Object>> playerTreaties() {
        List<Map<String, Object>> out = new ArrayList<>();
        World w = sim.world;
        for (Treaty t : sim.diplomacy.treaties()) {
            if (t.a != playerId && t.b != playerId) continue;
            int otherId = t.a == playerId ? t.b : t.a;
            Character other = w.character(otherId);
            Map<String, Object> tm = new LinkedHashMap<>();
            tm.put("kind", t.kind.name());
            tm.put("kindZh", t.kind.nameZh());
            tm.put("otherId", otherId);
            tm.put("otherName", other != null ? other.name : "?");
            tm.put("expiresYear", t.expiresYear);
            out.add(tm);
        }
        return out;
    }

    private List<Map<String, Object>> allCharacters() {
        World w = sim.world;
        List<Map<String, Object>> out = new ArrayList<>();
        for (Character c : w.aliveCharacters()) {
            AttributeSet attrs = w.effectiveAttrs(c.id);
            Dynasty dynasty = w.dynasties.get(c.dynasty);
            Title title = c.primaryTitle != Constants.NONE_ID ? w.title(c.primaryTitle) : null;
            Map<String, Object> cm = new LinkedHashMap<>();
            cm.put("id", c.id);
            cm.put("name", c.name);
            cm.put("dynastyName", dynasty != null ? dynasty.name : "");
            cm.put("gender", c.gender.name());
            cm.put("age", c.ageAt(w.date));
            cm.put("isRuler", c.isRuler);
            cm.put("title", title != null ? title.name : "");
            cm.put("gold", (int) c.gold);
            cm.put("prestige", (int) c.prestige);
            cm.put("isMarried", c.isMarried());
            cm.put("spouseIds", new ArrayList<>(c.spouses));
            if (attrs != null) {
                Map<String, Object> am = new LinkedHashMap<>();
                am.put("diplomacy", attrs.diplomacy());
                am.put("martial", attrs.martial());
                am.put("stewardship", attrs.stewardship());
                am.put("intrigue", attrs.intrigue());
                am.put("learning", attrs.learning());
                am.put("prowess", attrs.prowess());
                cm.put("attrs", am);
            }
            cm.put("opinionOfPlayer", w.opinion(c.id, playerId));
            cm.put("playerOpinion", w.opinion(playerId, c.id));
            DiplomacyFlags flags = sim.diplomacy.flags(playerId, c.id);
            cm.put("relationAllied", flags.allied);
            cm.put("relationRival", flags.rival);
            cm.put("relationAtWar", flags.atWar);
            cm.put("relationMarriage", flags.marriagePact);
            cm.put("relationVassalage", flags.vassalage);
            cm.put("relationTradeAgreement", flags.tradeAgreement);
            cm.put("relationIntelSharing", flags.intelligenceSharing);
            cm.put("heldTitleIds", new ArrayList<>(c.heldTitles));
            cm.put("level", c.level);
            cm.put("xp", c.xp);
            cm.put("xpToNext", c.xpToNextLevel());
            out.add(cm);
        }
        return out;
    }

    private List<Map<String, Object>> playerStorylines() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Storyline s : sim.storylines.storylines) {
            if (s.characterId != 0 && s.characterId != playerId) continue;
            Map<String, Object> sm = new LinkedHashMap<>();
            sm.put("id", s.id);
            sm.put("title", s.title);
            sm.put("description", s.description);
            sm.put("status", s.status.name());
            sm.put("currentStage", s.currentStage);
            String stageTitle = "";
            String stageDesc = "";
            for (StorylineStage st : s.stages) {
                if (st.stageId == s.currentStage) {
                    stageTitle = st.title;
                    stageDesc = st.description;
                    break;
                }
            }
            sm.put("stageTitle", stageTitle);
            sm.put("stageDescription", stageDesc);
            sm.put("tags", new ArrayList<>(s.tags));
            out.add(sm);
        }
        return out;
    }

    private List<Map<String, Object>> schemeTypes() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (SchemeKind sk : SchemeKind.values()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("value", sk.name());
            m.put("name", sk.nameZh());
            out.add(m);
        }
        return out;
    }

    private List<Map<String, Object>> councilPositions() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (CouncilPosition cp : CouncilPosition.values()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("value", cp.name());
            m.put("name", cp.nameZh());
            out.add(m);
        }
        return out;
    }

    private List<Map<String, Object>> councilTasks() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (CouncilTask ct : CouncilTask.values()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("value", ct.name());
            m.put("name", ct.nameZh());
            out.add(m);
        }
        return out;
    }

    private List<Map<String, Object>> listSaves() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (SaveManager.SaveInfo si : SaveManager.listSaves()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", si.name);
            m.put("file", si.file);
            m.put("date", si.date);
            m.put("playerId", si.playerId);
            m.put("mtime", si.mtime);
            m.put("size", si.size);
            out.add(m);
        }
        return out;
    }

    private List<Map<String, Object>> serializeTradeRoutes() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (var route : sim.world.tradeRoutes) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("from", route.from());
            m.put("to", route.to());
            m.put("exchangeRate", route.exchangeRate());
            m.put("tradeVolume", route.tradeVolume());
            out.add(m);
        }
        return out;
    }

    private void addTreaty(int targetId, TreatyKind kind, int years) {
        Treaty treaty = new Treaty(playerId, targetId, kind, sim.world.date, sim.world.date.year() + years);
        sim.diplomacy.addTreaty(treaty);
    }

    // ─────────────────── 工具方法 ───────────────────

    private static int intVal(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v instanceof Number) return ((Number) v).intValue();
        throw new RuntimeException("缺少参数: " + key);
    }

    private static int intValOrDefault(Map<String, Object> map, String key, int fallback) {
        Object v = map.get(key);
        if (v instanceof Number) return ((Number) v).intValue();
        return fallback;
    }

    private static double doubleValOrDefault(Map<String, Object> map, String key, double fallback) {
        Object v = map.get(key);
        if (v instanceof Number) return ((Number) v).doubleValue();
        return fallback;
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static String format1(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    private static String format0(double v) {
        return String.format(Locale.ROOT, "%.0f", v);
    }
}
