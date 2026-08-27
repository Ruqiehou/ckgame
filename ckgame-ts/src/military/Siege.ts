import { GameDate } from '../core/calendar/GameDate.js';

/**
 * A siege on a county.
 */
export class Siege {
  public id: number;
  public county: number;
  public attackerArmy: number;
  public attacker: number;
  public defender: number;
  public progress: number = 0;
  public fortLevel: number = 1;
  public garrison: number = 50;
  public started: GameDate | null = null;
  public active: boolean = true;

  constructor(id: number, county: number, attackerArmy: number, attacker: number, defender: number) {
    this.id = id;
    this.county = county;
    this.attackerArmy = attackerArmy;
    this.attacker = attacker;
    this.defender = defender;
  }

  /** Required total siege progress. */
  requiredProgress(): number {
    return 100 + this.fortLevel * 40;
  }

  /** Whether the siege is complete. */
  isComplete(): boolean {
    return this.progress >= this.requiredProgress();
  }

  /** Daily tick: advance siege progress. Returns daily gain. */
  dailyTick(besiegerMen: number, martial: number): number {
    if (!this.active) return 0;
    const menFactor = Math.max(0.2, Math.min(3.0, besiegerMen / Math.max(50, this.garrison)));
    const fortPenalty = 1.0 / (1.0 + this.fortLevel * 0.25);
    const martialBonus = 1.0 + (martial - 8) * 0.03;
    const gain = 1.2 * menFactor * fortPenalty * martialBonus;
    this.progress = Math.min(this.requiredProgress(), this.progress + gain);
    return gain;
  }
}
