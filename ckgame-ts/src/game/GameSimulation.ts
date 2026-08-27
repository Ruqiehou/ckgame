import { AiDirector } from '../ai/AiDirector.js';
import { NONE_ID } from '../core/Constants.js';
import { Balance } from '../core/balance/Balance.js';
import { Season } from '../core/calendar/Season.js';
import { TitleTier } from '../core/TitleTier.js';
import { Scenario1066 } from './Scenario1066.js';
import { ArmyStatus } from '../military/ArmyStatus.js';
import { WarResult } from '../military/WarResult.js';
import { CasusBelli, cbAttackerPrestigeOnWin } from '../politics/CasusBelli.js';
import { WarManager } from '../military/WarManager.js';
import { SiegeManager } from '../military/SiegeManager.js';
import { BattleSimulator } from '../military/BattleSimulator.js';
import { EventEngine } from '../events/EventEngine.js';
import { StorylineSystem } from '../events/StorylineSystem.js';
import { FactionManager } from '../politics/FactionManager.js';
import { SchemeManager } from '../politics/SchemeManager.js';
import { Diplomacy } from '../politics/Diplomacy.js';
import { CouncilManager } from '../politics/CouncilManager.js';
import { BuildingSystem } from '../world/buildings/BuildingSystem.js';
import { RealmLaw, crownAuthorityTaxBonus, crownAuthorityVassalOpinionPenalty } from '../politics/Laws.js';
import { SchemeKind } from '../politics/SchemeKind.js';
import { factionKindNameZh } from '../politics/FactionKind.js';
import { WarParticipant } from '../military/WarParticipant.js';
import type { World } from '../world/World.js';
import type { Character } from '../world/Character.js';
import type { Army } from '../military/Army.js';
import type { War } from '../military/War.js';
import type { EventInstance } from '../events/EventInstance.js';
import type { EventChoice } from '../events/EventChoice.js';
import type { Council } from '../politics/Council.js';

export class GameSimulation {
  world: World;
  wars: WarManager;
  sieges: SiegeManager;
  events: EventEngine;
  factions: FactionManager;
  schemes: SchemeManager;
  diplomacy: Diplomacy;
  councils: CouncilManager;
  buildings: BuildingSystem;
  storylines: StorylineSystem;
  realmLaws: Map<number, RealmLaw> = new Map();
  playerIds: Set<number> = new Set();

  constructor() {
    this.world = Scenario1066.build();
    this.wars = new WarManager();
    this.sieges = new SiegeManager();
    this.events = new EventEngine();
    this.factions = new FactionManager(this.world);
    this.schemes = new SchemeManager(this.world);
    this.diplomacy = new Diplomacy(this.world);
    this.councils = new CouncilManager(this.world);
    this.buildings = new BuildingSystem();
    this.storylines = new StorylineSystem();
    this.bootstrap();
  }

  bootstrap(): void {
    let william = NONE_ID;
    let harold = NONE_ID;
    for (const c of this.world.aliveCharacters()) {
      if (william === NONE_ID && c.name.includes('\u5a01\u5ec9\u00b7\u5f81\u670d\u8005')) william = c.id;
      if (harold === NONE_ID && c.name.includes('\u54c8\u7f57\u5fb7')) harold = c.id;
    }
    if (william !== NONE_ID && harold !== NONE_ID) {
      this.diplomacy.setRival(william, harold);
      let eng = NONE_ID;
      for (const [, t] of this.world.titles) {
        if (t.name.includes('\u82f1\u683c\u5170')) { eng = t.id; break; }
      }
      if (eng !== NONE_ID) this.diplomacy.addClaim(william, eng, 80);
    }
    let edwin = NONE_ID;
    let morcar = NONE_ID;
    for (const c of this.world.aliveCharacters()) {
      if (edwin === NONE_ID && c.name.includes('\u57c3\u5fb7\u6e29')) edwin = c.id;
      if (morcar === NONE_ID && c.name.includes('\u83ab\u5361')) morcar = c.id;
    }
    if (edwin !== NONE_ID && morcar !== NONE_ID) this.diplomacy.formAlliance(edwin, morcar, this.world.date);
    for (const r of [...this.world.rulers()]) {
      this.ensureCouncil(r.id);
      if (!this.realmLaws.has(r.id)) {
        this.realmLaws.set(r.id, RealmLaw.feudalDefault());
      }
    }
    for (const sl of StorylineSystem.builtinStorylines()) {
      this.storylines.createStoryline(sl);
    }
  }

