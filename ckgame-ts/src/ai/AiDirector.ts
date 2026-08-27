import type { AiAction } from './AiAction.js';
import { computePersonality } from './PersonalityProfile.js';
import { NONE_ID } from '../core/Constants.js';
import type { World } from '../world/World.js';
import type { Character } from '../world/Character.js';
import type { WarManager } from '../military/WarManager.js';
import { Diplomacy } from '../politics/Diplomacy.js';
import type { SchemeManager } from '../politics/SchemeManager.js';
import { CasusBelli, cbNameZh, cbPrestigeCost } from '../politics/CasusBelli.js';
import { SchemeKind, schemeKindNameZh } from '../politics/SchemeKind.js';
import { WarParticipant } from '../military/WarParticipant.js';

export class AiDirector {
  static monthlyActions(world: World, wars: WarManager, diplomacy: Diplomacy, schemes: SchemeManager, playerIds: Set<number>): AiAction[] {
    const skip = playerIds ?? new Set<number>();
    const actions: AiAction[] = [];
    const rulers = world.rulers();
    for (const ruler of rulers) {
      const rid = ruler.id;
      if (skip.has(rid)) continue;
      const profile = computePersonality(ruler, world);
      let atWar = false;
      for (const w of wars.activeWars()) { if (w.involves(rid)) { atWar = true; break; } }
      const c = world.character(rid);
      if (!c) continue;
      if (c.spouses.length === 0 && isAdult(c, world) && Math.random() < 0.35) {
        const sp = findSpouseCandidate(world, rid);
        if (sp != null) actions.push({ type: 'marry', actor: rid, target: sp });
      }
      for (const childId of c.children) {
        const ch = world.character(childId);
        if (ch && isAlive(ch) && isAdult(ch, world) && ch.spouses.length === 0 && Math.random() < 0.2) {
          const sp = findSpouseCandidate(world, childId);
          if (sp != null) actions.push({ type: 'marry', actor: childId, target: sp });
        }
      }
      if (atWar) {
        let enemy: number | null = null;
        for (const w of wars.activeWars()) { if (w.involves(rid)) { enemy = w.isAttacker(rid) ? w.defenderPrimary : w.attackerPrimary; break; } }
        if (enemy != null) {
          if (wars.armiesOf(rid).length === 0) {
            const loc = capitalOf(world, rid);
            if (loc != null) actions.push({ type: 'raise_army', actor: rid, params: { location: loc, levies: estLocalLev(world, rid) } });
          } else {
            const tl = capitalOf(world, enemy);
            if (tl != null) for (const aid of wars.armiesOf(rid)) {
              const army = wars.army(aid);
              if (army && army.status === 'IDLE' && army.location !== tl) actions.push({ type: 'move_army', actor: rid, params: { armyId: aid, destination: tl } });
            }
          }
        }
      }
      if (!atWar && profile.aggression > 0.55 && Math.random() < profile.aggression * 0.12) {
        const tgt = findWarTarget(world, wars, diplomacy, rid, skip);
        if (tgt != null && !skip.has(tgt)) {
          const fl = diplomacy.flags(rid, tgt);
          const cb = fl.rival ? CasusBelli.RIVALRY : (diplomacy.claimsOf(rid).length > 0 ? CasusBelli.CLAIM : CasusBelli.CONQUEST);
          actions.push({ type: 'declare_war', actor: rid, target: tgt, params: { cb } });
          const loc = capitalOf(world, rid);
          if (loc != null) actions.push({ type: 'raise_army', actor: rid, params: { location: loc, levies: estLocalLev(world, rid) } });
        }
      }
      if (profile.diplomacy > 0.55 && Math.random() < 0.08) { const ally = findAllyCandidate(world, diplomacy, rid); if (ally != null) actions.push({ type: 'form_alliance', actor: rid, target: ally }); }
      if (profile.diplomacy > 0.55 && Math.random() < 0.1 && c.gold >= 30) { const others = rulers.filter(r => r.id !== rid); if (others.length > 0) { const t = others[Math.floor(Math.random() * others.length)]; actions.push({ type: 'send_gift', actor: rid, target: t.id, params: { amount: 15 } }); } }
      if (profile.cunning > 0.45 && Math.random() < 0.12) { let already = false; for (const s of schemes.schemeValues()) { if (s.owner === rid) { already = true; break; } } if (!already) { const tgt = findSchemeTarget(world, rid); if (tgt != null) { const kind = profile.aggression > 0.6 ? SchemeKind.MURDER : (Math.random() < 0.5 ? SchemeKind.SWAY : SchemeKind.FABRICATE_HOOK); actions.push({ type: 'start_scheme', actor: rid, target: tgt, params: { schemeKind: kind } }); } } }
      if (Math.random() < 0.1 && c.gold >= 20) actions.push({ type: 'hold_feast', actor: rid });
      if (profile.diplomacy > 0.45 && Math.random() < 0.15) { const others = rulers.filter(r => r.id !== rid); if (others.length > 0) { const t = others[Math.floor(Math.random() * others.length)]; actions.push({ type: 'improve_relations', actor: rid, target: t.id }); } }
      if (profile.greed > 0.5 && Math.random() < 0.1) actions.push({ type: 'develop', actor: rid });
      if (atWar && Math.random() < 0.15 && c.gold >= 25) { for (const aid of wars.armiesOf(rid)) actions.push({ type: 'recruit_knights', actor: rid, params: { armyId: aid } }); }
    }
    return actions;
  }

