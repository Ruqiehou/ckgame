import { GameDate } from '../core/calendar/GameDate.js';
import { Siege } from './Siege.js';
import { SiegeEvent } from './SiegeEvent.js';

/**
 * Siege manager: tracks all active sieges.
 */
export class SiegeManager {
  public sieges: Map<number, Siege> = new Map();
  public nextId: number = 1;

  /**
   * Start a siege on a county. If the county already has an active siege, return its id.
   */
  start(
    county: number,
    attackerArmy: number,
    attacker: number,
    defender: number,
    fortLevel: number,
    garrison: number,
    date: GameDate,
  ): number {
    for (const s of this.sieges.values()) {
      if (s.active && s.county === county) return s.id;
    }
    const sid = this.nextId++;
    const siege = new Siege(sid, county, attackerArmy, attacker, defender);
    siege.fortLevel = fortLevel;
    siege.garrison = garrison;
    siege.started = date;
    this.sieges.set(sid, siege);
    return sid;
  }

  /** Find the active siege at a county. */
  activeAt(county: number): Siege | undefined {
    for (const s of this.sieges.values()) {
      if (s.active && s.county === county) return s;
    }
    return undefined;
  }

  /** All active sieges. */
  activeSieges(): Siege[] {
    const out: Siege[] = [];
    for (const s of this.sieges.values()) {
      if (s.active) out.push(s);
    }
    return out;
  }

  /**
   * Daily tick for all sieges.
   * @param armyMen Map from army id to current men count
   * @param armyMartial Map from army id to commander martial
   * @param armyLocation Map from army id to current location
   */
  tickDay(
    armyMen: Map<number, number>,
    armyMartial: Map<number, number>,
    armyLocation: Map<number, number>,
  ): SiegeEvent[] {
    const events: SiegeEvent[] = [];
    const completed: number[] = [];

    for (const s of this.sieges.values()) {
      if (!s.active) continue;

      const loc = armyLocation.get(s.attackerArmy);
      const men = armyMen.get(s.attackerArmy) ?? 0;

      if (loc === undefined || loc !== s.county || men <= 0) {
        s.active = false;
        events.push(new SiegeEvent('lifted', s.id, s.county, 0, 0, 0, 0, '攻城部队离开或溃散'));
        continue;
      }

      const martial = armyMartial.get(s.attackerArmy) ?? 8;
      s.dailyTick(men, martial);
      events.push(new SiegeEvent('progressed', s.id, s.county, s.attacker, s.defender, s.progress, s.requiredProgress()));

      if (s.isComplete()) {
        completed.push(s.id);
      }
    }

    for (const sid of completed) {
      const s = this.sieges.get(sid);
      if (s) {
        s.active = false;
        events.push(new SiegeEvent('captured', sid, s.county, s.attacker, s.defender));
      }
    }

    return events;
  }
}
