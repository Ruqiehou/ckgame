import { readFileSync, writeFileSync, existsSync, mkdirSync, readdirSync, statSync, unlinkSync } from 'node:fs';
import { join } from 'node:path';
import { GameDate } from '../core/calendar/GameDate.js';
import { Gender } from '../core/Gender.js';
import { TitleTier } from '../core/TitleTier.js';
import { NONE_ID } from '../core/Constants.js';
import { GameSimulation } from '../game/GameSimulation.js';
import { RealmLaw } from '../politics/Laws.js';
import type { World } from '../world/World.js';

const VERSION = 1;
const SAVES_DIR = 'data/saves';

export interface SaveInfo { file: string; name: string; date: string; playerId: number; mtime: number; size: number; }
export interface LoadedGame { sim: GameSimulation; playerId: number; }

export class SaveManager {
  static save(sim: GameSimulation, playerId: number, slotName?: string): string {
    const dir = SAVES_DIR;
    mkdirSync(dir, { recursive: true });
    const file = sanitize(slotName ?? 'autosave') + '.json';
    const data = serialize(sim, playerId);
    const path = join(dir, file);
    writeFileSync(path, JSON.stringify(data, null, 2), 'utf-8');
    return path;
  }

  static load(slotName: string): LoadedGame | null {
    const dir = SAVES_DIR;
    const file = sanitize(slotName) + '.json';
    const path = join(dir, file);
    if (!existsSync(path)) return null;
    const raw = readFileSync(path, 'utf-8');
    const data = JSON.parse(raw);
    return deserialize(data);
  }

  static listSaves(): SaveInfo[] {
    const dir = SAVES_DIR;
    if (!existsSync(dir)) return [];
    const files = readdirSync(dir).filter(f => f.endsWith('.json'));
    const result: SaveInfo[] = [];
    for (const f of files) {
      try {
        const fp = join(dir, f);
        const st = statSync(fp);
        const raw = readFileSync(fp, 'utf-8');
        const data = JSON.parse(raw);
        const dateArr = data.date;
        let dateStr = '';
        if (Array.isArray(dateArr) && dateArr.length >= 3) dateStr = dateArr[0] + '-' + String(dateArr[1]).padStart(2, '0') + '-' + String(dateArr[2]).padStart(2, '0');
        result.push({ file: f, name: f.replace('.json', ''), date: dateStr, playerId: data.playerId ?? 0, mtime: st.mtimeMs, size: st.size });
      } catch { /* skip invalid */ }
    }
    result.sort((a, b) => b.mtime - a.mtime);
    return result;
  }

  static deleteSave(slotName: string): boolean {
    const file = sanitize(slotName) + '.json';
    if (file === 'autosave.json') return false;
    const path = join(SAVES_DIR, file);
    try { if (existsSync(path)) { unlinkSync(path); return true; } return false; } catch { return false; }
  }
}

function sanitize(name: string): string { return (name ?? 'autosave').replace(/[^a-zA-Z0-9_\-\u4e00-\u9fff]/g, '_').substring(0, 64) || 'autosave'; }

function dateToList(d: GameDate | undefined): number[] { if (!d) return [0, 0, 0]; return [d.year(), d.month(), d.day()]; }

function serialize(sim: GameSimulation, playerId: number): any {
  const w = sim.world;
  const data: any = {
    version: VERSION,
    date: dateToList(w.date),
    tick: (w as any).tick ?? 0,
    playerId,
    playerIds: [...sim.playerIds],
    world: serializeWorld(w),
    diplomacy: sim.diplomacy.saveState?.() ?? {},
    factions: sim.factions.saveState?.() ?? {},
    schemes: sim.schemes.saveState?.() ?? {},
    councils: sim.councils.saveState?.() ?? {},
    buildings: sim.buildings.saveState?.() ?? {},
    realmLaws: serializeRealmLaws(sim.realmLaws),
  };
  return data;
}

