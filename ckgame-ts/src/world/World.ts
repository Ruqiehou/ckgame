import { NONE_ID } from '../core/Constants.js';
import { Gender } from '../core/Gender.js';
import { TitleTier } from '../core/TitleTier.js';
import { GameDate } from '../core/calendar/GameDate.js';
import { AttributeSet } from '../core/stats/AttributeSet.js';
import { Trait } from '../core/traits/Trait.js';
import { builtinTraits } from '../core/traits/traits.js';
import { Character } from './Character.js';
import { County } from './County.js';
import { Dynasty } from './Dynasty.js';
import { CrownAuthority, crownAuthorityTaxBonus, HeirCandidate, RealmLaw } from './Laws.js';
import { MapGraph } from './MapGraph.js';
import { Title } from './Title.js';
import { Terrain, terrainDevelopmentCap } from './Terrain.js';
import { TradeRoute, tradeRouteKey } from './TradeRoute.js';

/** 贸易事件记录。 */
export interface TradeEvent {
  date: string;
  route: TradeRoute;
  type: string;
  description: string;
}

/** 世界状态：所有角色、王朝、头衔、地图与贸易的经济逻辑。 */
export class World {
  date: GameDate;
  tick: number = 0;
  characters: Map<number, Character> = new Map();
  dynasties: Map<number, Dynasty> = new Map();
  titles: Map<number, Title> = new Map();
  map: MapGraph = new MapGraph();
  traits: Trait[] = builtinTraits();
  log: string[] = [];
  nextChar: number = 1;
  nextDynasty: number = 1;
  nextTitle: number = 1;
  nextCounty: number = 1;

  tradeRoutes: TradeRoute[] = [];
  exchangeRates: Map<string, number> = new Map();
  tradeEvents: TradeEvent[] = [];

  private rngState: number;

  constructor(dateOrNull?: GameDate | null, seed?: number) {
    this.date = dateOrNull ?? new GameDate(1066, 10, 14);
    this.rngState = seed ?? Date.now();
    // Ensure non-zero seed
    if (this.rngState === 0) this.rngState = 1;
  }

  // ---------- RNG ----------

  /** Simple mulberry32 PRNG for deterministic results. */
  private rng(): number {
    let t = (this.rngState += 0x6D2B79F5);
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  }

  nextDouble(): number {
    return this.rng();
  }

  nextInt(bound: number): number;
  nextInt(origin: number, bound: number): number;
  nextInt(originOrBound: number, bound?: number): number {
    if (bound === undefined) {
      return Math.floor(this.rng() * originOrBound);
    }
    return originOrBound + Math.floor(this.rng() * (bound - originOrBound));
  }

  // ---------- Logging ----------

  pushLog(msg: string): void {
    this.log.push(`[${this.date}] ${msg}`);
    if (this.log.length > 500) {
      this.log.splice(0, 100);
    }
  }

  // ---------- 创建 ----------

  createDynasty(name: string): number {
    const did = this.nextDynasty++;
    this.dynasties.set(did, Dynasty.newDynasty(did, name, NONE_ID));
    return did;
  }

  createCharacter(name: string, dynasty: number, gender: Gender, birth: GameDate): number {
    const cid = this.nextChar++;
    const c = new Character(cid, name, dynasty, gender, birth);
    c.baseAttrs = new AttributeSet(
      this.nextInt(4, 14),
      this.nextInt(4, 14),
      this.nextInt(4, 14),
      this.nextInt(4, 14),
      this.nextInt(4, 14),
      this.nextInt(4, 14),
    );
    if (this.traits.length > 0) {
      const n = this.nextInt(1, 3);
      for (let i = 0; i < n; i++) {
        const t = this.traits[this.nextInt(this.traits.length)].id;
        if (!c.traits.includes(t)) {
          c.traits.push(t);
        }
      }
    }
    const d = this.dynasties.get(dynasty);
    if (d !== undefined) {
      d.addMember(cid);
      if (d.head === NONE_ID) {
        d.head = cid;
        d.founder = cid;
      }
    }
    this.characters.set(cid, c);
    return cid;
  }

