import { GameSimulation } from '../game/GameSimulation.js';
import { SaveManager } from './SaveManager.js';
import { NONE_ID } from '../core/Constants.js';
import { Balance } from '../core/balance/Balance.js';
import { CasusBelli, cbNameZh, cbPrestigeCost } from '../politics/CasusBelli.js';
import { SchemeKind, schemeKindNameZh } from '../politics/SchemeKind.js';
import { ArmyStatus } from '../military/ArmyStatus.js';
import { WarResult } from '../military/WarResult.js';
import { WarParticipant } from '../military/WarParticipant.js';
import { TreatyKind } from '../politics/TreatyKind.js';
import { Treaty } from '../politics/Treaty.js';
import { CouncilPosition } from '../politics/CouncilPosition.js';
import { CouncilTask } from '../politics/CouncilTask.js';
import { RealmLaw, SuccessionLaw, CrownAuthority, GenderLaw } from '../politics/Laws.js';
import { BuildingKind } from '../world/buildings/BuildingKind.js';
const CHEAT_GOLD = 99999;
const CHEAT_PRESTIGE = 99999;
const CHEAT_PIETY = 99999;
export class GameAPI {
    sim;
    playerId;
    selectedCounty = -1;
    selectedArmy = -1;
    messages = [];
    cheatMode = false;
    infiniteGoldMode = false;
    constructor() {
        this.sim = new GameSimulation();
        this.playerId = this.defaultPlayer();
        this.syncPlayer();
        this.messages.push('\u6b22\u8fce\u3002\u70b9\u51fb\u5730\u56fe\u7701\u4efd\u67e5\u770b\u8be6\u60c5\uff0c\u4f7f\u7528\u4fa7\u680f\u4e0b\u8fbe\u6307\u4ee4\u3002');
    }
    constructorFromSave(sim, playerId) {
        this.sim = sim;
        this.playerId = playerId;
        this.syncPlayer();
        this.messages = ['\u8bfb\u6863\u5b8c\u6210\u3002'];
    }
    snapshot() {
        const w = this.sim.world;
        const player = w.character(this.playerId);
        const counties = [];
        const armies = this.sim.wars.armies;
        for (const county of w.map.list()) {
            const holder = w.character(county.holder);
            const armiesHere = [];
            if (armies)
                for (const [, a] of armies) {
                    if (a.isActive() && a.location === county.id)
                        armiesHere.push(a.id);
                }
            const siege = this.sim.sieges.activeAt?.(county.id) ?? null;
            counties.push({ id: county.id, name: county.name, terrain: county.terrain, development: county.development, devCap: county.terrain?.developmentCap?.() ?? 30, control: r1(county.control), levies: county.monthlyLevies?.() ?? county.levies, tax: r2(county.monthlyTax?.() ?? county.tax), fort: county.fortLevel, buildings: this.countyBuildings(county.id), holderId: county.holder === NONE_ID ? null : county.holder, holderName: holder?.name ?? '\u65e0\u4e3b', color: this.holderColor(county.holder), neighbors: [...county.neighbors], armies: armiesHere, siege: siege ? { progress: r1(siege.progress), required: r1(siege.requiredProgress?.() ?? 100), attacker: siege.attacker } : null, isPlayer: holder != null && holder.id === this.playerId, hasPort: county.hasPort ?? false, portLevel: county.portLevel ?? 0, portIncome: r1(county.portIncome ?? 0), tradeRouteProtected: county.tradeRouteProtected ?? false, tradeRouteProtectionLevel: county.tradeRouteProtectionLevel ?? 0, tradeRouteMaintenanceLevel: county.tradeRouteMaintenanceLevel ?? 0, tradeRouteUpgradeCost: r1(county.tradeRouteUpgradeCost ?? 0) });
        }
        const armiesSnap = [];
        if (armies)
            for (const [, a] of armies) {
                if (!a.isActive())
                    continue;
                const loc = w.map.get(a.location);
                const owner = w.character(a.owner);
                armiesSnap.push({ id: a.id, name: a.name, ownerId: a.owner, ownerName: owner?.name ?? '?', location: a.location, locationName: loc?.name ?? '?', men: a.totalMen(), status: a.status, morale: r1(a.morale), supply: r1(a.supply), supplyLow: a.supply < Balance.SUPPLY_LOW_THRESHOLD, inEnemy: false, isPlayer: a.owner === this.playerId, path: [...(a.path ?? [])] });
            }
        const warsSnap = [];
        for (const war of this.sim.wars.activeWars()) {
            const atk = w.character(war.attackerPrimary);
            const def = w.character(war.defenderPrimary);
            warsSnap.push({ id: war.id, name: war.name, active: war.active, warscore: war.warscore, cb: cbNameZh(war.cb), attacker: atk?.name ?? '?', defender: def?.name ?? '?', involvesPlayer: war.involves(this.playerId), months: war.active ? (war.monthsElapsed?.(w.date) ?? 0) : 0, canWhitePeace: false });
        }
        const rulers = [];
        for (const r of w.rulers()) {
            const attrs = w.effectiveAttrs?.(r.id);
            const title = w.title(r.primaryTitle);
            rulers.push({ id: r.id, name: r.name, title: title?.name ?? '\u65e0', gold: r1(r.gold), prestige: r1(r.prestige), martial: attrs?.martial ?? 0, income: r1(w.monthlyIncomeOf?.(r.id) ?? 0), men: this.sim.wars.totalMenOf?.(r.id) ?? 0, isPlayer: r.id === this.playerId });
        }
        const playerInfo = player ? { id: player.id, name: player.name, title: w.title(player.primaryTitle)?.name ?? '\u65e0', gold: r1(player.gold), prestige: r1(player.prestige), piety: r1(player.piety), stress: player.stress, health: r2(player.health), attrs: { diplomacy: player.baseAttrs.diplomacy, martial: player.baseAttrs.martial, stewardship: player.baseAttrs.stewardship, intrigue: player.baseAttrs.intrigue, learning: player.baseAttrs.learning, prowess: player.baseAttrs.prowess }, income: r1(w.monthlyIncomeOf?.(player.id) ?? 0), men: this.sim.wars.totalMenOf?.(player.id) ?? 0, laws: this.playerLaws(), level: player.level, xp: player.xp, xpToNext: player.xpToNextLevel?.() ?? 100 } : null;
        const playable = [];
        for (const r of w.rulers()) {
            const t = w.title(r.primaryTitle);
            playable.push({ id: r.id, name: r.name, title: t?.name ?? '' });
        }
        const pendingEvents = [];
        const pending = this.sim.events.pending;
        if (pending)
            for (const inst of pending) {
                if (inst.character === this.playerId)
                    pendingEvents.push({ eventId: inst.eventId, title: inst.title, description: inst.description, choices: inst.choices.map((ch) => ({ id: ch.id, text: ch.text, aiWeight: ch.aiWeight })) });
            }
        const log = w.log.length > 30 ? w.log.slice(-30) : [...w.log];
        return { date: w.date.toString(), season: w.date.season(), tick: w.tick ?? 0, player: playerInfo, player_war_exhaustion: r1(this.sim.diplomacy.warExhaustion?.get?.(this.playerId) ?? 0), playable, counties, armies: armiesSnap, wars: warsSnap, factions: [], rulers, log, messages: this.getMessages(), selected_county: this.selectedCounty, selected_army: this.selectedArmy, saves: SaveManager.listSaves(), cheat_mode: this.cheatMode, infinite_gold_mode: this.infiniteGoldMode, pending_events: pendingEvents, player_schemes: this.playerSchemes(), player_council: this.playerCouncil(), player_claims: this.playerClaims(), treaties: this.playerTreaties(), characters: this.allCharacters(), scheme_types: this.schemeTypes(), council_positions: this.councilPositions(), council_tasks: this.councilTasks(), storylines: this.playerStorylines(), trade_routes: [], exchange_rates: {} };
    }
    action(payload) {
        const kind = payload.action;
        try {
            switch (kind) {
                case 'select_county':
                    this.selectedCounty = intV(payload, 'county_id');
                    this.selectedArmy = -1;
                    break;
                case 'select_army':
                    this.selectedArmy = intV(payload, 'army_id');
                    this.selectedCounty = -1;
                    break;
                case 'set_player':
                    this.playerId = intV(payload, 'character_id');
                    this.syncPlayer();
                    this.selectedArmy = -1;
                    this.notify('\u5207\u6362\u73a9\u5bb6\u4e3a ' + this.name(this.playerId));
                    break;
                case 'advance': {
                    const days = Math.max(1, Math.min(365, intVD(payload, 'days', 1)));
                    this.syncPlayer();
                    this.sim.runDays(days);
                    this.notify('\u65f6\u95f4\u63a8\u8fdb ' + days + ' \u5929 -> ' + this.sim.world.date);
                    break;
                }
                case 'raise_army':
                    this.doRaiseArmy(intV(payload, 'county_id'));
                    break;
                case 'move_army':
                    this.doMoveArmy(intV(payload, 'army_id'), intV(payload, 'county_id'));
                    break;
                case 'disband_army':
                    this.doDisbandArmy(intV(payload, 'army_id'));
                    break;
                case 'set_commander':
                    this.doSetCommander(intV(payload, 'army_id'), intV(payload, 'character_id'));
                    break;
                case 'recruit_knights':
                    this.doRecruitKnights();
                    break;
                case 'declare_war':
                    this.doDeclareWar(intV(payload, 'target_id'));
                    break;
                case 'white_peace':
                    this.doWhitePeace(intV(payload, 'war_id'));
                    break;
                case 'improve_relations':
                    this.doImproveRelations(intV(payload, 'target_id'));
                    break;
                case 'form_alliance':
                    this.doFormAlliance(intV(payload, 'target_id'));
                    break;
                case 'form_non_aggression':
                    this.doFormTreaty(intV(payload, 'target_id'), 'NON_AGGRESSION');
                    break;
                case 'form_vassalage':
                    this.doFormTreaty(intV(payload, 'target_id'), 'VASSALAGE');
                    break;
                case 'form_trade_agreement':
                    this.doFormTreaty(intV(payload, 'target_id'), 'TRADE_AGREEMENT');
                    break;
                case 'form_intelligence_sharing':
                    this.doFormTreaty(intV(payload, 'target_id'), 'INTELLIGENCE_SHARING');
                    break;
                case 'arrange_marriage':
                    this.doArrangeMarriage(intV(payload, 'target_id'));
                    break;
                case 'send_gift':
                    this.doSendGift(intV(payload, 'target_id'), doubleVD(payload, 'amount', 50));
                    break;
                case 'set_rival':
                    this.doSetRival(intV(payload, 'target_id'));
                    break;
                case 'invite_to_court':
                    this.doInviteToCourt(intV(payload, 'target_id'));
                    break;
                case 'host_feast_for':
                    this.doHostFeastFor(intV(payload, 'target_id'));
                    break;
                case 'duel':
                    this.doDuel(intV(payload, 'target_id'));
                    break;
                case 'start_scheme':
                    this.doStartScheme(payload.scheme_kind, intV(payload, 'target_id'));
                    break;
                case 'appoint_council':
                    this.doAppointCouncil(payload.position, intVD(payload, 'character_id', NONE_ID));
                    break;
                case 'assign_council_task':
                    this.doAssignCouncilTask(payload.position, payload.task);
                    break;
                case 'grant_title':
                    this.doGrantTitle(intV(payload, 'title_id'), intV(payload, 'target_id'));
                    break;
                case 'hold_feast':
                    this.doHoldFeast();
                    break;
                case 'appease_faction':
                    this.doAppeaseFaction(intV(payload, 'faction_id'));
                    break;
                case 'develop_county':
                    this.doDevelopCounty(intV(payload, 'county_id'));
                    break;
                case 'upgrade_building':
                    this.doUpgradeBuilding(intV(payload, 'county_id'), payload.building_kind);
                    break;
                case 'upgrade_port':
                    this.doUpgradePort(intV(payload, 'county_id'));
                    break;
                case 'upgrade_trade_route_maintenance':
                    this.doUpgradeTradeRouteMaintenance(intV(payload, 'county_id'));
                    break;
                case 'fabricate_claim':
                    this.doFabricateClaim(intV(payload, 'county_id'));
                    break;
                case 'set_succession_law':
                    this.doSetSuccessionLaw(payload.law);
                    break;
                case 'set_crown_authority':
                    this.doSetCrownAuthority(intVD(payload, 'level', 0));
                    break;
                case 'set_gender_law':
                    this.doSetGenderLaw(payload.law);
                    break;
                case 'resolve_event':
                    this.doResolveEvent(intV(payload, 'event_id'), intV(payload, 'choice_id'));
                    break;
                case 'save':
                    this.doSave(payload.name);
                    break;
                case 'load':
                    this.doLoad(payload.name);
                    break;
                case 'delete_save':
                    this.doDeleteSave(payload.name);
                    break;
                case 'new_game':
                    this.doNewGame();
                    break;
                case 'toggle_cheat':
                    this.cheatMode = !this.cheatMode;
                    if (this.cheatMode) {
                        this.applyCheat();
                        this.notify('\u4f5c\u5f0a\u6a21\u5f0f\u5df2\u5f00\u542f');
                    }
                    else
                        this.notify('\u4f5c\u5f0a\u6a21\u5f0f\u5df2\u5173\u95ed');
                    break;
                case 'toggle_infinite_gold':
                    this.infiniteGoldMode = !this.infiniteGoldMode;
                    if (this.infiniteGoldMode) {
                        this.applyInfiniteGold();
                        this.notify('\u65e0\u9650\u91d1\u94b1\u6a21\u5f0f\u5df2\u5f00\u542f');
                    }
                    else
                        this.notify('\u65e0\u9650\u91d1\u94b1\u6a21\u5f0f\u5df2\u5173\u95ed');
                    break;
                case 'cheat_add_gold': {
                    const amt = doubleVD(payload, 'amount', 1000);
                    const p = this.sim.world.character(this.playerId);
                    if (p) {
                        p.gold += amt;
                        this.notify('\u4f5c\u5f0a\uff1a+' + Math.floor(amt) + ' \u91d1');
                    }
                    break;
                }
                default: this.notify('\u672a\u77e5\u64cd\u4f5c: ' + kind);
            }
        }
        catch (e) {
            this.notify('\u64cd\u4f5c\u5931\u8d25: ' + (e.message ?? e));
        }
        this.applyCheat();
        this.applyInfiniteGold();
        return this.snapshot();
    }
    // Action implementations
    doRaiseArmy(countyId) {
        const w = this.sim.world;
        let county = w.map.get(countyId);
        if (!county)
            throw new Error('\u7701\u4efd\u4e0d\u5b58\u5728');
        if (county.holder !== this.playerId) {
            const owned = w.map.list().filter(c => c.holder === this.playerId);
            if (owned.length === 0)
                throw new Error('\u6ca1\u6709\u53ef\u5f81\u53ec\u7684\u9886\u5730');
            countyId = owned[0].id;
            county = w.map.get(countyId);
        }
        if (this.sim.wars.armiesOf(this.playerId).length > 0)
            throw new Error('\u5df2\u6709\u91ce\u6218\u519b');
        let total = 0;
        for (const c of w.map.list()) {
            if (c.holder === this.playerId)
                total += c.monthlyLevies?.() ?? c.levies;
        }
        const levies = Math.max(200, total);
        const aid = this.sim.wars.raiseArmy(this.playerId, countyId, levies);
        const army = this.sim.wars.army(aid);
        if (army) {
            army.addMen('HEAVY_INFANTRY', Math.floor(levies / 10));
            army.addMen('ARCHERS', Math.floor(levies / 12));
            army.addMen('LIGHT_CAVALRY', Math.floor(levies / 20));
        }
        this.selectedArmy = aid;
        w.pushLog(this.name(this.playerId) + ' \u5728 ' + county.name + ' \u5f81\u53ec ' + levies + ' \u4eba');
        this.notify('\u5f81\u53ec\u6210\u529f\uff1a' + levies + ' \u4eba @ ' + county.name);
    }
    doMoveArmy(armyId, countyId) {
        const army = this.sim.wars.army(armyId);
        if (!army || !army.isActive())
            throw new Error('\u519b\u56e2\u4e0d\u5b58\u5728');
        if (army.owner !== this.playerId)
            throw new Error('\u53ea\u80fd\u8c03\u52a8\u81ea\u5df1\u7684\u519b\u56e2');
        const path = this.sim.world.map.path(army.location, countyId);
        if (!path || path.length === 0)
            throw new Error('\u65e0\u6cd5\u5230\u8fbe');
        army.setPath(path);
        const dest = this.sim.world.map.get(countyId);
        this.sim.world.pushLog(army.name + ' \u5411 ' + (dest?.name ?? '?') + ' \u8fdb\u519b');
        this.notify('\u4e0b\u4ee4\u8fdb\u519b\uff1a' + army.name + ' -> ' + (dest?.name ?? '?'));
        this.selectedArmy = armyId;
    }
    doDisbandArmy(armyId) { const army = this.sim.wars.army(armyId); if (!army || army.owner !== this.playerId)
        throw new Error('\u65e0\u6cd5\u89e3\u6563'); army.status = ArmyStatus.DISBANDED; if (army.stacks)
        army.stacks.length = 0; this.selectedArmy = -1; this.notify('\u5df2\u89e3\u6563 ' + army.name); }
    doSetCommander(armyId, characterId) { const army = this.sim.wars.army(armyId); if (!army || army.owner !== this.playerId)
        throw new Error('\u65e0\u6cd5\u6307\u6325'); const t = this.sim.world.character(characterId); if (!t || !t.isAlive())
        throw new Error('\u4eba\u9009\u65e0\u6548'); army.commander = characterId; this.notify('\u4efb\u547d ' + t.name + ' \u4e3a\u6307\u6325\u5b98'); }
    doRecruitKnights() { const p = this.sim.world.character(this.playerId); if (!p || p.gold < 25)
        throw new Error('\u91d1\u5e01\u4e0d\u8db3'); const aids = this.sim.wars.armiesOf(this.playerId); if (aids.length === 0)
        throw new Error('\u65e0\u91ce\u6218\u519b'); const army = this.sim.wars.army(aids[0]); p.gold -= 25; army.addMen('HEAVY_CAVALRY', 40); army.addMen('HEAVY_INFANTRY', 80); this.notify('\u62db\u52df\u7cbe\u9510\uff1a\u91cd\u9a91\u5175+40 \u91cd\u6b65\u5175+80'); }
    doDeclareWar(targetId) {
        if (targetId === this.playerId)
            throw new Error('\u4e0d\u80fd\u5bf9\u81ea\u5df1\u5ba3\u6218');
        const w = this.sim.world;
        if (this.sim.diplomacy.areAllied(this.playerId, targetId))
            throw new Error('\u540c\u76df\u65e0\u6cd5\u5ba3\u6218');
        if (!this.sim.diplomacy.canDeclareWar(this.playerId, targetId, w.date.year()))
            throw new Error('\u65e0\u6cd5\u5ba3\u6218');
        for (const wa of this.sim.wars.activeWars()) {
            if (wa.involves(this.playerId) || wa.involves(targetId))
                throw new Error('\u4e00\u65b9\u5df2\u5728\u6218\u4e89\u4e2d');
        }
        const player = w.character(this.playerId);
        const target = w.character(targetId);
        if (!player || !target)
            throw new Error('\u76ee\u6807\u65e0\u6548');
        const cb = this.sim.diplomacy.flags(this.playerId, targetId).rival ? CasusBelli.RIVALRY : CasusBelli.CONQUEST;
        if (player.prestige < cbPrestigeCost(cb))
            throw new Error('\u5a01\u671b\u4e0d\u8db3');
        player.prestige -= cbPrestigeCost(cb);
        const warName = player.name + ' \u5bf9 ' + target.name + ' \u7684' + cbNameZh(cb);
        const wid = this.sim.wars.declareWar(cb, this.playerId, targetId, w.date, warName);
        this.sim.diplomacy.setAtWar(this.playerId, targetId, true);
        const war = this.sim.wars.war(wid);
        if (war)
            for (const ally of this.sim.diplomacy.alliesOf(this.playerId)) {
                if (ally !== targetId && !war.involves(ally)) {
                    war.participants.push(new WarParticipant(ally, true, w.date));
                    this.sim.diplomacy.setAtWar(ally, targetId, true);
                }
            }
        w.pushLog('\u5ba3\u6218\uff01' + warName);
        this.sim.diplomacy.addWarExhaustion(this.playerId);
        this.sim.diplomacy.addWarExhaustion(targetId);
        this.notify('\u5df2\u5bf9 ' + target.name + ' \u5ba3\u6218');
        this.grantXp(50);
    }
    doWhitePeace(warId) { const w = this.sim.wars.war(warId); if (!w || !w.active)
        throw new Error('\u6218\u4e89\u4e0d\u5b58\u5728'); if (!w.involves(this.playerId))
        throw new Error('\u65e0\u6743\u63d0\u8bae'); this.sim.wars.endWar(warId, WarResult.WHITE_PEACE); this.sim.diplomacy.setAtWar(w.attackerPrimary, w.defenderPrimary, false); if (this.sim.diplomacy.setTruce)
        this.sim.diplomacy.setTruce(w.attackerPrimary, w.defenderPrimary, this.sim.world.date.year() + 3); this.notify('\u5df2\u8fbe\u6210\u767d\u548c'); }
    doImproveRelations(targetId) { const p = this.sim.world.character(this.playerId); if (!p || p.gold < 10)
        throw new Error('\u91d1\u5e01\u4e0d\u8db3'); p.gold -= 10; this.sim.world.modifyOpinion(targetId, this.playerId, 15); this.sim.world.modifyOpinion(this.playerId, targetId, 5); this.notify('\u6539\u5584\u4e0e ' + this.name(targetId) + ' \u7684\u5173\u7cfb'); this.grantXp(20); }
    doFormAlliance(targetId) { if (targetId === this.playerId)
        throw new Error('\u4e0d\u80fd\u4e0e\u81ea\u5df1\u7ed3\u76df'); if (this.sim.diplomacy.areAllied(this.playerId, targetId))
        throw new Error('\u5df2\u662f\u540c\u76df'); this.sim.diplomacy.formAlliance(this.playerId, targetId, this.sim.world.date); this.sim.world.modifyOpinion(this.playerId, targetId, 20); this.sim.world.modifyOpinion(targetId, this.playerId, 20); this.notify('\u4e0e ' + this.name(targetId) + ' \u7ed3\u76df'); }
    doFormTreaty(targetId, kind) { if (this.sim.diplomacy.addTreaty) {
        const tk = TreatyKind[kind] ?? kind;
        this.sim.diplomacy.addTreaty(new Treaty(this.playerId, targetId, tk, this.sim.world.date, this.sim.world.date.year() + 5));
        this.notify('\u5df2\u7b7e\u8ba2' + kind);
    }
    else
        this.notify('\u6761\u7ea6\u7cfb\u7edf\u4e0d\u53ef\u7528'); }
    doArrangeMarriage(targetId) { if (this.sim.world.marry(this.playerId, targetId)) {
        this.sim.diplomacy.flagsMut(this.playerId, targetId).marriagePact = true;
        this.notify('\u4e0e ' + this.name(targetId) + ' \u8054\u59fb');
    }
    else
        throw new Error('\u65e0\u6cd5\u8054\u59fb'); }
    doSendGift(targetId, amount) { const p = this.sim.world.character(this.playerId); if (!p || p.gold < amount)
        throw new Error('\u91d1\u5e01\u4e0d\u8db3'); p.gold -= amount; this.sim.world.modifyOpinion(targetId, this.playerId, Math.floor(amount / 3)); this.notify('\u5411 ' + this.name(targetId) + ' \u8d60\u793c ' + amount + ' \u91d1'); }
    doSetRival(targetId) { this.sim.diplomacy.setRival(this.playerId, targetId); this.notify('\u8bbe ' + this.name(targetId) + ' \u4e3a\u5bbf\u654c'); }
    doInviteToCourt(targetId) { const p = this.sim.world.character(this.playerId); if (!p || p.gold < 30)
        throw new Error('\u91d1\u5e01\u4e0d\u8db3'); p.gold -= 30; this.sim.world.modifyOpinion(targetId, this.playerId, 15); this.notify('\u9080\u8bf7 ' + this.name(targetId) + ' \u5165\u5bab\u5ef7'); }
    doHostFeastFor(targetId) { const p = this.sim.world.character(this.playerId); if (!p || p.gold < 40)
        throw new Error('\u91d1\u5e01\u4e0d\u8db3'); p.gold -= 40; this.sim.world.modifyOpinion(targetId, this.playerId, 20); p.prestige += 10; this.notify('\u4e3a ' + this.name(targetId) + ' \u4e3e\u529e\u5bb4\u4f1a'); }
    doDuel(targetId) { const p = this.sim.world.character(this.playerId); const t = this.sim.world.character(targetId); if (!p || !t)
        throw new Error('\u76ee\u6807\u65e0\u6548'); const pPow = p.baseAttrs.prowess + Math.floor(Math.random() * 10); const tPow = t.baseAttrs.prowess + Math.floor(Math.random() * 10); if (pPow >= tPow) {
        p.prestige += 20;
        t.prestige -= 10;
        this.notify('\u51b3\u6597\u80dc\u5229\uff01\u5a01\u671b+20');
    }
    else {
        p.prestige -= 10;
        p.stress += 15;
        this.notify('\u51b3\u6597\u5931\u8d25\uff01');
    } }
    doStartScheme(schemeKind, targetId) { if (!schemeKind)
        throw new Error('\u7f3a\u5c11\u9634\u8c0b\u7c7b\u578b'); const kind = SchemeKind[schemeKind]; if (kind == null)
        throw new Error('\u672a\u77e5\u9634\u8c0b\u7c7b\u578b'); this.sim.schemes.start(kind, this.playerId, targetId, this.sim.world.date); this.notify('\u9634\u8c0b\u5df2\u53d1\u8d77'); this.grantXp(30); }
    doAppointCouncil(position, characterId) { const council = this.sim.councils.get?.(this.playerId) ?? this.sim.councils.councilFor(this.playerId); if (!council)
        throw new Error('\u65e0\u5185\u9601'); const pos = CouncilPosition[position]; if (pos != null && council.set)
        council.set(pos, characterId); this.notify('\u5df2\u4efb\u547d\u5b98\u5458'); }
    doAssignCouncilTask(position, task) { const council = this.sim.councils.get?.(this.playerId) ?? this.sim.councils.councilFor(this.playerId); if (!council)
        throw new Error('\u65e0\u5185\u9601'); const pos = CouncilPosition[position]; const tk = CouncilTask[task]; if (pos != null && tk != null && council.tasks)
        council.tasks.set(pos, tk); this.notify('\u4efb\u52a1\u5df2\u5206\u914d'); }
    doGrantTitle(titleId, targetId) { const t = this.sim.world.title(titleId); if (!t)
        throw new Error('\u5934\u8854\u4e0d\u5b58\u5728'); this.sim.world.grantTitle(titleId, targetId); this.notify('\u5c06\u300c' + t.name + '\u300d\u6388\u4e88 ' + this.name(targetId)); }
    doHoldFeast() { const p = this.sim.world.character(this.playerId); if (!p || p.gold < 20)
        throw new Error('\u91d1\u5e01\u4e0d\u8db3'); p.gold -= 20; p.prestige += 15; p.stress = Math.max(0, p.stress - 10); this.sim.world.pushLog(p.name + ' \u4e3e\u529e\u4e86\u5bb4\u4f1a'); this.notify('\u4e3e\u529e\u5bb4\u4f1a\uff1a\u5a01\u671b+15\uff0c\u538b\u529b-10'); }
    doAppeaseFaction(factionId) { const f = this.sim.factions.factions?.()?.get?.(factionId); if (!f)
        throw new Error('\u6d3e\u7cfb\u4e0d\u5b58\u5728'); const p = this.sim.world.character(this.playerId); if (!p || p.gold < 25)
        throw new Error('\u91d1\u5e01\u4e0d\u8db3'); p.gold -= 25; for (const mid of f.members)
        this.sim.world.modifyOpinion(mid, this.playerId, 12); this.sim.factions.appease?.(factionId, 30); this.notify('\u6d3e\u7cfb\u4e0d\u6ee1\u4e0b\u964d'); }
    doDevelopCounty(countyId) { const county = this.sim.world.map.get(countyId); if (!county)
        throw new Error('\u7701\u4efd\u4e0d\u5b58\u5728'); if (county.holder !== this.playerId)
        throw new Error('\u4e0d\u662f\u5df1\u65b9\u9886\u5730'); const p = this.sim.world.character(this.playerId); if (!p || p.gold < 10)
        throw new Error('\u91d1\u5e01\u4e0d\u8db3'); const cap = county.terrain?.developmentCap?.() ?? 30; if (county.development >= cap)
        throw new Error('\u5df2\u8fbe\u4e0a\u9650'); p.gold -= 10; county.development = Math.min(cap, county.development + 1); this.notify(county.name + ' \u53d1\u5c55\u5ea6 +1'); }
    doUpgradeBuilding(countyId, buildingKindName) { const county = this.sim.world.map.get(countyId); if (!county)
        throw new Error('\u7701\u4efd\u4e0d\u5b58\u5728'); if (county.holder !== this.playerId)
        throw new Error('\u4e0d\u662f\u5df1\u65b9\u9886\u5730'); if (!buildingKindName)
        throw new Error('\u7f3a\u5c11\u5efa\u7b51\u7c7b\u578b'); const p = this.sim.world.character(this.playerId); if (!p)
        throw new Error('\u73a9\u5bb6\u65e0\u6548'); const kind = BuildingKind[buildingKindName]; if (!kind)
        throw new Error('\u672a\u77e5\u5efa\u7b51\u7c7b\u578b'); const b = this.sim.buildings.getBuilding?.(countyId, kind); if (!b) {
        const cost = kind.upgradeCost?.(0) ?? 50;
        if (p.gold < cost)
            throw new Error('\u91d1\u5e01\u4e0d\u8db3');
        p.gold -= cost;
        this.sim.buildings.addBuilding?.(countyId, kind);
        this.notify('\u5efa\u9020 ' + (kind.nameZh?.() ?? buildingKindName));
    }
    else {
        if (!b.canUpgrade?.())
            throw new Error('\u5df2\u8fbe\u6700\u9ad8\u7ea7');
        const cost = b.upgradeCost?.() ?? 50;
        if (p.gold < cost)
            throw new Error('\u91d1\u5e01\u4e0d\u8db3');
        p.gold -= cost;
        b.level += 1;
        this.notify((kind.nameZh?.() ?? buildingKindName) + ' \u5347\u7ea7\u5230 ' + b.level + ' \u7ea7');
    } this.grantXp(20); }
    doUpgradePort(countyId) { const county = this.sim.world.map.get(countyId); if (!county)
        throw new Error('\u7701\u4efd\u4e0d\u5b58\u5728'); if (county.holder !== this.playerId)
        throw new Error('\u4e0d\u662f\u5df1\u65b9\u9886\u5730'); if (!county.hasPort)
        throw new Error('\u65e0\u6e2f\u53e3'); const p = this.sim.world.character(this.playerId); const cost = county.upgradePort?.() ?? 30; if (cost <= 0)
        throw new Error('\u5df2\u8fbe\u6700\u9ad8\u7ea7'); if (p && p.gold >= cost) {
        p.gold -= cost;
        this.notify(county.name + ' \u6e2f\u53e3\u5347\u7ea7');
    }
    else
        throw new Error('\u91d1\u5e01\u4e0d\u8db3'); }
    doUpgradeTradeRouteMaintenance(countyId) { const county = this.sim.world.map.get(countyId); if (!county)
        throw new Error('\u7701\u4efd\u4e0d\u5b58\u5728'); if (county.holder !== this.playerId)
        throw new Error('\u4e0d\u662f\u5df1\u65b9\u9886\u5730'); const p = this.sim.world.character(this.playerId); const cost = county.upgradeTradeRouteMaintenance?.() ?? 20; if (cost <= 0)
        throw new Error('\u5df2\u8fbe\u6700\u9ad8\u7ea7'); if (p && p.gold >= cost) {
        p.gold -= cost;
        this.notify(county.name + ' \u8d38\u6613\u8def\u7ebf\u5347\u7ea7');
    }
    else
        throw new Error('\u91d1\u5e01\u4e0d\u8db3'); }
    doFabricateClaim(countyId) { const county = this.sim.world.map.get(countyId); if (!county)
        throw new Error('\u7701\u4efd\u4e0d\u5b58\u5728'); if (county.holder === this.playerId)
        throw new Error('\u5df2\u662f\u5df1\u65b9\u9886\u5730'); const p = this.sim.world.character(this.playerId); if (!p || p.gold < 50)
        throw new Error('\u91d1\u5e01\u4e0d\u8db3'); p.gold -= 50; this.sim.diplomacy.addClaim(this.playerId, county.ownerTitle, countyId, 60); this.sim.world.pushLog(p.name + ' \u4f2a\u9020\u4e86\u5bf9 ' + county.name + ' \u7684\u5ba3\u79f0'); this.notify('\u5df2\u4f2a\u9020\u5ba3\u79f0'); this.grantXp(40); }
    doSetSuccessionLaw(lawName) { if (!lawName)
        throw new Error('\u7f3a\u5c11 law'); const newLaw = SuccessionLaw[lawName]; if (newLaw == null)
        throw new Error('\u672a\u77e5\u7ee7\u627f\u6cd5'); const old = this.getEffectiveLaw(); const updated = new RealmLaw(newLaw, old.crownAuthority, old.genderLaw, old.partitionEnabled); this.sim.realmLaws.set(this.playerId, updated); const t = this.playerTitle(); if (t)
        t.realmLaw = updated; this.notify('\u7ee7\u627f\u6cd5\u5df2\u6539\u4e3a\uff1a' + lawName); }
    doSetCrownAuthority(level) { const ca = CrownAuthority[level] ?? Object.values(CrownAuthority ?? {})[level]; if (!ca)
        throw new Error('\u672a\u77e5\u738b\u6743\u7b49\u7ea7'); const old = this.getEffectiveLaw(); const updated = new RealmLaw(old.succession, ca, old.genderLaw, old.partitionEnabled); this.sim.realmLaws.set(this.playerId, updated); const t = this.playerTitle(); if (t)
        t.realmLaw = updated; this.notify('\u738b\u6743\u5df2\u6539\u4e3a\uff1a' + level); }
    doSetGenderLaw(lawName) { if (!lawName)
        throw new Error('\u7f3a\u5c11 law'); const newLaw = GenderLaw[lawName]; if (newLaw == null)
        throw new Error('\u672a\u77e5\u6027\u522b\u6cd5'); const old = this.getEffectiveLaw(); const updated = new RealmLaw(old.succession, old.crownAuthority, newLaw, old.partitionEnabled); this.sim.realmLaws.set(this.playerId, updated); const t = this.playerTitle(); if (t)
        t.realmLaw = updated; this.notify('\u6027\u522b\u6cd5\u5df2\u6539\u4e3a\uff1a' + lawName); }
    doResolveEvent(eventId, choiceId) { let inst = null; const pending = this.sim.events.pending; if (pending)
        for (const e of pending) {
            if (e.eventId === eventId && e.character === this.playerId) {
                inst = e;
                break;
            }
        } if (!inst)
        throw new Error('\u4e8b\u4ef6\u4e0d\u5b58\u5728'); this.sim.events.resolveChoice(this.sim.world, inst, choiceId); pending.delete?.(inst) ?? pending.splice(pending.indexOf(inst), 1); this.notify('\u5df2\u9009\u62e9: ' + inst.title); this.grantXp(30); }
    doSave(slotName) { try {
        SaveManager.save(this.sim, this.playerId, slotName);
        this.notify('\u5df2\u5b58\u6863');
    }
    catch (e) {
        throw new Error('\u5b58\u6863\u5931\u8d25: ' + e.message);
    } }
    doLoad(slotName) { try {
        const lg = SaveManager.load(slotName ?? 'autosave');
        if (!lg)
            throw new Error('\u6ca1\u6709\u5b58\u6863');
        this.sim = lg.sim;
        this.playerId = lg.playerId;
        this.syncPlayer();
        this.selectedCounty = -1;
        this.selectedArmy = -1;
        this.notify('\u5df2\u8bfb\u6863');
    }
    catch (e) {
        throw new Error('\u8bfb\u6863\u5931\u8d25: ' + e.message);
    } }
    doDeleteSave(slotName) { if (!slotName)
        throw new Error('\u7f3a\u5c11\u5b58\u6863\u540d'); if (!SaveManager.deleteSave(slotName))
        throw new Error('\u65e0\u6cd5\u5220\u9664\u5b58\u6863'); this.notify('\u5df2\u5220\u9664\u5b58\u6863 ' + slotName); }
    doNewGame() { this.sim = new GameSimulation(); this.playerId = this.defaultPlayer(); this.syncPlayer(); this.selectedCounty = -1; this.selectedArmy = -1; this.messages = ['\u65b0\u5c40\u5f00\u59cb\u3002']; }
    // Helpers
    defaultPlayer() { for (const c of this.sim.world.aliveCharacters()) {
        if (c.name.includes('\u54c8\u7f57\u5fb7'))
            return c.id;
    } const rulers = this.sim.world.rulers(); return rulers.length > 0 ? rulers[0].id : 1; }
    syncPlayer() { this.sim.playerIds.clear(); this.sim.playerIds.add(this.playerId); }
    notify(msg) { this.messages.push(msg); if (this.messages.length > 80)
        this.messages.splice(0, this.messages.length - 60); }
    name(charId) { return this.sim.world.character(charId)?.name ?? '?'; }
    getMessages() { const from = Math.max(0, this.messages.length - 12); return this.messages.slice(from); }
    holderColor(holderId) { if (holderId === NONE_ID)
        return '#4a5568'; const c = this.sim.world.character(holderId); if (!c)
        return '#4a5568'; const d = this.sim.world.dynasties.get(c.dynasty); if (d && (d.colorR !== 128 || d.colorG !== 128 || d.colorB !== 128))
        return 'rgb(' + d.colorR + ',' + d.colorG + ',' + d.colorB + ')'; const hue = (holderId * 47) % 360; return 'hsl(' + hue + ' 55% 42%)'; }
    playerTitle() { const p = this.sim.world.character(this.playerId); if (!p || p.primaryTitle === NONE_ID)
        return null; return this.sim.world.title(p.primaryTitle); }
    getEffectiveLaw() { const law = this.sim.realmLaws.get(this.playerId); if (law)
        return law; const t = this.playerTitle(); if (t?.realmLaw)
        return t.realmLaw; return RealmLaw.feudalDefault(); }
    playerLaws() { const law = this.getEffectiveLaw(); return { succession: law.succession, crown_authority: law.crownAuthority, gender_law: law.genderLaw }; }
    applyCheat() { if (!this.cheatMode)
        return; const p = this.sim.world.character(this.playerId); if (!p)
        return; p.gold = CHEAT_GOLD; p.prestige = CHEAT_PRESTIGE; p.piety = CHEAT_PIETY; p.stress = 0; }
    applyInfiniteGold() { if (!this.infiniteGoldMode)
        return; const p = this.sim.world.character(this.playerId); if (!p)
        return; p.gold = CHEAT_GOLD; }
    grantXp(amount) { const p = this.sim.world.character(this.playerId); if (!p)
        return; if (p.gainXp?.(amount))
        this.notify('\u89d2\u8272\u5347\u7ea7\uff01\u7b49\u7ea7\uff1a' + p.level); }
    countyBuildings(countyId) { const bs = this.sim.buildings.getBuildings?.(countyId) ?? []; return bs.map((b) => ({ kind: b.kind?.name ?? '', name: b.kind?.nameZh?.() ?? '', level: b.level, maxLevel: b.kind?.maxLevel?.() ?? 3, canUpgrade: b.canUpgrade?.() ?? false, upgradeCost: b.upgradeCost?.() ?? 0, description: b.kind?.description?.() ?? '' })); }
    playerSchemes() { const out = []; for (const s of this.sim.schemes.schemeValues()) {
        if (s.owner !== this.playerId || s.exposed || s.isComplete())
            continue;
        const t = this.sim.world.character(s.target);
        out.push({ id: s.id, kind: String(s.kind), kindZh: schemeKindNameZh(s.kind), targetId: s.target, targetName: t?.name ?? '?', progress: r1(s.progress), secrecy: r1(s.secrecy) });
    } return out; }
    playerCouncil() { const council = this.sim.councils.get?.(this.playerId) ?? this.sim.councils.councilFor?.(this.playerId); if (!council)
        return null; return { members: [] }; }
    playerClaims() { const out = []; const claims = this.sim.diplomacy.claimsOf(this.playerId); for (const cl of claims) {
        const title = cl.title !== NONE_ID ? this.sim.world.title(cl.title) : null;
        const countyId = cl.county ?? NONE_ID;
        const county = countyId !== NONE_ID ? this.sim.world.map.get(countyId) : null;
        out.push({ titleId: cl.title !== NONE_ID ? cl.title : null, titleName: title?.name ?? null, countyId: countyId, countyName: county?.name ?? null, strength: cl.strength, pressed: cl.pressed });
    } return out; }
    playerTreaties() { const out = []; const treaties = this.sim.diplomacy.getTreaties(); for (const t of treaties) {
        if (t.a !== this.playerId && t.b !== this.playerId)
            continue;
        const oid = t.a === this.playerId ? t.b : t.a;
        const other = this.sim.world.character(oid);
        out.push({ kind: String(t.kind), kindZh: String(t.kind), otherId: oid, otherName: other?.name ?? '?', expiresYear: t.expiresYear });
    } return out; }
    allCharacters() { const w = this.sim.world; const out = []; for (const c of w.aliveCharacters()) {
        const attrs = w.effectiveAttrs?.(c.id);
        const dynasty = w.dynasties.get(c.dynasty);
        const title = c.primaryTitle !== NONE_ID ? w.title(c.primaryTitle) : null;
        out.push({ id: c.id, name: c.name, dynasty_name: dynasty?.name ?? '', gender: c.gender, age: c.birth ? w.date.year() - c.birth.year() : 0, isRuler: c.isRuler, title: title?.name ?? '', gold: Math.floor(c.gold), prestige: Math.floor(c.prestige), isMarried: c.spouses.length > 0, spouse_ids: [...c.spouses], attrs: attrs ? { diplomacy: attrs.diplomacy, martial: attrs.martial, stewardship: attrs.stewardship, intrigue: attrs.intrigue, learning: attrs.learning, prowess: attrs.prowess } : null, opinion_of_player: w.opinion(c.id, this.playerId), player_opinion: w.opinion(this.playerId, c.id), relation_allied: this.sim.diplomacy.flags(this.playerId, c.id).allied, relation_rival: this.sim.diplomacy.flags(this.playerId, c.id).rival, relation_at_war: this.sim.diplomacy.flags(this.playerId, c.id).atWar, relation_marriage: this.sim.diplomacy.flags(this.playerId, c.id).marriagePact, relation_vassalage: this.sim.diplomacy.flags(this.playerId, c.id).vassalage ?? false, relation_trade_agreement: this.sim.diplomacy.flags(this.playerId, c.id).tradeAgreement ?? false, relation_intelligence_sharing: this.sim.diplomacy.flags(this.playerId, c.id).intelligenceSharing ?? false, held_title_ids: [...c.heldTitles], level: c.level, xp: c.xp, xpToNext: c.xpToNextLevel?.() ?? 100, is_alive: c.life === 'ALIVE' });
    } return out; }
    playerStorylines() { const out = []; for (const s of this.sim.storylines.storylines) {
        if (s.characterId !== 0 && s.characterId !== this.playerId)
            continue;
        out.push({ id: s.id, title: s.title, description: s.description, status: String(s.status), currentStage: s.currentStage, tags: [...(s.tags ?? [])] });
    } return out; }
    schemeTypes() { return Object.values(SchemeKind).map(v => ({ value: v.name?.() ?? v, name: v.nameZh?.() ?? String(v) })); }
    councilPositions() { return Object.values(CouncilPosition ?? {}).map((v) => ({ value: v?.name?.() ?? v, name: v?.nameZh?.() ?? String(v) })); }
    councilTasks() { return Object.values(CouncilTask ?? {}).map((v) => ({ value: v?.name?.() ?? v, name: v?.nameZh?.() ?? String(v) })); }
}
function r1(v) { return Math.round(v * 10) / 10; }
function r2(v) { return Math.round(v * 100) / 100; }
function intV(m, k) { const v = m[k]; if (typeof v === 'number')
    return v; throw new Error('\u7f3a\u5c11\u53c2\u6570: ' + k); }
function intVD(m, k, d) { const v = m[k]; return typeof v === 'number' ? v : d; }
function doubleVD(m, k, d) { const v = m[k]; return typeof v === 'number' ? v : d; }
//# sourceMappingURL=GameAPI.js.map