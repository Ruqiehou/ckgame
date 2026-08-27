import { unitDamageValue } from './UnitType.js';
/**
 * A stack of units of the same type.
 */
export class UnitStack {
    unitType;
    men;
    maxMen;
    constructor(unitType, men, maxMen) {
        this.unitType = unitType;
        this.men = men;
        this.maxMen = maxMen;
    }
    /** Current men as ratio of max. */
    strengthRatio() {
        return this.maxMen === 0 ? 0 : this.men / this.maxMen;
    }
    /** Combat power accounting for current strength ratio. */
    combatPower() {
        return this.men * unitDamageValue(this.unitType) * this.strengthRatio();
    }
    /** Take casualties, returns actual killed. */
    takeCasualties(amount) {
        const killed = Math.min(this.men, amount);
        this.men -= killed;
        return killed;
    }
}
//# sourceMappingURL=UnitStack.js.map