  createTitle(name: string, tier: TitleTier): number {
    const tid = this.nextTitle++;
    this.titles.set(tid, Title.newTitle(tid, name, tier));
    return tid;
  }

  createCounty(name: string, terrain: Terrain): number {
    const cid = this.nextCounty++;
    this.map.insert(County.newCounty(cid, name, terrain));
    return cid;
  }

  // ---------- 查询 ----------

  character(cid: number): Character | undefined {
    return this.characters.get(cid);
  }

  title(tid: number): Title | undefined {
    return this.titles.get(tid);
  }

  aliveCharacters(): Character[] {
    const out: Character[] = [];
    for (const c of this.characters.values()) {
      if (c.isAlive()) out.push(c);
    }
    return out;
  }

  rulers(): Character[] {
    const out: Character[] = [];
    for (const c of this.characters.values()) {
      if (c.isAlive() && c.isRuler) out.push(c);
    }
    return out;
  }

  traitBonus(traitIds: number[]): AttributeSet {
    let total = AttributeSet.zero();
    const byId = new Map<number, Trait>();
    for (const t of this.traits) byId.set(t.id, t);
    for (const tid of traitIds) {
      const t = byId.get(tid);
      if (t !== undefined) total = total.add(t.attrBonus);
    }
    return total;
  }

  effectiveAttrs(cid: number): AttributeSet | null {
    const c = this.character(cid);
    if (c === undefined) return null;
    return c.effectiveAttrs(this.traitBonus(c.traits));
  }

  // ---------- 关系与封建 ----------

  setParents(child: number, father: number, mother: number): void {
    const c = this.character(child);
    if (c === undefined) return;
    c.father = father;
    c.mother = mother;
    if (father !== NONE_ID) {
      const p = this.character(father);
      if (p !== undefined && !p.children.includes(child)) p.children.push(child);
    }
    if (mother !== NONE_ID) {
      const p = this.character(mother);
      if (p !== undefined && !p.children.includes(child)) p.children.push(child);
    }
  }

  marry(a: number, b: number): boolean {
    const ca = this.character(a);
    const cb = this.character(b);
    if (ca === undefined || cb === undefined) return false;
    if (!ca.isAlive() || !cb.isAlive()) return false;
    if (ca.gender === cb.gender) return false;
    if (!ca.spouses.includes(b)) ca.spouses.push(b);
    if (!cb.spouses.includes(a)) cb.spouses.push(a);
    this.pushLog(`${ca.name} 与 ${cb.name} 成婚`);
    return true;
  }

  grantTitle(titleId: number, holder: number): boolean {
    const t = this.title(titleId);
    const h = this.character(holder);
    if (t === undefined || h === undefined) return false;
    const old = t.holder;
    if (old !== NONE_ID && old !== holder) {
      const oldC = this.character(old);
      if (oldC !== undefined) {
        const idx = oldC.heldTitles.indexOf(titleId);
        if (idx >= 0) oldC.heldTitles.splice(idx, 1);
        if (oldC.primaryTitle === titleId) {
          oldC.primaryTitle = oldC.heldTitles.length === 0 ? NONE_ID : oldC.heldTitles[0];
          oldC.isRuler = oldC.primaryTitle !== NONE_ID;
        }
      }
    }
    t.setHolder(holder);
    if (!h.heldTitles.includes(titleId)) h.heldTitles.push(titleId);
    h.isRuler = true;
    let shouldUpgrade = h.primaryTitle === NONE_ID;
    if (!shouldUpgrade) {
      const cur = this.title(h.primaryTitle);
      shouldUpgrade = cur === undefined || t.tier > cur.tier;
    }
    if (shouldUpgrade) h.primaryTitle = titleId;
    for (const cid of t.counties) {
      const county = this.map.get(cid);
      if (county !== undefined) {
        county.holder = holder;
        county.ownerTitle = titleId;
      }
    }
    this.pushLog(`${h.name} 获得头衔「${t.name}」`);
    return true;
  }

