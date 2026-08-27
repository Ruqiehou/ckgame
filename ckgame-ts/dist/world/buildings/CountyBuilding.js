import { buildingMaxLevel, buildingUpgradeCost } from './BuildingKind.js';
/** 省份中的一座建筑及其等级。 */
export class CountyBuilding {
    kind;
    level;
    constructor(kind, level = 0) {
        this.kind = kind;
        this.level = level;
    }
    canUpgrade() {
        return this.level < buildingMaxLevel(this.kind);
    }
    upgradeCost() {
        return buildingUpgradeCost(this.kind, this.level);
    }
}
//# sourceMappingURL=CountyBuilding.js.map