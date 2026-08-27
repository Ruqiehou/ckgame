/**
 * Siege event (progress, capture, or lift).
 */
export class SiegeEvent {
  public readonly kind: string; // 'progressed' | 'captured' | 'lifted'
  public readonly siegeId: number;
  public readonly county: number;
  public readonly attacker: number;
  public readonly defender: number;
  public readonly progress: number;
  public readonly required: number;
  public readonly reason: string;

  constructor(
    kind: string,
    siegeId: number,
    county: number,
    attacker: number = 0,
    defender: number = 0,
    progress: number = 0,
    required: number = 0,
    reason: string = '',
  ) {
    this.kind = kind;
    this.siegeId = siegeId;
    this.county = county;
    this.attacker = attacker;
    this.defender = defender;
    this.progress = progress;
    this.required = required;
    this.reason = reason;
  }
}