  setVassal(vassalTitle: number, liegeTitle: number): boolean {
    const v = this.title(vassalTitle);
    const l = this.title(liegeTitle);
    if (v === undefined || l === undefined) return false;
    if (v.deFactoLiege !== NONE_ID && v.deFactoLiege !== liegeTitle) {
      const oldLiege = this.title(v.deFactoLiege);
      if (oldLiege !== undefined) {
        const idx = oldLiege.deFactoVassals.indexOf(vassalTitle);
        if (idx >= 0) oldLiege.deFactoVassals.splice(idx, 1);
      }
    }
    v.deFactoLiege = liegeTitle;
    if (!l.deFactoVassals.includes(vassalTitle)) l.deFactoVassals.push(vassalTitle);
    return true;
  }

  clearVassalLink(titleId: number): void {
    const t = this.title(titleId);
    if (t === undefined) return;
    if (t.deFactoLiege !== NONE_ID) {
      const oldLiege = this.title(t.deFactoLiege);
      if (oldLiege !== undefined) {
        const idx = oldLiege.deFactoVassals.indexOf(titleId);
        if (idx >= 0) oldLiege.deFactoVassals.splice(idx, 1);
      }
    }
    t.deFactoLiege = NONE_ID;
  }

  attachCountyToTitle(countyId: number, titleId: number): boolean {
    const t = this.title(titleId);
    const c = this.map.get(countyId);
    if (t === undefined || c === undefined) return false;
    if (!t.counties.includes(countyId)) t.counties.push(countyId);
    if (t.capital === NONE_ID) t.capital = countyId;
    c.ownerTitle = titleId;
    c.holder = t.holder;
    return true;
  }

  // ---------- Opinion ----------

  private traitById(): Map<number, Trait> {
    const byId = new Map<number, Trait>();
    for (const t of this.traits) byId.set(t.id, t);
    return byId;
  }

  opinion(from: number, to: number): number {
    if (from === to) return 100;
    const a = this.character(from);
    const b = this.character(to);
    if (a === undefined || b === undefined) return 0;
    let op = 0;
    if (a.dynasty !== NONE_ID && a.dynasty === b.dynasty) op += 15;
    op += (a.culture === b.culture) ? 10 : -5;
    op += (a.faith === b.faith) ? 10 : -20;
    if (a.father === to || a.mother === to || b.father === from || b.mother === from) op += 30;
    if (a.spouses.includes(to)) op += 25;
    if (a.children.includes(to) || b.children.includes(from)) op += 20;
    const byId = this.traitById();
    for (const tid of a.traits) {
      const t = byId.get(tid);
      if (t !== undefined) op += t.opinionSelf;
    }
    for (const tid of b.traits) {
      const t = byId.get(tid);
      if (t !== undefined) op += Math.floor(t.opinionOthers / 2);
    }
    const attrs = this.effectiveAttrs(from);
    if (attrs !== null) op += (attrs.diplomacy - 8) * 2;
    op += a.opinionCache.get(to) ?? 0;
    return Math.max(-100, Math.min(100, op));
  }

  modifyOpinion(from: number, to: number, delta: number): void {
    const c = this.character(from);
    if (c === undefined) return;
    let cur = (c.opinionCache.get(to) ?? 0) + delta;
    cur = Math.max(-100, Math.min(100, cur));
    if (cur === 0) c.opinionCache.delete(to);
    else c.opinionCache.set(to, cur);
    if (c.opinionCache.size > 100) {
      const entries = Array.from(c.opinionCache.entries());
      const start = Math.max(0, entries.length - 50);
      const trimmed = new Map<number, number>();
      for (let i = start; i < entries.length; i++) {
        trimmed.set(entries[i][0], entries[i][1]);
      }
      c.opinionCache.clear();
      for (const [k, v] of trimmed) {
        c.opinionCache.set(k, v);
      }
    }
  }

  // ---------- 继承 ----------

  private heirRows(ids: number[]): HeirCandidate[] {
    const rows: HeirCandidate[] = [];
    for (const cid of ids) {
      const ch = this.character(cid);
      if (ch === undefined) continue;
      rows.push({ id: ch.id, gender: ch.gender, birthOrdinal: ch.birth.toOrdinal(), alive: ch.isAlive() });
    }
    return rows;
  }

