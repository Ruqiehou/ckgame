import { Balance } from '../core/balance/Balance.js';
import type { World } from '../world/World.js';
import { Faction } from './Faction.js';
import { FactionKind } from './FactionKind.js';
import { FactionEvent, createFactionEvent } from './FactionEvent.js';

/**
 * Faction manager: creates, tracks, and ticks factions.
 */
export class FactionManager {
  private world: World | null;
  private factions: Map<number, Faction> = new Map();
  private nextId: number = 1;

  constructor(world?: World | null) {
    this.world = world ?? null;
  }

  getWorld(): World | null {
    return this.world;
  }

  /** Return factions map (read-only view). */
  getFactions(): ReadonlyMap<number, Faction> {
    return this.factions;
  }

  /** Create a new faction and return its id. */
  create(kind: FactionKind, liege: number, claimant?: number): number {
    const fid = this.nextId++;
    this.factions.set(fid, new Faction(fid, kind, liege, claimant));
    return fid;
  }

  /** A character joins a faction. */
  join(factionId: number, who: number): void {
    const f = this.factions.get(factionId);
    if (f) f.addMember(who);
  }

  /** A character leaves a faction. */
  leave(factionId: number, who: number): void {
    const f = this.factions.get(factionId);
    if (f) f.removeMember(who);
  }

  /** Dissolve a faction. */
  dissolve(factionId: number): void {
    this.factions.delete(factionId);
  }

  /**
   * Liege appeases a faction: reduces discontent; may dissolve if low enough.
   * @param amount If undefined, uses default appease value.
   */
  appease(factionId: number, amount?: number): boolean {
    const delta = amount ?? Balance.FACTION_APPEASE_DEFAULT;
    const f = this.factions.get(factionId);
    if (!f) return false;
    f.discontent = Math.max(0, f.discontent - delta);
    f.ultimatumSent = false;
    if (f.discontent <= Balance.FACTION_DISSOLVE_DISCONTENT && f.power < Balance.FACTION_DISSOLVE_POWER) {
      this.dissolve(factionId);
    }
    return true;
  }

  /** Find a faction for a given liege and kind. Returns faction id or null. */
  findForLiege(liege: number, kind: FactionKind): number | null {
    for (const f of this.factions.values()) {
      if (f.targetLiege === liege && f.kind === kind) {
        return f.id;
      }
    }
    return null;
  }

  /** Recompute faction power from military power maps. */
  recomputePower(militaryPower: Map<number, number>, liegePower: Map<number, number>): void {
    for (const f of this.factions.values()) {
      let memberPower = 0;
      for (const m of f.members) {
        memberPower += militaryPower.get(m) ?? 10;
      }
      const lp = Math.max(1, liegePower.get(f.targetLiege) ?? 100);
      f.power = Math.min(200, (memberPower / lp) * 100);
    }
  }

  /** Tick discontent based on opinion of liege. */
  tickDiscontent(opinionOfLiege: Map<number, number>): void {
    for (const f of this.factions.values()) {
      let avg: number;
      if (f.members.length === 0) {
        avg = 0;
      } else {
        let sum = 0;
        for (const m of f.members) {
          sum += opinionOfLiege.get(m) ?? 0;
        }
        avg = sum / f.members.length;
      }
      if (avg < 0) {
        f.discontent = Math.min(100, f.discontent + (-avg) * 0.15);
      } else {
        f.discontent = Math.max(0, f.discontent - avg * 0.08);
      }
      if (f.members.length < 2) {
        f.discontent = Math.max(0, f.discontent - 5);
      }
    }
  }

  /**
   * Monthly AI for factions.
   * @param vassals Array of [vassal, liege, opinion] triples
   * @param rngRoll Function returning 0~1 random number
   */
  monthlyAi(vassals: number[][], rngRoll: () => number): FactionEvent[] {
    const events: FactionEvent[] = [];

    for (const row of vassals) {
      const vassal = row[0];
      const liege = row[1];
      const opinion = row[2];

      if (opinion > -10) {
        // Leave all factions targeting this liege
        const leaveIds: number[] = [];
        for (const f of this.factions.values()) {
          if (f.targetLiege === liege && f.members.includes(vassal)) {
            leaveIds.push(f.id);
          }
        }
        for (const fid of leaveIds) {
          this.leave(fid, vassal);
        }
        continue;
      }

      if (rngRoll() > 0.25) continue;

      let kind: FactionKind;
      if (opinion < -50) {
        kind = FactionKind.INDEPENDENCE;
      } else if (opinion < -30) {
        kind = FactionKind.LOWER_CROWN_AUTHORITY;
      } else {
        kind = FactionKind.LIBERTY;
      }

      const fid = this.findForLiege(liege, kind);
      if (fid !== null) {
        const f = this.factions.get(fid)!;
        if (!f.members.includes(vassal)) {
          this.join(fid, vassal);
          events.push(createFactionEvent('joined', fid, liege, 0, vassal));
        }
      } else if (rngRoll() < 0.4) {
        const newFid = this.create(kind, liege);
        this.join(newFid, vassal);
        events.push(createFactionEvent('formed', newFid, liege, vassal, 0, kind));
      }
    }

    // Dissolve empty factions
    const empty: number[] = [];
    for (const f of this.factions.values()) {
      if (f.members.length === 0) empty.push(f.id);
    }
    for (const fid of empty) {
      this.dissolve(fid);
      events.push(createFactionEvent('dissolved', fid, 0, 0, 0, undefined, undefined, '无人支持'));
    }

    // Check ultimatums and revolts
    for (const f of [...this.factions.values()]) {
      if (f.canSendUltimatum()) {
        f.ultimatumSent = true;
        events.push(createFactionEvent('ultimatum', f.id, f.targetLiege, 0, 0, f.kind, [...f.members]));
      } else if (f.isReadyToRevolt()) {
        events.push(createFactionEvent('revolt', f.id, f.targetLiege, 0, 0, f.kind, [...f.members]));
      }
    }

    return events;
  }

  /** Serialize faction state for save games. */
  saveState(): Record<string, unknown> {
    const s: Record<string, unknown> = {};
    s['nextId'] = this.nextId;
    const fm: Record<string, Record<string, unknown>> = {};
    for (const [id, f] of this.factions) {
      fm[String(id)] = {
        kind: f.kind,
        targetLiege: f.targetLiege,
        members: [...f.members],
        power: f.power,
        discontent: f.discontent,
        ultimatumSent: f.ultimatumSent,
        claimant: f.claimant,
      };
    }
    s['factions'] = fm;
    return s;
  }

  /** Restore faction state from a save. */
  loadState(s: Record<string, unknown>): void {
    this.factions.clear();
    this.nextId = Number(s['nextId'] ?? 1);
    const fm = s['factions'] as Record<string, Record<string, unknown>> | undefined;
    if (!fm) return;
    for (const [key, m] of Object.entries(fm)) {
      const id = Number(key);
      const kind = m['kind'] as FactionKind;
      const liege = Number(m['targetLiege']);
      const claimantObj = m['claimant'];
      const claimant = claimantObj != null ? Number(claimantObj) : undefined;
      const f = new Faction(id, kind, liege, claimant);
      f.members = [...(m['members'] as number[])];
      f.power = Number(m['power']);
      f.discontent = Number(m['discontent']);
      f.ultimatumSent = Boolean(m['ultimatumSent']);
      this.factions.set(id, f);
    }
  }
}