  ensureCouncil(ruler: number): void {
    const candidates: number[][] = [];
    for (const c of this.world.aliveCharacters()) {
      if (c.id === ruler || !isAdult(c, this.world)) continue;
      const a = this.world.effectiveAttrs ? this.world.effectiveAttrs(c.id) : null;
      if (!a) continue;
      candidates.push([c.id, a.diplomacy, a.martial, a.stewardship, a.intrigue, a.learning]);
    }
    const council = this.councils.councilFor(ruler);
    if (council && council.members().length === 0 && council.autoAppoint) council.autoAppoint(candidates);
  }

  runDays(days: number): void { for (let i = 0; i < days; i++) this.tickDay(); }

  tickDay(): void {
    this.world.date = this.world.date.advanceOneDay();
    (this.world as any).tick = ((this.world as any).tick ?? 0) + 1;
    const season = this.world.date.season();
    const winter = season === Season.WINTER;
    if (this.wars.tickMovement) {
      this.wars.tickMovement((army: any) => {
        let chance = 1.0;
        if (season === Season.WINTER) chance *= Balance.MOVE_CHANCE_WINTER;
        else if (season === Season.AUTUMN) chance *= Balance.MOVE_CHANCE_AUTUMN;
        if (army.supply < Balance.SUPPLY_MOVE_SLOW_THRESHOLD) chance *= Balance.MOVE_CHANCE_LOW_SUPPLY;
        return Math.max(Balance.MOVE_CHANCE_MIN, chance);
      });
    }
    this.tickArmySupply(winter);
    this.resolveEncounters();
    this.tickSieges();
    if (this.wars.disbandEmpty) this.wars.disbandEmpty();
    if (this.events.tickCooldowns) this.events.tickCooldowns();
    if (this.world.date.isMonthStart()) this.tickMonth();
    if (this.world.date.isYearStart()) {
      this.world.pushLog('\u2014\u2014 ' + this.world.date.year() + ' \u5e74\u6765\u4e34 \u2014\u2014');
      if (this.diplomacy.expireTreaties) {
        for (const line of this.diplomacy.expireTreaties(this.world.date.year(), this.world)) this.world.pushLog(line);
      }
    }
  }

  tickArmySupply(winter: boolean): void {
    const enemyHolders = new Map<number, Set<number>>();
    for (const w of this.wars.activeWars()) {
      const atk = new Set<number>(); const dfd = new Set<number>();
      for (const p of w.participants) { if (p.isAttacker) atk.add(p.character); else dfd.add(p.character); }
      for (const a of atk) { if (!enemyHolders.has(a)) enemyHolders.set(a, new Set()); for (const d of dfd) enemyHolders.get(a)!.add(d); }
      for (const d of dfd) { if (!enemyHolders.has(d)) enemyHolders.set(d, new Set()); for (const a of atk) enemyHolders.get(d)!.add(a); }
    }
    const armies = (this.wars as any).armies;
    if (!armies) return;
    for (const [, army] of armies) {
      if (!army.isActive()) continue;
      const county = this.world.map.get(army.location);
      if (!county) continue;
      const enemies = enemyHolders.get(army.owner) ?? new Set<number>();
      const inFriendly = !enemies.has(county.holder);
      if (army.applySupplyTick) army.applySupplyTick(inFriendly, winter);
    }
  }