  findHeir(ruler: number, law: RealmLaw | null): number | null {
    const c = this.character(ruler);
    if (c === undefined) return null;
    const children = this.heirRows([...c.children]);
    const dynastyIds: number[] = [];
    const d = this.dynasties.get(c.dynasty);
    if (d !== undefined) {
      for (const m of d.members) {
        if (m !== ruler) dynastyIds.push(m);
      }
    }
    const dynastyMembers = this.heirRows(dynastyIds);

    if (law !== null) {
      const heir = law.pickHeir(children, dynastyMembers);
      if (heir !== null) return heir.id;
    }

    // 默认：男长嗣 -> 女长嗣 -> 王朝成员
    const sons = children
      .filter(r => r.alive && r.gender === Gender.MALE)
      .sort((a, b) => a.birthOrdinal - b.birthOrdinal);
    if (sons.length > 0) return sons[0].id;

    const daughters = children
      .filter(r => r.alive && r.gender === Gender.FEMALE)
      .sort((a, b) => a.birthOrdinal - b.birthOrdinal);
    if (daughters.length > 0) return daughters[0].id;

    for (const r of dynastyMembers) {
      if (r.alive) return r.id;
    }
    return null;
  }

  onDeath(who: number, law: RealmLaw | null): void {
    const c = this.character(who);
    if (c === undefined || !c.isAlive()) return;
    const name = c.name;
    const titleIds = [...c.heldTitles];
    const heir = this.findHeir(who, law);
    const age = c.ageAt(this.date);
    c.kill(this.date);
    this.pushLog(`${name} 去世，享年 ${age}`);

    if (heir !== null) {
      const heirC = this.character(heir);
      const heirName = heirC !== undefined ? heirC.name : '?';
      if (law !== null && law.partitionEnabled) {
        const livingKids: number[] = [];
        for (const cid of c.children) {
          const k = this.character(cid);
          if (k !== undefined && k.isAlive()) livingKids.push(cid);
        }
        const heirs: number[] = [heir];
        for (const k of livingKids) {
          if (k !== heir && heirs.length < 4) heirs.push(k);
        }
        for (const part of law.partitionTitles(titleIds, heirs)) {
          for (const tid of part.titles) {
            this.grantTitle(tid, part.heirId);
          }
        }
      } else {
        for (const tid of titleIds) {
          this.grantTitle(tid, heir);
        }
      }
      this.pushLog(`${heirName} 继承了 ${name} 的遗产`);
    } else {
      for (const tid of titleIds) {
        const t = this.title(tid);
        if (t !== undefined) {
          t.clearHolder();
          for (const cid of t.counties) {
            const county = this.map.get(cid);
            if (county !== undefined) county.holder = NONE_ID;
          }
        }
      }
      this.pushLog(`${name} 绝嗣，头衔悬空`);
    }
  }

  occupyCounty(countyId: number, newHolder: number): boolean {
    const county = this.map.get(countyId);
    if (county === undefined) return false;
    county.control = 30;
    county.holder = newHolder;
    const tid = county.ownerTitle;
    if (tid === NONE_ID) return true;
    const t = this.title(tid);
    if (t === undefined || t.tier !== TitleTier.COUNTY) return true;
    this.grantTitle(tid, newHolder);
    const conqueror = this.character(newHolder);
    if (conqueror === undefined) return true;
    const primary = this.title(conqueror.primaryTitle);
    if (primary !== undefined && primary.id !== tid && primary.tier > t.tier) {
      this.setVassal(tid, primary.id);
    } else {
      this.clearVassalLink(tid);
    }
    return true;
  }

  // ---------- 贸易系统 ----------

  loadTradeRoutes(routes: TradeRoute[]): void {
    this.tradeRoutes = routes;
    for (const route of routes) {
      this.exchangeRates.set(tradeRouteKey(route), route.exchangeRate);
    }
  }

