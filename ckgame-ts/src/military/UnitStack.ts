import { UnitType, unitDamageValue } from './UnitType.js';

/**
 * A stack of units of the same type.
 */
export class UnitStack {
  public unitType: UnitType;
  public men: number;
  public maxMen: number;

  constructor(unitType: UnitType, men: number, maxMen: number) {
    this.unitType = unitType;
    this.men = men;
    this.maxMen = maxMen;
  }

  /** Current men as ratio of max. */
  strengthRatio(): number {
    return this.maxMen === 0 ? 0 : this.men / this.maxMen;
  }

  /** Combat power accounting for current strength ratio. */
  combatPower(): number {
    return this.men * unitDamageValue(this.unitType) * this.strengthRatio();
  }

  /** Take casualties, returns actual killed. */
  takeCasualties(amount: number): number {
    const killed = Math.min(this.men, amount);
    this.men -= killed;
    return killed;
  }
}