  tickMonth(): void {
    if (this.world.processHealth) this.world.processHealth();
    // Army maintenance
    const byOwner = new Map<number, Army[]>();
    const armies = (this.wars as any).armies;
    if (armies) {
      for (const [, army] of armies) {
        if (army.isActive()) { const arr = byOwner.get(army.owner) ?? []; arr.push(army); byOwner.set(army.owner, arr); }
      }
    }
    for (const [oid, arr] of byOwner) {
      const c = this.world.character(oid); if (!c) continue;
      let cost = 0; for (const a of arr) cost += (a.monthlyMaintenance?.() ?? a.totalMen() * 0.01);
      cost *= Balance.ARMY_MAINTENANCE_MULT;
      if (c.gold >= cost) { c.gold -= cost; continue; }
      c.gold = 0;
      arr.sort((a: any, b: any) => b.totalMen() - a.totalMen());
      const dis = arr[0]; dis.status = ArmyStatus.DISBANDED; if (dis.stacks) dis.stacks.length = 0;
      this.world.pushLog(c.name + ' \u56fd\u5e93\u7a7a\u865a\uff0c\u88ab\u8feb\u89e3\u6563 ' + dis.name);
    }
    if (this.world.processMonthlyEconomy) this.world.processMonthlyEconomy();
    if (this.world.processFertility) this.world.processFertility();
    for (const c of this.world.aliveCharacters()) { if (c.gainXp) c.gainXp(5); }
    for (const [rid, law] of this.realmLaws) {
      if (crownAuthorityTaxBonus(law?.crownAuthority) > 0) {
        const income = (this.world.monthlyIncomeOf?.(rid) ?? 0) * crownAuthorityTaxBonus(law.crownAuthority);
        const c = this.world.character(rid); if (c) c.gold += income;
      }
    }
    this.tickCouncils();
    const chars: number[] = [];
    for (const c of this.world.aliveCharacters()) {
      if (c.isRuler || isAdult(c, this.world)) chars.push(c.id);
      if (chars.length >= 50) break;
    }
    if (this.events.dailyCheck) this.events.dailyCheck(this.world, chars);
    const aiPending: EventInstance[] = [];
    const pending = (this.events as any).pending;
    if (pending) {
      const toRemove: EventInstance[] = [];
      for (const inst of pending) { if (!this.playerIds.has(inst.character)) { aiPending.push(inst); toRemove.push(inst); } }
      for (const inst of toRemove) pending.delete?.(inst) ?? pending.splice(pending.indexOf(inst), 1);
    }
    for (const inst of aiPending) {
      const best = inst.choices.reduce((a: any, b: any) => a.aiWeight > b.aiWeight ? a : b);
      if (this.events.resolveChoice) this.events.resolveChoice(this.world, inst, best.id);
    }
    this.tickFactions();
    this.tickSchemes();
    const actions = AiDirector.monthlyActions(this.world, this.wars, this.diplomacy, this.schemes, new Set(this.playerIds));
    AiDirector.applyActions(this.world, this.wars, this.diplomacy, this.schemes, actions);
    this.tickWars();
    const atWarIds = new Set<number>();
    for (const w of this.wars.activeWars()) { atWarIds.add(w.attackerPrimary); atWarIds.add(w.defenderPrimary); }
    if (this.diplomacy.tickWarExhaustion) this.diplomacy.tickWarExhaustion(atWarIds);
    this.tryStartSieges();
    for (const r of [...this.world.rulers()]) {
      this.ensureCouncil(r.id);
      if (!this.realmLaws.has(r.id)) { this.realmLaws.set(r.id, RealmLaw.feudalDefault()); }
    }
  }

  tickCouncils(): void {
    const skillMap = new Map<number, any>();
    for (const c of this.world.aliveCharacters()) { const a = this.world.effectiveAttrs ? this.world.effectiveAttrs(c.id) : null; if (a) skillMap.set(c.id, a); }
    const allCouncils = this.councils.allCouncils?.() ?? {};
    for (const [rid, council] of Object.entries(allCouncils)) {
      const rulerId = Number(rid);
      const effect = (council as any).monthlyEffect?.(skillMap) ?? { gold: 0, prestige: 0, piety: 0, controlGain: 0, developmentChance: 0, claimProgress: 0, logs: [] };
      const c = this.world.character(rulerId);
      if (c) { c.gold += effect.gold ?? 0; c.prestige += effect.prestige ?? 0; c.piety = Math.max(0, c.piety + (effect.piety ?? 0)); }
      if ((effect.controlGain ?? 0) > 0 && c) {
        for (const tid of [...c.heldTitles]) { const t = this.world.title(tid); if (!t) continue; for (const cid of t.counties) { const county = this.world.map.get(cid); if (county) county.control = Math.min(100, county.control + (effect.controlGain ?? 0) * 0.2); } }
      }
    }
  }