  calculateTradeIncome(ruler: number): number {
    const c = this.character(ruler);
    if (c === undefined) return 0;
    let income = 0;
    for (const tid of c.heldTitles) {
      const t = this.title(tid);
      if (t === undefined) continue;
      for (const cid of t.counties) {
        const county = this.map.get(cid);
        if (county === undefined) continue;
        for (const route of this.tradeRoutes) {
          if (route.from === county.key || route.to === county.key) {
            const key = tradeRouteKey(route);
            const exchangeRate = this.exchangeRates.get(key) ?? 1.0;
            let portBonus = 1.0;
            if (county.hasPort) portBonus *= (1.0 + county.portLevel * 0.2);
            income += route.tradeVolume * exchangeRate * 0.1 * portBonus;
            if (county.hasPort) income += county.portIncome * 0.5;
          }
        }
      }
    }
    return income;
  }

  // ---------- 经济 ----------

  monthlyIncomeOf(ruler: number): number {
    const c = this.character(ruler);
    if (c === undefined) return 0;
    let income = 0;
    for (const tid of c.heldTitles) {
      const t = this.title(tid);
      if (t === undefined) continue;
      for (const cid of t.counties) {
        const county = this.map.get(cid);
        if (county !== undefined) income += county.monthlyTax();
      }
    }
    income += this.calculateTradeIncome(ruler);
    const attrs = this.effectiveAttrs(ruler);
    if (attrs !== null) income *= 1.0 + (attrs.stewardship - 8) * 0.03;
    const primaryTitle = this.title(c.primaryTitle);
    if (primaryTitle !== undefined) {
      income *= 1.0 + crownAuthorityTaxBonus(primaryTitle.realmLaw.crownAuthority);
    }
    return income;
  }

  processMonthlyEconomy(): void {
    const net = new Map<number, number>();
    for (const c of this.aliveCharacters()) {
      let income = this.monthlyIncomeOf(c.id);

      // Trade route maintenance
      for (const route of this.tradeRoutes) {
        const primaryTitleStr = String(c.primaryTitle);
        if (route.from === primaryTitleStr || route.to === primaryTitleStr) {
          if (this.rng() < 0.05) this.triggerTradeEvent(route);
        }
      }
      for (const route of this.tradeRoutes) {
        const primaryTitleStr = String(c.primaryTitle);
        if (route.from === primaryTitleStr || route.to === primaryTitleStr) {
          const maintenanceCost = route.tradeVolume * 0.05;
          income -= maintenanceCost;
          this.pushLog(`${c.name} 支付贸易路线维护成本：${maintenanceCost.toFixed(1)} 金`);
        }
      }

      // Port maintenance
      for (const tid of c.heldTitles) {
        const t = this.title(tid);
        if (t === undefined) continue;
        for (const cid of t.counties) {
          const county = this.map.get(cid);
          if (county !== undefined && county.hasPort) {
            const portMaintenanceCost = county.portIncome * 0.2;
            income -= portMaintenanceCost;
            this.pushLog(`${c.name} 支付港口维护成本：${portMaintenanceCost.toFixed(1)} 金`);
          }
        }
      }

      // Building upkeep
      for (const tid of c.heldTitles) {
        const t = this.title(tid);
        if (t === undefined) continue;
        for (const cid of t.counties) {
          const county = this.map.get(cid);
          if (county !== undefined) income -= county.upkeep();
        }
      }

      net.set(c.id, income);
    }

    // 封臣缴税：按 de facto 层级上缴 10%
    for (const t of this.titles.values()) {
      if (t.deFactoLiege !== NONE_ID && t.holder !== NONE_ID) {
        const liegeT = this.title(t.deFactoLiege);
        if (liegeT !== undefined && liegeT.holder !== NONE_ID) {
          const tax = (net.get(t.holder) ?? 0) * 0.1;
          net.set(liegeT.holder, (net.get(liegeT.holder) ?? 0) + tax);
          net.set(t.holder, (net.get(t.holder) ?? 0) - tax);
        }
      }
    }

    for (const [charId, income] of net) {
      const c = this.character(charId);
      if (c === undefined) continue;
      c.addGold(income);
      if (income > 0) c.addPrestige(1.0);
    }

    this.updateExchangeRates();
  }

