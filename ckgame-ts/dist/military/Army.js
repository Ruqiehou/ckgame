import { ArmyStatus } from './ArmyStatus.js';
import { unitMaintenanceValue } from './UnitType.js';
import { UnitStack } from './UnitStack.js';
/**
 * An army with stacks, path, supply, and morale.
 */
export class Army {
    id;
    owner;
    commander;
    name;
    location;
    status = ArmyStatus.IDLE;
    stacks = [];
    path = [];
    supply = 100;
    morale = 100;
    constructor(id, owner, name, location, commander) {
        this.id = id;
        this.owner = owner;
        this.name = name;
        this.location = location;
        this.commander = commander;
    }
    /** Whether the army is still active (not disbanded and has men). */
    isActive() {
        return this.status !== ArmyStatus.DISBANDED && this.totalMen() > 0;
    }
    /** Total men across all stacks. */
    totalMen() {
        let total = 0;
        for (const stack of this.stacks) {
            total += Math.max(0, stack.men);
        }
        return total;
    }
    /** Add men of a given unit type. */
    addMen(unitType, men) {
        if (men <= 0)
            return;
        for (const stack of this.stacks) {
            if (stack.unitType === unitType) {
                stack.men += men;
                stack.maxMen += men;
                return;
            }
        }
        this.stacks.push(new UnitStack(unitType, men, men));
    }
    /** Monthly maintenance cost. */
    monthlyMaintenance() {
        let total = 0;
        for (const stack of this.stacks) {
            total += stack.men * unitMaintenanceValue(stack.unitType);
        }
        return total;
    }
    /** Set a movement path. */
    setPath(newPath) {
        this.path = [];
        if (!newPath || newPath.length === 0) {
            this.status = ArmyStatus.IDLE;
            return;
        }
        this.path = [...newPath];
        // Remove current location from start of path
        if (this.path.length > 0 && this.path[0] === this.location) {
            this.path.shift();
        }
        this.status = this.path.length === 0 ? ArmyStatus.IDLE : ArmyStatus.MOVING;
    }
    /** Advance along path by one step. */
    advanceMove(moveChance) {
        if ((this.status !== ArmyStatus.MOVING && this.status !== ArmyStatus.RETREATING) || this.path.length === 0) {
            return;
        }
        if (moveChance < 1.0 && Math.random() > Math.max(0.0, moveChance)) {
            return;
        }
        this.location = this.path.shift();
        if (this.status === ArmyStatus.RETREATING && this.path.length > 0) {
            return;
        }
        this.status = this.path.length === 0 ? ArmyStatus.IDLE : ArmyStatus.MOVING;
    }
    /** Apply a supply tick. */
    applySupplyTick(inFriendlyCounty, winter) {
        let delta = inFriendlyCounty ? 5 : -8;
        if (winter)
            delta -= 4;
        this.supply = Math.max(0, Math.min(150, this.supply + delta));
        if (this.supply < 30) {
            this.morale = Math.max(10, this.morale - 3);
        }
        else if (this.supply > 80) {
            this.morale = Math.min(100, this.morale + 1);
        }
        if (this.totalMen() <= 0) {
            this.status = ArmyStatus.DISBANDED;
        }
    }
}
//# sourceMappingURL=Army.js.map