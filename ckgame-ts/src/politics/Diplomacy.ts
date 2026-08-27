import { NONE_ID } from '../core/Constants.js';
import { Balance } from '../core/balance/Balance.js';
import { GameDate } from '../core/calendar/GameDate.js';
import { DiplomacyFlags } from './DiplomacyFlags.js';
import { TreatyKind } from './TreatyKind.js';
import { Treaty } from './Treaty.js';
import { Claim } from './Claim.js';
import type { World } from '../world/World.js';

/**
 * Diplomacy system: relations, treaties, claims, war exhaustion.
 */
export class Diplomacy {
  private world: World | null;
  private relations: Map<string, DiplomacyFlags> = new Map();
  private treaties: Treaty[] = [];
  private claims: Map<number, Claim[]> = new Map();
  private truceUntil: Map<string, number> = new Map();
  private warExhaustion: Map<number, number> = new Map();

  constructor(world?: World | null) {
    this.world = world ?? null;
  }

  /** The world reference held at construction (may be null). */
  getWorld(): World | null {
    return this.world;
  }

  private pairKey(a: number, b: number): string {
    return a <= b ? `${a}|${b}` : `${b}|${a}`;
  }

  /** Get diplomatic flags between two rulers (read-only safe: returns new object if none). */
  flags(a: number, b: number): DiplomacyFlags {
    return this.relations.get(this.pairKey(a, b)) ?? new DiplomacyFlags();
  }

  /** Get or create diplomatic flags between two rulers. */
  flagsMut(a: number, b: number): DiplomacyFlags {
    const key = this.pairKey(a, b);
    let f = this.relations.get(key);
    if (!f) {
      f = new DiplomacyFlags();
      this.relations.set(key, f);
    }
    return f;
  }

  /** Set two rulers as rivals. */
  setRival(a: number, b: number): void {
    this.flagsMut(a, b).rival = true;
  }

  /** Form an alliance treaty. */
  formAlliance(a: number, b: number, date: GameDate): void {
    const f = this.flagsMut(a, b);
    f.allied = true;
    f.nonAggression = true;
    this.treaties.push(new Treaty(a, b, TreatyKind.ALLIANCE, date, date.year() + 50));
  }

  /** Add an arbitrary treaty to the internal list. */
  addTreaty(treaty: Treaty): void {
    this.treaties.push(treaty);
  }

  /** Set war status; going to war automatically cancels alliance and non-aggression. */
  setAtWar(a: number, b: number, atWar: boolean): void {
    const f = this.flagsMut(a, b);
    f.atWar = atWar;
    if (atWar) {
      f.allied = false;
      f.nonAggression = false;
    }
  }

  /** Whether two rulers are allied. */
  areAllied(a: number, b: number): boolean {
    return this.flags(a, b).allied;
  }

  /** Return all allies of a ruler. */
  alliesOf(who: number): number[] {
    const out: number[] = [];
    for (const t of this.treaties) {
      if (t.kind !== TreatyKind.ALLIANCE) continue;
      if (t.a === who) out.push(t.b);
      else if (t.b === who) out.push(t.a);
    }
    return out;
  }

  /** Add war exhaustion (default: declare-war constant). */
  addWarExhaustion(who: number, amount?: number): void {
    const amt = amount ?? Balance.WAR_EXHAUSTION_ON_DECLARE;
    this.warExhaustion.set(who, (this.warExhaustion.get(who) ?? 0) + amt);
  }

  /** Check whether a ruler can declare war on another in a given year. */
  canDeclareWar(a: number, b: number, year: number): boolean {
    if (a === b) return false;
    if ((this.warExhaustion.get(a) ?? 0) >= Balance.WAR_EXHAUSTION_DECLARE_BLOCK) return false;
    const f = this.flags(a, b);
    if (f.blocksWar() || f.atWar || this.hasTruce(a, b, year)) return false;
    return true;
  }

  /** Whether a valid truce exists between two rulers. */
  hasTruce(a: number, b: number, year: number): boolean {
    return (this.truceUntil.get(this.pairKey(a, b)) ?? 0) > year;
  }

  /** Set truce expiry year. */
  setTruce(a: number, b: number, untilYear: number): void {
    this.truceUntil.set(this.pairKey(a, b), untilYear);
    this.flagsMut(a, b).atWar = false;
  }

  /** Add a claim (with county). */
  addClaim(claimant: number, title: number, county: number | null, strength: number): void;
  /** Add a claim (without county). */
  addClaim(claimant: number, title: number, strength: number): void;
  addClaim(claimant: number, title: number, countyOrStrength: number | null, strength?: number): void {
    if (strength !== undefined) {
      // 3-arg overload: (claimant, title, county, strength)
      const list = this.claims.get(claimant) ?? [];
      list.push(new Claim(claimant, title, countyOrStrength as number | null, strength));
      this.claims.set(claimant, list);
    } else {
      // 2-arg overload: (claimant, title, strength)
      const list = this.claims.get(claimant) ?? [];
      list.push(new Claim(claimant, title, NONE_ID, countyOrStrength as number));
      this.claims.set(claimant, list);
    }
  }

  /** Return all claims of a character. */
  claimsOf(who: number): Claim[] {
    return this.claims.get(who) ?? [];
  }

  /** War exhaustion map (read-only view). */
  getWarExhaustion(): ReadonlyMap<number, number> {
    return this.warExhaustion;
  }

  /** Treaty list (read-only view). */
  getTreaties(): readonly Treaty[] {
    return this.treaties;
  }