  private triggerTradeEvent(route: TradeRoute): void {
    const types = ['blockade', 'boom', 'crisis', 'new_route', 'trade_route_protected'];
    let eventType = types[this.nextInt(types.length)];
    let eventDesc: string;

    const fromCounty = this.map.getByKey(route.from);
    const toCounty = this.map.getByKey(route.to);
    let protectionBonus = 1.0;
    if (fromCounty !== undefined && fromCounty.tradeRouteProtected) protectionBonus *= 1.3;
    if (toCounty !== undefined && toCounty.tradeRouteProtected) protectionBonus *= 1.3;
    const protectMult = protectionBonus;

    const key = tradeRouteKey(route);

    switch (eventType) {
      case 'blockade': {
        if (protectionBonus > 1.0 && this.rng() < 0.5) {
          eventType = 'protected_from_blockade';
          eventDesc = `贸易路线 ${route.from} → ${route.to} 遭遇封锁，但商路保护减少了损失`;
          this.adjustExchangeRate(key, 0.9);
        } else {
          eventDesc = `贸易路线 ${route.from} → ${route.to} 被封锁，收入减少`;
          this.adjustExchangeRate(key, 0.8);
        }
        break;
      }
      case 'boom': {
        eventDesc = `贸易路线 ${route.from} → ${route.to} 繁荣，收入增加`;
        this.adjustExchangeRate(key, 1.2 * protectMult);
        break;
      }
      case 'crisis': {
        eventDesc = `贸易路线 ${route.from} → ${route.to} 遭遇危机，收入大幅减少`;
        this.adjustExchangeRate(key, 0.6);
        break;
      }
      case 'new_route': {
        eventDesc = `新的贸易路线 ${route.from} → ${route.to} 建立，收入增加`;
        this.adjustExchangeRate(key, 1.1 * protectMult);
        break;
      }
      default: {
        eventDesc = `商路保护加强：贸易路线 ${route.from} → ${route.to} 得到保护，收入增加`;
        this.adjustExchangeRate(key, 1.15);
        break;
      }
    }

    this.pushLog(eventDesc);
    this.tradeEvents.push({ date: this.date.toString(), route, type: eventType, description: eventDesc });
  }

  private adjustExchangeRate(key: string, multiplier: number): void {
    const cur = this.exchangeRates.get(key);
    if (cur !== undefined) {
      this.exchangeRates.set(key, cur * multiplier);
    }
  }

  updateExchangeRates(): void {
    const routesSnapshot = [...this.tradeRoutes];
    for (const route of routesSnapshot) {
      const key = tradeRouteKey(route);
      if (this.exchangeRates.has(key)) {
        const fluctuation = 0.9 + (this.rng() * 0.2);
        let rate = (this.exchangeRates.get(key) ?? 1.0) * fluctuation;
        rate = Math.max(0.5, Math.min(2.0, rate));
        this.exchangeRates.set(key, rate);

        let maintenanceCost = route.tradeVolume * 0.05;
        const fromCounty = this.map.getByKey(route.from);
        const toCounty = this.map.getByKey(route.to);
        let maintenanceBonus = 1.0;
        if (fromCounty !== undefined) maintenanceBonus *= fromCounty.getTradeRouteMaintenanceBonus();
        if (toCounty !== undefined) maintenanceBonus *= toCounty.getTradeRouteMaintenanceBonus();
        maintenanceCost *= maintenanceBonus;

        if (this.rng() < 0.1) {
          // 维护不足，贸易量下降
          const newVolume = Math.round(route.tradeVolume * 0.9);
          const idx = this.tradeRoutes.indexOf(route);
          if (idx >= 0) {
            this.tradeRoutes[idx] = { from: route.from, to: route.to, exchangeRate: route.exchangeRate, tradeVolume: newVolume };
          }
          this.pushLog(`贸易路线 ${route.from} → ${route.to} 维护不足，贸易量下降`);
        }
      }
    }

    for (const county of this.map.list()) {
      if (county.control > 80 && county.development < terrainDevelopmentCap(county.terrain)) {
        if (this.rng() < 0.05) {
          county.development = Math.min(terrainDevelopmentCap(county.terrain), county.development + 1);
        }
      }
      if (county.control < 100) {
        county.control = Math.min(100, county.control + 1.0);
      }
    }
  }