  tickFactions(): void {
    if (!this.factions.tickDiscontent && !this.factions.recomputePower) return;
    const vassalPairs: number[][] = [];
    for (const [, t] of this.world.titles) {
      if (t.holder === NONE_ID || t.deFactoLiege === NONE_ID) continue;
      const lt = this.world.title(t.deFactoLiege);
      if (!lt || lt.holder === NONE_ID || lt.holder === t.holder) continue;
      let op = this.world.opinion(t.holder, lt.holder);
      const law = this.realmLaws.get(lt.holder);
      if (law?.crownAuthority) op += crownAuthorityVassalOpinionPenalty(law.crownAuthority);
      vassalPairs.push([t.holder, lt.holder, op]);
    }
    const mil = new Map<number, number>();
    for (const r of this.world.rulers()) mil.set(r.id, GameSimulation.estimatePower(this.world, r.id));
    for (const row of vassalPairs) { if (!mil.has(row[0])) mil.set(row[0], GameSimulation.estimatePower(this.world, row[0])); }
    const liegePow = new Map<number, number>();
    for (const r of this.world.rulers()) liegePow.set(r.id, mil.get(r.id) ?? 0);
    if (this.factions.recomputePower) this.factions.recomputePower(mil, liegePow);
    const opinions = new Map<number, number>();
    for (const row of vassalPairs) opinions.set(row[0], row[2]);
    if (this.factions.tickDiscontent) this.factions.tickDiscontent(opinions);
    if (this.factions.monthlyAi) {
      const evs = this.factions.monthlyAi(vassalPairs, () => Math.random());
      for (const ev of evs) {
        switch (ev.kind) {
          case 'formed': { const f = this.world.character(ev.founder); const l = this.world.character(ev.liege); if (f && l) this.world.pushLog(f.name + ' \u9488\u5bf9 ' + l.name + ' \u7ec4\u5efa\u4e86' + (ev.factionKind != null ? factionKindNameZh(ev.factionKind) : '\u6d3e\u7cfb')); break; }
          case 'joined': { const w = this.world.character(ev.who); if (w) this.world.pushLog(w.name + ' \u52a0\u5165\u4e86\u6d3e\u7cfb'); break; }
          case 'revolt': {
            const l = this.world.character(ev.liege); if (l) this.world.pushLog('\u53db\u4e71\u7206\u53d1\uff01' + (ev.factionKind != null ? factionKindNameZh(ev.factionKind) : '\u53db\u4e71') + ' vs ' + l.name);
            const members = ev.members; if (members && members.length > 0) { const leader = members[0]; const cb = (ev.factionKind === 'CLAIMANT' || ev.factionKind === 'POPULAR') ? CasusBelli.DEPOSE_LIEGE : CasusBelli.INDEPENDENCE; if (this.diplomacy.canDeclareWar(leader, ev.liege, this.world.date.year())) { this.wars.declareWar(cb, leader, ev.liege, this.world.date, '\u53db\u4e71'); this.diplomacy.setAtWar(leader, ev.liege, true); } }
            if (this.factions.dissolve) this.factions.dissolve(ev.factionId);
            break;
          }
          case 'dissolved': this.world.pushLog('\u6d3e\u7cfb\u89e3\u6563\uff1a' + ev.reason); break;
          default: break;
        }
      }
    }
  }