function serializeWorld(w: World): any {
  const m: any = {
    nextChar: w.nextChar, nextDynasty: w.nextDynasty, nextTitle: w.nextTitle, nextCounty: w.nextCounty,
    log: [...w.log],
    characters: [], dynasties: [], titles: [], counties: [],
  };
  for (const [, c] of w.characters) {
    const cm: any = { id: c.id, name: c.name, dynasty: c.dynasty, gender: c.gender, birth: dateToList(c.birth), death: c.death ? dateToList(c.death) : null, life: c.life, baseAttrs: [c.baseAttrs.diplomacy, c.baseAttrs.martial, c.baseAttrs.stewardship, c.baseAttrs.intrigue, c.baseAttrs.learning, c.baseAttrs.prowess], traits: [...c.traits], gold: c.gold, prestige: c.prestige, piety: c.piety, stress: c.stress, health: c.health, fertility: c.fertility, father: c.father, mother: c.mother, spouses: [...c.spouses], children: [...c.children], heldTitles: [...c.heldTitles], primaryTitle: c.primaryTitle, isRuler: c.isRuler, employer: c.employer, level: c.level, xp: c.xp };
    const op: any = {}; for (const [k, v] of c.opinionCache) { if (v !== 0) op[String(k)] = v; } cm.opinionCache = op;
    m.characters.push(cm);
  }
  for (const [, d] of w.dynasties) m.dynasties.push({ id: d.id, name: d.name, head: d.head, founder: d.founder, members: [...d.members], colorR: d.colorR, colorG: d.colorG, colorB: d.colorB, motto: d.motto, prestige: d.prestige });
  for (const [, t] of w.titles) { const tm: any = { id: t.id, name: t.name, tier: t.tier, holder: t.holder, deJureLiege: t.deJureLiege, deFactoLiege: t.deFactoLiege, deJureVassals: [...t.deJureVassals], deFactoVassals: [...t.deFactoVassals], capital: t.capital, counties: [...t.counties], creationCost: t.creationCost, destroyable: t.destroyable }; if (t.realmLaw) tm.realmLaw = [t.realmLaw.succession, t.realmLaw.crownAuthority, t.realmLaw.genderLaw, t.realmLaw.partitionEnabled]; m.titles.push(tm); }
  for (const c of w.map.list()) m.counties.push({ id: c.id, name: c.name, terrain: c.terrain, key: (c as any).key ?? '', development: c.development, control: c.control, prosperity: c.prosperity, ownerTitle: c.ownerTitle, holder: c.holder, fortLevel: c.fortLevel, buildings: [...c.buildings], levies: c.levies, tax: c.tax, neighbors: [...c.neighbors], hasPort: (c as any).hasPort ?? false, portLevel: (c as any).portLevel ?? 0, portIncome: (c as any).portIncome ?? 0, tradeRouteProtected: (c as any).tradeRouteProtected ?? false, tradeRouteProtectionLevel: (c as any).tradeRouteProtectionLevel ?? 0, tradeRouteMaintenanceLevel: (c as any).tradeRouteMaintenanceLevel ?? 0, tradeRouteUpgradeCost: (c as any).tradeRouteUpgradeCost ?? 0 });
  return m;
}

function serializeRealmLaws(laws: Map<number, any>): any {
  const m: any = {};
  for (const [k, v] of laws) { if (v) m[String(k)] = [v.succession, v.crownAuthority, v.genderLaw, v.partitionEnabled]; }
  return m;
}

function deserialize(data: any): LoadedGame | null {
  try {
    const sim = new GameSimulation();
    const w = sim.world;
    const wm = data.world;
    const dateArr = data.date;
    w.date = new GameDate(dateArr[0], dateArr[1], dateArr[2]);
    (w as any).tick = data.tick ?? 0;
    w.nextChar = wm.nextChar; w.nextDynasty = wm.nextDynasty; w.nextTitle = wm.nextTitle; w.nextCounty = wm.nextCounty;
    w.log = [...(wm.log ?? [])];
    w.characters.clear(); w.dynasties.clear(); w.titles.clear();
    for (const cm of wm.characters) {
      const c = { ...cm, birth: new GameDate(cm.birth[0], cm.birth[1], cm.birth[2]), death: cm.death ? new GameDate(cm.death[0], cm.death[1], cm.death[2]) : undefined, baseAttrs: { diplomacy: cm.baseAttrs[0], martial: cm.baseAttrs[1], stewardship: cm.baseAttrs[2], intrigue: cm.baseAttrs[3], learning: cm.baseAttrs[4], prowess: cm.baseAttrs[5], total: () => cm.baseAttrs[0]+cm.baseAttrs[1]+cm.baseAttrs[2]+cm.baseAttrs[3]+cm.baseAttrs[4]+cm.baseAttrs[5] }, opinionCache: new Map<number, number>(), traits: [...cm.traits], spouses: [...cm.spouses], children: [...cm.children], heldTitles: [...cm.heldTitles] };
      for (const [k, v] of Object.entries(cm.opinionCache ?? {})) c.opinionCache.set(Number(k), v as number);
      w.characters.set(c.id, c as any);
    }
    for (const dm of wm.dynasties) { w.dynasties.set(dm.id, { ...dm, members: [...dm.members] }); }
    for (const cm of wm.counties) { const c = { ...cm, neighbors: [...cm.neighbors], buildings: [...cm.buildings] }; (w.map as any).counties?.set?.(c.id, c) ?? w.map.insert(c as any); }
    for (const cm of wm.counties) { for (const n of cm.neighbors) w.map.connect(cm.id, n); }
    for (const tm of wm.titles) { const t = { ...tm, deJureVassals: [...tm.deJureVassals], deFactoVassals: [...tm.deFactoVassals], counties: [...tm.counties] }; if (tm.realmLaw) { t.realmLaw = new RealmLaw(tm.realmLaw[0], tm.realmLaw[1], tm.realmLaw[2], tm.realmLaw[3]); } w.titles.set(t.id, t); }
    if (data.diplomacy?.loadState) sim.diplomacy.loadState(data.diplomacy);
    if (data.factions?.loadState) sim.factions.loadState(data.factions);
    if (data.schemes?.loadState) sim.schemes.loadState(data.schemes);
    if (data.councils?.loadState) sim.councils.loadState(data.councils);
    if (data.buildings?.loadState) sim.buildings.loadState(data.buildings);
    sim.realmLaws.clear();
    for (const [k, v] of Object.entries(data.realmLaws ?? {})) { const arr = v as any[]; if (arr) { sim.realmLaws.set(Number(k), new RealmLaw(arr[0], arr[1], arr[2], arr[3])); } }
    sim.playerIds.clear(); for (const id of (data.playerIds ?? [])) sim.playerIds.add(id);
    return { sim, playerId: data.playerId };
  } catch (e) { return null; }
}
