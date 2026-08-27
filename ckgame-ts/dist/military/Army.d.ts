import { ArmyStatus } from './ArmyStatus.js';
import { UnitType } from './UnitType.js';
import { UnitStack } from './UnitStack.js';
/**
 * An army with stacks, path, supply, and morale.
 */
export declare class Army {
    id: number;
    owner: number;
    commander: number;
    name: string;
    location: number;
    status: ArmyStatus;
    stacks: UnitStack[];
    path: number[];
    supply: number;
    morale: number;
    constructor(id: number, owner: number, name: string, location: number, commander: number);
    /** Whether the army is still active (not disbanded and has men). */
    isActive(): boolean;
    /** Total men across all stacks. */
    totalMen(): number;
    /** Add men of a given unit type. */
    addMen(unitType: UnitType, men: number): void;
    /** Monthly maintenance cost. */
    monthlyMaintenance(): number;
    /** Set a movement path. */
    setPath(newPath: number[]): void;
    /** Advance along path by one step. */
    advanceMove(moveChance: number): void;
    /** Apply a supply tick. */
    applySupplyTick(inFriendlyCounty: boolean, winter: boolean): void;
}
