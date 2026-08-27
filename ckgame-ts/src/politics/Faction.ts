import { NONE_ID } from '../core/Constants.js';
import { FactionKind } from './FactionKind.js';

/**
 * A faction object.
 */
export class Faction {
  public readonly id: number;
  public readonly kind: FactionKind;
  public readonly targetLiege: number;
  public members: number[] = [];
  public power: number = 0;
  public discontent: number = 0;
  public ultimatumSent: boolean = false;
  public claimant: number = NONE_ID;

  constructor(id: number, kind: FactionKind, targetLiege: number, claimant?: number) {
    this.id = id;
    this.kind = kind;
    this.targetLiege = targetLiege;
    if (claimant !== undefined && claimant !== null) {
      this.claimant = claimant;
    }
  }

  /** Add a member (deduplicated). */
  addMember(who: number): void {
    if (!this.members.includes(who)) {
      this.members.push(who);
    }
  }

  /** Remove a member. */
  removeMember(who: number): void {
    const idx = this.members.indexOf(who);
    if (idx >= 0) this.members.splice(idx, 1);
  }

  /** Whether the faction is ready to revolt. */
  isReadyToRevolt(): boolean {
    return this.power >= 80 && this.discontent >= 50 && this.members.length >= 2;
  }

  /** Whether the faction can send an ultimatum. */
  canSendUltimatum(): boolean {
    return this.power >= 60
      && this.discontent >= 40
      && !this.ultimatumSent
      && this.members.length >= 2;
  }
}
