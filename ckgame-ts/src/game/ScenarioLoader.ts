import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import { GameDate } from '../core/calendar/GameDate.js';
import { Gender } from '../core/Gender.js';
import { TitleTier } from '../core/TitleTier.js';
import { NONE_ID } from '../core/Constants.js';
import { AttributeSet } from '../core/stats/AttributeSet.js';
import { World } from '../world/World.js';

export class ScenarioLoader {
  static loadScenario(filePath?: string): World {
    let data: any;
    if (filePath) {
      data = JSON.parse(readFileSync(filePath, 'utf-8'));
    } else {
      const here = typeof __dirname !== 'undefined' ? __dirname : dirname(fileURLToPath(import.meta.url));
      const projRoot = join(here, '..', '..');
      const defaultPath = join(projRoot, 'data', 'scenarios', '1066.json');
      data = JSON.parse(readFileSync(defaultPath, 'utf-8'));
    }
    const sd = data.start_date;
    const start = (Array.isArray(sd) && sd.length === 3) ? new GameDate(sd[0], sd[1], sd[2]) : new GameDate(1066, 1, 1);
    const world = new World(start);
    if (data.trade_routes) {
      const routes: any[] = [];
      for (const row of data.trade_routes) routes.push({ from: row.from, to: row.to, exchangeRate: row.exchange_rate ?? 1.0, tradeVolume: row.trade_volume ?? 0 });
      if (typeof (world as any).loadTradeRoutes === 'function') (world as any).loadTradeRoutes(routes);
      else if (Array.isArray((world as any).tradeRoutes)) for (const r of routes) (world as any).tradeRoutes.push(r);
    }
    const dyn: Record<string, number> = {};
    for (const row of (data.dynasties ?? [])) {
      const did = world.createDynasty(row.name); dyn[row.key] = did;
      const d = world.dynasties.get(did);
      if (d && row.color) { d.colorR = row.color[0]; d.colorG = row.color[1]; d.colorB = row.color[2]; }
      if (d && row.motto) d.motto = row.motto;
    }
    const counties: Record<string, number> = {};
    for (const row of (data.counties ?? [])) {
      const cid = world.createCounty(row.name, row.terrain); counties[row.key] = cid;
      const c = world.map.get(cid); if (!c) continue;
      (c as any).key = row.key;
      if (row.development != null) c.development = row.development;
      if (row.levies != null) c.levies = row.levies;
      if (row.tax != null) c.tax = row.tax;
      if (row.fort != null) c.fortLevel = row.fort;
      c.buildings = ['庄园', '市场']; if (c.fortLevel >= 2) c.buildings.push('城堡');
      if (row.trade_route_protected != null) (c as any).tradeRouteProtected = row.trade_route_protected;
      if (row.trade_route_protection_level != null) (c as any).tradeRouteProtectionLevel = row.trade_route_protection_level;
      if (row.has_port != null) (c as any).hasPort = row.has_port;
      if (row.port_level != null) (c as any).portLevel = row.port_level;
      if (row.trade_route_maintenance_level != null) (c as any).tradeRouteMaintenanceLevel = row.trade_route_maintenance_level;
    }
    for (const pair of (data.connections ?? [])) world.map.connect(counties[pair[0]], counties[pair[1]]);
    const titles: Record<string, number> = {};
    for (const row of (data.titles ?? [])) { const tid = world.createTitle(row.name, TitleTier[row.tier as keyof typeof TitleTier]); titles[row.key] = tid; }
    const tc = data.title_counties ?? {};
    for (const [tkey, ckeys] of Object.entries(tc)) {
      const tid = titles[tkey]; if (tid == null) continue; const t = world.title(tid); if (!t) continue;
      for (const ck of (ckeys as string[])) { const cid = counties[ck]; if (cid == null) continue;
        if (t.tier === TitleTier.COUNTY) { if (typeof (world as any).attachCountyToTitle === 'function') (world as any).attachCountyToTitle(cid, tid); }
        else if (!t.counties.includes(cid)) t.counties.push(cid); }
    }
    for (const pair of (data.vassals ?? [])) { if (typeof (world as any).setVassal === 'function') (world as any).setVassal(titles[pair[0]], titles[pair[1]]); }
    const chars: Record<string, number> = {};
    for (const row of (data.characters ?? [])) {
      const cid = world.createCharacter(row.name, dyn[row.dynasty] ?? 0, Gender[row.gender as keyof typeof Gender] as any, new GameDate(row.birth[0], row.birth[1], row.birth[2]));
      chars[row.key] = cid; const ch = world.character(cid); if (!ch) continue;
      if (row.culture != null) ch.culture = row.culture; if (row.faith != null) ch.faith = row.faith;
      if (row.attrs) { const a = row.attrs; ch.baseAttrs = new AttributeSet(a[0], a[1], a[2], a[3], a[4], a[5]); }
      if (row.gold != null) ch.gold = row.gold; if (row.prestige != null) ch.prestige = row.prestige;
      for (const t of (row.traits ?? [])) { if (!ch.traits.includes(t)) ch.traits.push(t); }
    }
    for (const triple of (data.parents ?? [])) world.setParents(chars[triple[0]], chars[triple[1]], chars[triple[2]]);
    for (const pair of (data.marriages ?? [])) world.marry(chars[pair[0]], chars[pair[1]]);
    for (const pair of (data.grants ?? [])) world.grantTitle(titles[pair[0]], chars[pair[1]]);
    for (const pair of (data.post_grants_vassals ?? [])) { if (typeof (world as any).setVassal === 'function') (world as any).setVassal(titles[pair[0]], titles[pair[1]]); }
    for (const [tkey, ckeys] of Object.entries(tc)) { const tid = titles[tkey]; if (tid == null) continue; const t = world.title(tid); if (!t) continue;
      for (const ck of (ckeys as string[])) { const cid = counties[ck]; if (cid == null) continue; if (!t.counties.includes(cid)) t.counties.push(cid); } }
    for (const triple of (data.opinions ?? [])) world.modifyOpinion(chars[triple[0]], chars[triple[1]], triple[2]);
    for (const line of (data.logs ?? [])) world.pushLog(line);
    return world;
  }
}