  tickSchemes(): void {
    const intrigue = new Map<number, number>();
    for (const c of this.world.aliveCharacters()) { const a = this.world.effectiveAttrs ? this.world.effectiveAttrs(c.id) : null; if (a) intrigue.set(c.id, a.intrigue); }
    const allCouncils = this.councils.allCouncils?.() ?? {};
    for (const [, council] of Object.entries(allCouncils)) { if ((council as any).spymaster && (council as any).spymaster !== NONE_ID) intrigue.set((council as any).ruler, (intrigue.get((council as any).ruler) ?? 8) + 2); }
    if (!this.schemes.monthlyTick) return;
    const outcomes = this.schemes.monthlyTick(intrigue, () => Math.random());
    for (const o of outcomes) {
      if (o.kind === 'success' && o.schemeKind) {
        const owner = this.world.character(o.owner); const target = this.world.character(o.target);
        const on = owner?.name ?? '?'; const tn = target?.name ?? '?';
        if (o.schemeKind === SchemeKind.MURDER) { this.world.pushLog('\u9634\u8c0b\u6210\u529f\uff1a' + on + ' \u6697\u6740\u4e86 ' + tn + '\uff01'); if (this.world.onDeath) this.world.onDeath(o.target, null); if (owner) { owner.stress += 20; owner.prestige -= 15; } }
        else if (o.schemeKind === SchemeKind.SWAY) { this.world.modifyOpinion(o.target, o.owner, 25); this.world.pushLog(on + ' \u6210\u529f\u62c9\u62e2\u4e86 ' + tn); }
        else if (o.schemeKind === SchemeKind.FABRICATE_HOOK) { this.world.modifyOpinion(o.target, o.owner, -10); this.world.pushLog(on + ' \u638c\u63e1\u4e86 ' + tn + ' \u7684\u628a\u67c4'); if (owner) owner.prestige += 10; }
      } else if (o.kind === 'exposed') {
        const owner = this.world.character(o.owner); const target = this.world.character(o.target);
        this.world.pushLog('\u9634\u8c0b\u8d25\u9732\uff01' + (owner?.name ?? '?') + ' \u5bf9 ' + (target?.name ?? '?') + ' \u7684\u5bc6\u8c0b\u88ab\u53d1\u73b0');
        this.world.modifyOpinion(o.target, o.owner, -40);
        if (owner) { owner.prestige -= 25; owner.stress += 15; }
      }
    }
  }

  resolveEncounters(): void {
    const byLoc = new Map<number, Array<{id: number; owner: number; men: number}>>();
    const armies = (this.wars as any).armies;
    if (!armies) return;
    for (const [, a] of armies) { if (a.isActive()) { const arr = byLoc.get(a.location) ?? []; arr.push({ id: a.id, owner: a.owner, men: a.totalMen() }); byLoc.set(a.location, arr); } }
    for (const [, locArmies] of byLoc) {
      if (locArmies.length < 2) continue;
      const byOwner = new Map<number, Array<{id: number; men: number}>>();
      for (const row of locArmies) { const arr = byOwner.get(row.owner) ?? []; arr.push({ id: row.id, men: row.men }); byOwner.set(row.owner, arr); }
      const owners = [...byOwner.keys()];
      for (let i = 0; i < owners.length; i++) {
        for (let j = i + 1; j < owners.length; j++) {
          const oa = owners[i]; const ob = owners[j];
          let enemies = false;
          for (const w of this.wars.activeWars()) { if ((w.isAttacker(oa) && !w.isAttacker(ob) && w.involves(ob)) || (w.isAttacker(ob) && !w.isAttacker(oa) && w.involves(oa))) { enemies = true; break; } }
          if (!enemies) continue;
          const aArr = byOwner.get(oa)!; const bArr = byOwner.get(ob)!;
          const aId = aArr.reduce((a, b) => a.men > b.men ? a : b).id;
          const bId = bArr.reduce((a, b) => a.men > b.men ? a : b).id;
          const armyA = this.wars.army(aId); const armyB = this.wars.army(bId);
          if (armyA && armyB) this.resolveBattlePair(armyA, armyB);
        }
      }
    }
  }