  static applyActions(world: World, wars: WarManager, diplomacy: Diplomacy, schemes: SchemeManager, actions: AiAction[]): void {
    for (const act of actions) {
      switch (act.type) {
        case 'raise_army': { if (wars.armiesOf(act.actor).length > 0) continue; const loc = act.params?.location ?? 0; const levies = act.params?.levies ?? 100; const aid = wars.raiseArmy(act.actor, loc, levies); const army = wars.army(aid); if (army) { army.addMen('HEAVY_INFANTRY' as any, Math.floor(levies/10)); army.addMen('ARCHERS' as any, Math.floor(levies/12)); army.addMen('LIGHT_CAVALRY' as any, Math.floor(levies/20)); } const ch = world.character(act.actor); if (ch) world.pushLog(ch.name + ' \u5f81\u53ec\u4e86 ' + levies + ' \u4eba (\u519b\u56e2 ' + aid + ')'); break; }
        case 'move_army': { const army = wars.army(act.params?.armyId ?? 0); if (!army) continue; const dest = act.params?.destination ?? 0; const p = world.map.path(army.location, dest); if (p && p.length > 0) { army.setPath(p); const dc = world.map.get(dest); world.pushLog(army.name + ' \u5411 ' + (dc ? dc.name : '?') + ' \u8fdb\u519b'); } break; }
        case 'declare_war': { const cb = act.params?.cb as CasusBelli | undefined; if (!cb || act.target == null) continue; if (!diplomacy.canDeclareWar(act.actor, act.target, world.date.year())) continue; if (diplomacy.areAllied(act.actor, act.target)) continue; let any = false; for (const w of wars.activeWars()) { if (w.involves(act.actor) || w.involves(act.target)) { any = true; break; } } if (any) continue; const atk = world.character(act.actor); if (!atk || atk.prestige < cbPrestigeCost(cb)) continue; atk.prestige -= cbPrestigeCost(cb); const def = world.character(act.target); const wid = wars.declareWar(cb, act.actor, act.target, world.date, atk.name + ' \u5bf9 ' + (def ? def.name : '?') + ' \u7684' + cbNameZh(cb)); diplomacy.setAtWar(act.actor, act.target, true); diplomacy.addWarExhaustion(act.actor); diplomacy.addWarExhaustion(act.target); const war = wars.war(wid); if (war) for (const ally of diplomacy.alliesOf(act.actor)) { if (ally !== act.target && !war.involves(ally)) { war.participants.push(new WarParticipant(ally, true, world.date)); diplomacy.setAtWar(ally, act.target, true); } } world.pushLog('\u5ba3\u6218\uff01' + cbNameZh(cb) + ' (\u6218\u4e89 ' + wid + ')'); break; }
        case 'marry': { if (act.target != null && world.marry(act.actor, act.target)) { const fl = diplomacy.flagsMut(act.actor, act.target); if (fl) fl.marriagePact = true; } break; }
        case 'improve_relations': { if (act.target != null) { world.modifyOpinion(act.actor, act.target, 10); world.modifyOpinion(act.target, act.actor, 5); } break; }
        case 'form_alliance': { if (act.target == null) continue; if (diplomacy.flags(act.actor, act.target).allied) continue; diplomacy.formAlliance(act.actor, act.target, world.date); world.modifyOpinion(act.actor, act.target, 20); world.modifyOpinion(act.target, act.actor, 20); const a = world.character(act.actor); const b = world.character(act.target); if (a && b) world.pushLog(a.name + ' \u4e0e ' + b.name + ' \u7ed3\u6210\u540c\u76df'); break; }
        case 'send_gift': { const ch = world.character(act.actor); const amt = act.params?.amount ?? 15; if (ch && ch.gold >= amt && act.target != null) { ch.gold -= amt; world.modifyOpinion(act.target, act.actor, Diplomacy.giftOpinionGain(amt)); const t = world.character(act.target); if (t) world.pushLog(ch.name + ' \u5411 ' + t.name + ' \u8d60\u793c ' + amt + ' \u91d1'); } break; }
        case 'start_scheme': { const kind = act.params?.schemeKind as SchemeKind | undefined; if (!kind || act.target == null) continue; const sid = schemes.start(kind, act.actor, act.target, world.date); const o = world.character(act.actor); const t = world.character(act.target); if (o && t) world.pushLog(o.name + ' \u5bf9 ' + t.name + ' \u542f\u52a8\u9634\u8c0b\u300c' + schemeKindNameZh(kind) + '\u300d(#' + sid + ')'); break; }
        case 'hold_feast': { const ch = world.character(act.actor); if (ch && ch.gold >= 20) { ch.gold -= 20; ch.prestige += 15; ch.stress = Math.max(0, ch.stress - 10); world.pushLog(ch.name + ' \u4e3e\u529e\u4e86\u5bb4\u4f1a'); } break; }
        case 'develop': { const ch = world.character(act.actor); if (ch && ch.gold >= 10) { ch.gold -= 10; ch.prestige += 5; } const cap = capitalOf(world, act.actor); if (cap != null) { const co = world.map.get(cap); if (co && co.development < 30) { co.development += 1; world.pushLog('\u9886\u5730 ' + co.name + ' \u53d1\u5c55\u5ea6\u63d0\u5347'); } } break; }
        case 'recruit_knights': { const ch = world.character(act.actor); const army = wars.army(act.params?.armyId ?? 0); if (ch && army && ch.gold >= 25) { ch.gold -= 25; army.addMen('HEAVY_CAVALRY' as any, 40); army.addMen('HEAVY_INFANTRY' as any, 80); world.pushLog(ch.name + ' \u4e3a\u519b\u56e2\u8865\u5145\u4e86\u7cbe\u9510'); } break; }
        default: break;
      }
    }
  }
}

