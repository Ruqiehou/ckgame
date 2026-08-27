import { NONE_ID } from '../core/Constants.js';
import { GameDate } from '../core/calendar/GameDate.js';
import { CasusBelli } from '../politics/CasusBelli.js';
import type { Diplomacy } from '../politics/Diplomacy.js';
import type { World } from '../world/World.js';
import { Army } from './Army.js';
import { ArmyStatus } from './ArmyStatus.js';
import { UnitType } from './UnitType.js';
import { War } from './War.js';
import { WarParticipant } from './WarParticipant.js';
import { WarResult } from './WarResult.js';

/**
 * War manager: manages all wars and armies.
 */
export class WarManager {
  public wars: Map<number, War> = new Map();
  public armies: Map<number, Army> = new Map();
  public nextWar: number = 1;
  public nextArmy: number = 1;

  private world: World | null;
  private diplomacy: Diplomacy | null;

  constructor(world?: World | null, diplomacy?: Diplomacy | null) {
    this.world = world ?? null;
    this.diplomacy = diplomacy ?? null;
  }

  getWorld(): World | null {
    return this.world;
  }

  getDiplomacy(): Diplomacy | null {
    return this.diplomacy;
  }

  /** Declare war and create a war entry. */
  declareWar(cb: CasusBelli, attacker: number, defender: number, date: GameDate, name: string, targetTitle?: number): number;
  declareWar(cb: CasusBelli, attacker: number, defender: number, date: GameDate, name: string): number;
  declareWar(cb: CasusBelli, attacker: number, defender: number, date: GameDate, name: string, targetTitle: number = NONE_ID): number {
    const wid = this.nextWar++;
    const war = new War(wid, name, cb, attacker, defender, date);
    war.targetTitle = targetTitle;
    war.participants.push(new WarParticipant(attacker, true, date));
    war.participants.push(new WarParticipant(defender, false, date));
    this.wars.set(wid, war);
    return wid;
  }

  /** Get a war by id. */
  war(wid: number): War | undefined {
    return this.wars.get(wid);
  }

  /** All active wars. */
  activeWars(): War[] {
    const out: War[] = [];
    for (const w of this.wars.values()) {
      if (w.active) out.push(w);
    }
    return out;
  }

  /** End a war with the given result. */
  endWar(wid: number, result: WarResult): void {
    const w = this.wars.get(wid);
    if (!w) return;
    w.active = false;
    w.result = result;
  }

  /** Raise a new army. */
  raiseArmy(owner: number, location: number, levies: number, name?: string): number;
  raiseArmy(owner: number, location: number, levies: number): number;
  raiseArmy(owner: number, location: number, levies: number, name: string = ''): number {
    const aid = this.nextArmy++;
    const armyName = (!name || name.trim() === '') ? `军团#${aid}` : name;
    const army = new Army(aid, owner, armyName, location, owner);
    army.addMen(UnitType.LEVIES, levies);
    this.armies.set(aid, army);
    return aid;
  }

  /** Get an army by id. */
  army(aid: number): Army | undefined {
    return this.armies.get(aid);
  }

  /** All active army ids for a given owner. */
  armiesOf(owner: number): number[] {
    const out: number[] = [];
    for (const a of this.armies.values()) {
      if (a.owner === owner && a.isActive()) out.push(a.id);
    }
    return out;
  }

  /** Total men across all active armies for a given owner. */
  totalMenOf(owner: number): number {
    let total = 0;
    for (const a of this.armies.values()) {
      if (a.owner === owner && a.isActive()) total += a.totalMen();
    }
    return total;
  }

  /** Tick movement for all moving armies. */
  tickMovement(moveChanceOf?: (army: Army) => number): void {
    for (const army of this.armies.values()) {
      if (army.status === ArmyStatus.MOVING) {
        const chance = moveChanceOf ? moveChanceOf(army) : 1.0;
        army.advanceMove(chance);
      }
    }
  }

  /** Disband empty armies. */
  disbandEmpty(): void {
    for (const army of this.armies.values()) {
      if (army.totalMen() <= 0) {
        army.status = ArmyStatus.DISBANDED;
      }
    }
  }

  /** Set path for an army. */
  move(armyId: number, path: number[]): void {
    const army = this.armies.get(armyId);
    if (army) army.setPath(path);
  }

  /** Disband an army. */
  disband(armyId: number): void {
    const army = this.armies.get(armyId);
    if (army) army.status = ArmyStatus.DISBANDED;
  }

  /** Daily tick: movement + cleanup. */
  tick(): void {
    this.tickMovement();
    this.disbandEmpty();
  }
}