  /** Monthly decay of war exhaustion; exceptIds are skipped. */
  tickWarExhaustion(exceptIds?: Iterable<number>): void {
    const skip = new Set(exceptIds ?? []);
    for (const [who, v] of this.warExhaustion) {
      if (skip.has(who)) continue;
      const nv = v - Balance.WAR_EXHAUSTION_DECAY;
      if (nv <= 0) {
        this.warExhaustion.delete(who);
      } else {
        this.warExhaustion.set(who, nv);
      }
    }
  }

  /** Expire treaties past the given year; returns log lines. */
  expireTreaties(year: number, world: World): string[] {
    const lines: string[] = [];
    const remaining: Treaty[] = [];
    for (const t of this.treaties) {
      if (t.expiresYear > year) {
        remaining.push(t);
        continue;
      }
      const aChar = world.characters.get(t.a);
      const bChar = world.characters.get(t.b);
      const an = aChar?.name ?? '?';
      const bn = bChar?.name ?? '?';
      if (t.kind === TreatyKind.TRUCE) {
        lines.push(`${an} 与 ${bn} 的停战到期`);
      } else if (t.kind === TreatyKind.ALLIANCE) {
        const f = this.flagsMut(t.a, t.b);
        f.allied = false;
        lines.push(`${an} 与 ${bn} 的同盟到期`);
      }
    }
    this.treaties = remaining;
    return lines;
  }

  /** Compute opinion gain from a gift. */
  static giftOpinionGain(amount: number): number {
    return Math.max(1, Math.min(30, Math.floor(amount / 5)));
  }

  /** Serialize diplomacy state for save games. */
  saveState(): Record<string, unknown> {
    const s: Record<string, unknown> = {};

    const rels: Record<string, unknown>[] = [];
    for (const [key, f] of this.relations) {
      const parts = key.split('|');
      rels.push({
        a: Number(parts[0]),
        b: Number(parts[1]),
        allied: f.allied,
        atWar: f.atWar,
        nonAggression: f.nonAggression,
        rival: f.rival,
        marriagePact: f.marriagePact,
        vassalage: f.vassalage,
        tradeAgreement: f.tradeAgreement,
        intelligenceSharing: f.intelligenceSharing,
      });
    }
    s['relations'] = rels;

    const ts: Record<string, unknown>[] = [];
    for (const t of this.treaties) {
      ts.push({
        a: t.a,
        b: t.b,
        kind: t.kind,
        startYear: t.start.year(),
        startMonth: t.start.month(),
        startDay: t.start.day(),
        expiresYear: t.expiresYear,
      });
    }
    s['treaties'] = ts;

    const cl: Record<string, Record<string, unknown>[]> = {};
    for (const [claimant, claims] of this.claims) {
      cl[String(claimant)] = claims.map(c => ({
        claimant: c.claimant,
        title: c.title,
        county: c.county,
        pressed: c.pressed,
        strength: c.strength,
      }));
    }
    s['claims'] = cl;

    const tr: Record<string, number> = {};
    for (const [key, val] of this.truceUntil) {
      tr[key] = val;
    }
    s['truceUntil'] = tr;

    const we: Record<string, number> = {};
    for (const [k, v] of this.warExhaustion) {
      we[String(k)] = v;
    }
    s['warExhaustion'] = we;

    return s;
  }

  /** Restore diplomacy state from a save. */
  loadState(s: Record<string, unknown>): void {
    this.relations.clear();
    this.treaties = [];
    this.claims.clear();
    this.truceUntil.clear();
    this.warExhaustion.clear();

    const rels = s['relations'] as Record<string, unknown>[] | undefined;
    if (rels) {
      for (const r of rels) {
        const a = Number(r['a']);
        const b = Number(r['b']);
        const f = this.flagsMut(a, b);
        f.allied = Boolean(r['allied']);
        f.atWar = Boolean(r['atWar']);
        f.nonAggression = Boolean(r['nonAggression']);
        f.rival = Boolean(r['rival']);
        f.marriagePact = Boolean(r['marriagePact']);
        f.vassalage = Boolean(r['vassalage']);
        f.tradeAgreement = Boolean(r['tradeAgreement']);
        f.intelligenceSharing = Boolean(r['intelligenceSharing']);
      }
    }

    const ts = s['treaties'] as Record<string, unknown>[] | undefined;
    if (ts) {
      for (const tm of ts) {
        const a = Number(tm['a']);
        const b = Number(tm['b']);
        const kind = tm['kind'] as TreatyKind;
        const start = new GameDate(
          Number(tm['startYear']),
          Number(tm['startMonth']),
          Number(tm['startDay']),
        );
        const exp = Number(tm['expiresYear']);
        this.treaties.push(new Treaty(a, b, kind, start, exp));
      }
    }

    const cl = s['claims'] as Record<string, Record<string, unknown>[]> | undefined;
    if (cl) {
      for (const [key, clList] of Object.entries(cl)) {
        const claimant = Number(key);
        for (const cm of clList) {
          const title = Number(cm['title']);
          const countyObj = cm['county'];
          const county = countyObj != null ? Number(countyObj) : null;
          const strength = Number(cm['strength']);
          this.addClaim(claimant, title, county, strength);
        }
      }
    }

    const tr = s['truceUntil'] as Record<string, number> | undefined;
    if (tr) {
      for (const [key, val] of Object.entries(tr)) {
        const parts = key.split('|');
        const a = Number(parts[0]);
        const b = Number(parts[1]);
        this.truceUntil.set(this.pairKey(a, b), val);
      }
    }

    const we = s['warExhaustion'] as Record<string, number> | undefined;
    if (we) {
      for (const [key, val] of Object.entries(we)) {
        this.warExhaustion.set(Number(key), val);
      }
    }
  }
}