  resolveBattlePair(armyA: Army, armyB: Army): void {
    const atkAttrs = this.world.effectiveAttrs ? this.world.effectiveAttrs(armyA.commander) : null;
    const defAttrs = this.world.effectiveAttrs ? this.world.effectiveAttrs(armyB.commander) : null;
    const atkM = atkAttrs?.martial ?? 8; const defM = defAttrs?.martial ?? 8;
    const county = this.world.map.get(armyA.location);
    const width = (county as any)?.terrain?.combatWidth?.() ?? 1.0;
    const season = this.world.date.season();
    const seasonMod = season === Season.SPRING ? Balance.SEASON_COMBAT_SPRING : season === Season.SUMMER ? Balance.SEASON_COMBAT_SUMMER : season === Season.AUTUMN ? Balance.SEASON_COMBAT_AUTUMN : Balance.SEASON_COMBAT_WINTER;
    const result = BattleSimulator.resolve(armyA, armyB, atkM, defM, width, Math.random, seasonMod);
    const an = this.world.character(armyA.owner); const bn = this.world.character(armyB.owner);
    this.world.pushLog('\u6218\u6597\uff01' + (an?.name ?? '?') + ' vs ' + (bn?.name ?? '?') + ' \u2014 ' + result.description);
    for (const w of [...this.wars.activeWars()]) {
      if (w.involves(armyA.owner) && w.involves(armyB.owner)) { if (w.isAttacker(armyA.owner)) w.applyWarscore(result.warscoreChange); else w.applyWarscore(-result.warscoreChange); }
    }
    this.retreatArmy(result.attackerWon ? armyB : armyA);
  }

  retreatArmy(army: Army): void {
    const county = this.world.map.get(army.location);
    if (!county || county.neighbors.length === 0) { army.status = ArmyStatus.RETREATING; return; }
    const enemyHolders = new Set<number>();
    for (const w of this.wars.activeWars()) { if (!w.involves(army.owner)) continue; for (const p of w.participants) { if (w.isAttacker(p.character) !== w.isAttacker(army.owner)) enemyHolders.add(p.character); } }
    const friendly: number[] = []; const neutral: number[] = []; const hostile: number[] = [];
    for (const nid of county.neighbors) { const n = this.world.map.get(nid); if (!n) continue; if (n.holder === army.owner) friendly.push(nid); else if (enemyHolders.has(n.holder)) hostile.push(nid); else neutral.push(nid); }
    const pool = friendly.length > 0 ? friendly : neutral.length > 0 ? neutral : hostile.length > 0 ? hostile : county.neighbors;
    army.location = pool[0]; army.path = []; army.status = ArmyStatus.RETREATING;
  }

  tryStartSieges(): void {
    const snapshots: Array<{id: number; owner: number; location: number}> = [];
    const armies = (this.wars as any).armies;
    if (!armies) return;
    for (const [, a] of armies) { if (a.isActive() && a.status === ArmyStatus.IDLE) snapshots.push({ id: a.id, owner: a.owner, location: a.location }); }
    for (const row of snapshots) {
      const county = this.world.map.get(row.location);
      if (!county || county.holder === NONE_ID || county.holder === row.owner) continue;
      let enemies = false;
      for (const w of this.wars.activeWars()) { if (w.involves(row.owner) && w.involves(county.holder) && w.isAttacker(row.owner) !== w.isAttacker(county.holder)) { enemies = true; break; } }
      if (!enemies || this.sieges.activeAt?.(row.location)) continue;
      const sid = this.sieges.start(row.location, row.id, row.owner, county.holder, county.fortLevel, Math.max(50, Math.floor(county.levies / 4)), this.world.date);
      const army = this.wars.army(row.id); if (army) army.status = ArmyStatus.SIEGING;
      const o = this.world.character(row.owner);
      this.world.pushLog((o?.name ?? '?') + ' \u5f00\u59cb\u56f4\u653b ' + county.name + ' (\u56f4\u57ce #' + sid + ')');
    }
  }

