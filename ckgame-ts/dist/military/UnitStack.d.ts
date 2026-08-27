import { UnitType } from './UnitType.js';
/**
 * A stack of units of the same type.
 */
export declare class UnitStack {
    unitType: UnitType;
    men: number;
    maxMen: number;
    constructor(unitType: UnitType, men: number, maxMen: number);
    /** Current men as ratio of max. */
    strengthRatio(): number;
    /** Combat power accounting for current strength ratio. */
    combatPower(): number;
    /** Take casualties, returns actual killed. */
    takeCasualties(amount: number): number;
}