function isAdult(c: Character, w: World): boolean { return c.birth ? (w.date.year() - c.birth.year()) >= 16 : true; }
function isAlive(c: Character): boolean { return c.life === 'ALIVE'; }
function findSpouseCandidate(world: World, who: number): number | null { const c = world.character(who); if (!c) return null; const need = c.gender === 'MALE' ? 'FEMALE' : 'MALE'; const cands: Array<{s: number; id: number}> = []; for (const o of world.aliveCharacters()) { if (o.id !== who && o.gender === need && o.spouses.length === 0 && isAdult(o, world) && o.dynasty !== c.dynasty) { const age = o.birth ? world.date.year() - o.birth.year() : 25; if (age < 40) { const a = world.effectiveAttrs ? world.effectiveAttrs(o.id) : null; cands.push({ s: a ? a.total() : 0, id: o.id }); } } } if (cands.length === 0) return null; cands.sort((a, b) => b.s - a.s); return cands[0].id; }
function findWarTarget(world: World, wars: WarManager, dip: Diplomacy, who: number, skip: Set<number>): number | null { const mp = estLev(world, who) + wars.totalMenOf(who); let best: number | null = null; let bp = 1e9; for (const r of world.rulers()) { if (r.id === who || skip.has(r.id)) continue; if (!dip.canDeclareWar(who, r.id, world.date.year())) continue; if (dip.areAllied(who, r.id)) continue; let aw = false; for (const w of wars.activeWars()) { if (w.involves(r.id)) { aw = true; break; } } if (aw) continue; const th = estLev(world, r.id); if (mp > th * 0.85 && th < bp) { bp = th; best = r.id; } } return best; }
function findAllyCandidate(world: World, dip: Diplomacy, who: number): number | null { let best: number | null = null; let bo = -999; for (const r of world.rulers()) { if (r.id === who) continue; const f = dip.flags(who, r.id); if (f.allied || f.atWar) continue; const op = world.opinion(who, r.id); if (op >= 0 && op > bo) { bo = op; best = r.id; } } return best; }
function findSchemeTarget(world: World, who: number): number | null { let best: number | null = null; let bo = 999; for (const c of world.aliveCharacters()) { if (c.id === who || !isAdult(c, world)) continue; if (!(c.isRuler || c.gold > 80)) continue; const op = world.opinion(who, c.id); if (op < bo) { bo = op; best = c.id; } } return best; }
function capitalOf(world: World, who: number): number | null { const c = world.character(who); if (!c) return null; const t = world.title(c.primaryTitle); if (!t) return null; if (t.capital && t.capital !== NONE_ID) return t.capital; return t.counties.length === 0 ? null : t.counties[0]; }
function estLocalLev(world: World, who: number): number { const loc = capitalOf(world, who); if (loc == null) return estLev(world, who); const co = world.map.get(loc); if (!co) return estLev(world, who); const ml = typeof (co as any).monthlyLevies === 'function' ? (co as any).monthlyLevies() : (co.levies ?? 100); return Math.max(100, ml * 3); }
function estLev(world: World, who: number): number { const c = world.character(who); if (!c) return 0; const seen = new Set<number>(); let total = 0; for (const tid of c.heldTitles) { const t = world.title(tid); if (!t) continue; for (const cid of t.counties) { if (!seen.add(cid)) continue; const co = world.map.get(cid); if (co) total += typeof (co as any).monthlyLevies === 'function' ? (co as any).monthlyLevies() : (co.levies ?? 100); } } return Math.max(100, total); }
