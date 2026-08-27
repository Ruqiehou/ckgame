import { BuildingKind, buildingMaxLevel, buildingUpgradeCost } from './BuildingKind.js';

/** 省份中的一座建筑及其等级。 */
export class CountyBuilding {
  kind: BuildingKind;
  level: number;

  constructor(kind: BuildingKind, level: number = 0) {
    this.kind = kind;
    this.level = level;
  }

  canUpgrade(): boolean {
    return this.level < buildingMaxLevel(this.kind);
  }

  upgradeCost(): number {
    return buildingUpgradeCost(this.kind, this.level);
  }
}