  tickSieges(): void {
    const men = new Map<number, number>(); const martial = new Map<number, number>(); const locs = new Map<number, number>();
    const armies = (this.wars as any).armies;
    if (armies) for (const [, a] of armies) { men.set(a.id, a.totalMen()); locs.set(a.id, a.location); const attrs = this.world.effectiveAttrs ? this.world.effectiveAttrs(a.commander) : null; martial.set(a.id, attrs?.martial ?? 8); }
    if (!this.sieges.tickDay) return;
    for (const ev of this.sieges.tickDay(men, martial, locs)) {
      if (ev.kind === 'captured') {
        const county = this.world.map.get(ev.county); const attacker = this.world.character(ev.attacker);
        this.world.pushLog((attacker?.name ?? '?') + ' \u653b\u9677\u4e86 ' + (county?.name ?? '?') + '\uff01');
        if (this.world.occupyCounty) this.world.occupyCounty(ev.county, ev.attacker);
        for (const w of [...this.wars.activeWars()]) { if (w.involves(ev.attacker) && w.involves(ev.defender)) { if (w.isAttacker(ev.attacker)) w.applyWarscore(25); else w.applyWarscore(-25); } }
        const s = (this.sieges as any).sieges?.get(ev.siegeId);
        if (s) { const army = this.wars.army(s.attackerArmy); if (army && army.status === ArmyStatus.SIEGING) army.status = ArmyStatus.IDLE; }
        if (attacker) { attacker.prestige += 10; attacker.gold += 5; }
      } else if (ev.kind === 'lifted') {
        const county = this.world.map.get(ev.county);
        this.world.pushLog('\u56f4\u653b ' + (county?.name ?? '?') + ' \u89e3\u9664\uff1a' + ev.reason);
        const s = (this.sieges as any).sieges?.get(ev.siegeId);
        if (s) { const army = this.wars.army(s.attackerArmy); if (army && army.status === ArmyStatus.SIEGING) army.status = ArmyStatus.IDLE; }
      }
    }
  }

  tickWars(): void {
    const warIds: number[] = [];
    for (const w of this.wars.activeWars()) warIds.push(w.id);
    for (const wid of warIds) {
      const w = this.wars.war(wid); if (!w) continue;
      if (this.diplomacy.addWarExhaustion) { this.diplomacy.addWarExhaustion(w.attackerPrimary, Balance.WAR_EXHAUSTION_MONTHLY_ATK); this.diplomacy.addWarExhaustion(w.defenderPrimary, Balance.WAR_EXHAUSTION_MONTHLY_DEF); }
      if (w.warscore > 0) w.applyWarscore(-1); else if (w.warscore < 0) w.applyWarscore(1);
      if (w.canEnforce?.() || w.warscore >= 100) {
        this.wars.endWar(wid, WarResult.ATTACKER_VICTORY);
        this.diplomacy.setAtWar(w.attackerPrimary, w.defenderPrimary, false);
        if (this.diplomacy.setTruce) this.diplomacy.setTruce(w.attackerPrimary, w.defenderPrimary, this.world.date.year() + Balance.VICTORY_TRUCE_YEARS);
        const an = this.world.character(w.attackerPrimary); const dn = this.world.character(w.defenderPrimary);
        this.world.pushLog('\u6218\u4e89\u7ed3\u675f\uff1a' + (an?.name ?? '?') + ' \u6218\u80dc ' + (dn?.name ?? '?') + '\uff0c\u5f3a\u5236\u6267\u884c\u548c\u7ea6');
        this.transferTerritory(w.attackerPrimary, w.defenderPrimary);
        if (an) { an.prestige += cbAttackerPrestigeOnWin(w.cb); an.gold += 30; }
        if (dn) { dn.prestige -= 30; dn.gold -= 20; }
        if (w.cb === CasusBelli.CONQUEST || w.cb === CasusBelli.CLAIM || w.cb === CasusBelli.DE_JURE) this.transferOneCounty(w.defenderPrimary, w.attackerPrimary);
      } else if (w.canSurrender?.() || w.warscore <= -100) {
        this.wars.endWar(wid, WarResult.DEFENDER_VICTORY);
        this.diplomacy.setAtWar(w.attackerPrimary, w.defenderPrimary, false);
        if (this.diplomacy.setTruce) this.diplomacy.setTruce(w.attackerPrimary, w.defenderPrimary, this.world.date.year() + Balance.VICTORY_TRUCE_YEARS);
        const an = this.world.character(w.attackerPrimary); const dn = this.world.character(w.defenderPrimary);
        this.world.pushLog('\u6218\u4e89\u7ed3\u675f\uff1a' + (dn?.name ?? '?') + ' \u51fb\u9000 ' + (an?.name ?? '?'));
        this.transferTerritory(w.defenderPrimary, w.attackerPrimary);
        if (dn) dn.prestige += 40; if (an) an.prestige -= 20;
      } else {
        const atkExh = this.diplomacy.getWarExhaustion().get(w.attackerPrimary) ?? 0;
        const defExh = this.diplomacy.getWarExhaustion().get(w.defenderPrimary) ?? 0;
        if (w.canWhitePeace?.(this.world.date, atkExh, defExh)) {
          this.wars.endWar(wid, WarResult.WHITE_PEACE);
          this.diplomacy.setAtWar(w.attackerPrimary, w.defenderPrimary, false);
          if (this.diplomacy.setTruce) this.diplomacy.setTruce(w.attackerPrimary, w.defenderPrimary, this.world.date.year() + Balance.WHITE_PEACE_TRUCE_YEARS);
          const an = this.world.character(w.attackerPrimary); const dn = this.world.character(w.defenderPrimary);
          this.world.pushLog('\u767d\u548c\uff1a' + (an?.name ?? '?') + ' \u4e0e ' + (dn?.name ?? '?') + ' \u505c\u6218');
          if (an) an.prestige -= 5; if (dn) dn.prestige += 5;
        }
      }
    }
  }

