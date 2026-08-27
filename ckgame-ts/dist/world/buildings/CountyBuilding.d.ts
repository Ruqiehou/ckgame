import { BuildingKind } from './BuildingKind.js';
/** 省份中的一座建筑及其等级。 */
export declare class CountyBuilding {
    kind: BuildingKind;
    level: number;
    constructor(kind: BuildingKind, level?: number);
    canUpgrade(): boolean;
    upgradeCost(): number;
}