  // ---------- Health & Fertility ----------

  processHealth(): void {
    const deaths: number[] = [];
    for (const c of this.aliveCharacters()) {
      const age = c.ageAt(this.date);
      if (age > 45) c.health -= 0.002 * (age - 45);
      if (age > 60) c.health -= 0.005;
      for (const tid of c.traits) {
        for (const t of this.traits) {
          if (t.id === tid) {
            c.health += t.healthMod;
            break;
          }
        }
      }
      if (this.rng() < 0.001) {
        c.health -= 0.5;
        c.addStress(10);
      }
      if (c.health <= 0) deaths.push(c.id);
    }
    for (const did of deaths) {
      const c = this.character(did);
      let law: RealmLaw | null = null;
      if (c !== undefined && c.primaryTitle !== NONE_ID) {
        const t = this.title(c.primaryTitle);
        if (t !== undefined) law = t.realmLaw;
      }
      this.onDeath(did, law);
    }
  }

  processFertility(): void {
    const couples: Array<[number, number]> = [];
    for (const c of this.aliveCharacters()) {
      if (c.gender === Gender.MALE && c.isMarried() && c.isAdult(this.date)) {
        for (const s of c.spouses) couples.push([c.id, s]);
      }
    }
    const maleNames = ['艾德温', '哈罗德', '威廉', '罗伯特', '亨利'];
    const femaleNames = ['玛蒂尔达', '爱丽丝', '埃莉诺', '伊莎贝拉', '阿黛拉'];

    for (const [fatherId, motherId] of couples) {
      const f = this.character(fatherId);
      const m = this.character(motherId);
      if (f === undefined || m === undefined) continue;
      if (!m.isAlive() || m.gender !== Gender.FEMALE) continue;
      if (!m.isAdult(this.date) || m.ageAt(this.date) > 45) continue;
      const chance = Math.max(0, Math.min(0.15, f.fertility * m.fertility * 0.02));
      if (this.rng() >= chance) continue;
      const gender = this.rng() < 0.5 ? Gender.MALE : Gender.FEMALE;
      const dname = this.dynasties.get(f.dynasty);
      const dlabel = dname !== undefined ? dname.name : '无名';
      const first = gender === Gender.MALE
        ? maleNames[this.nextInt(maleNames.length)]
        : femaleNames[this.nextInt(femaleNames.length)];
      const childName = `${first}·${dlabel}`;
      const child = this.createCharacter(childName, f.dynasty, gender, this.date);
      this.setParents(child, fatherId, motherId);
      const fa = this.effectiveAttrs(fatherId);
      const ma = this.effectiveAttrs(motherId);
      const ch = this.character(child);
      if (fa !== null && ma !== null && ch !== undefined) {
        ch.baseAttrs = new AttributeSet(
          Math.max(1, Math.floor((fa.diplomacy + ma.diplomacy) / 2) + this.nextInt(-2, 3)),
          Math.max(1, Math.floor((fa.martial + ma.martial) / 2) + this.nextInt(-2, 3)),
          Math.max(1, Math.floor((fa.stewardship + ma.stewardship) / 2) + this.nextInt(-2, 3)),
          Math.max(1, Math.floor((fa.intrigue + ma.intrigue) / 2) + this.nextInt(-2, 3)),
          Math.max(1, Math.floor((fa.learning + ma.learning) / 2) + this.nextInt(-2, 3)),
          Math.max(1, Math.floor((fa.prowess + ma.prowess) / 2) + this.nextInt(-2, 3)),
        );
        ch.culture = f.culture;
        ch.faith = f.faith;
      }
      this.pushLog(`新生儿：${childName}`);
    }
  }
}