  transferTerritory(winner: number, loser: number): void {
    const lc = this.world.character(loser); if (!lc) return;
    const toTransfer: number[] = [];
    for (const tid of [...lc.heldTitles]) { const t = this.world.title(tid); if (t && t.tier === TitleTier.COUNTY && t.counties.length > 0) toTransfer.push(...t.counties); }
    if (toTransfer.length > 0) {
      for (let i = toTransfer.length - 1; i > 0; i--) { const j = Math.floor(Math.random() * (i + 1)); [toTransfer[i], toTransfer[j]] = [toTransfer[j], toTransfer[i]]; }
      const count = Math.min(toTransfer.length, 1 + Math.floor(Math.random() * 2));
      for (let i = 0; i < count; i++) { const cid = toTransfer[i]; if (this.world.occupyCounty) this.world.occupyCounty(cid, winner); const county = this.world.map.get(cid); if (county) this.world.pushLog('\u9886\u571f\u53d8\u66f4\uff1a' + county.name + ' \u88ab\u8f6c\u79fb'); }
    }
  }

  transferOneCounty(frm: number, to: number): void {
    const loser = this.world.character(frm); if (!loser) return;
    for (const tid of [...loser.heldTitles]) { const t = this.world.title(tid); if (t && t.tier === TitleTier.COUNTY && t.counties.length > 0) { for (const cid of [...t.counties]) { if (this.world.occupyCounty) this.world.occupyCounty(cid, to); } this.world.pushLog('\u548c\u7ea6\u5272\u8ba9\uff1a' + t.name); return; } }
  }

  static estimatePower(world: World, who: number): number {
    const c = world.character(who); if (!c) return 50;
    const seen = new Set<number>(); let total = 0;
    for (const tid of c.heldTitles) { const t = world.title(tid); if (!t) continue; for (const cid of t.counties) { if (!seen.add(cid)) continue; const county = world.map.get(cid); if (county) total += typeof (county as any).monthlyLevies === 'function' ? (county as any).monthlyLevies() : (county.levies ?? 100); } }
    return Math.max(50, total);
  }
}

function isAdult(c: Character, world: World): boolean { return c.birth ? (world.date.year() - c.birth.year()) >= 16 : true; }
