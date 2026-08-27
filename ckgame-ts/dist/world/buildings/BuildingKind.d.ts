/** 建筑类型与数值。 */
export declare enum BuildingKind {
    FARM = "FARM",
    BARRACKS = "BARRACKS",
    WALLS = "WALLS",
    MARKET = "MARKET",
    CHURCH = "CHURCH",
    WATCH_TOWER = "WATCH_TOWER",
    STABLE = "STABLE",
    WORKSHOP = "WORKSHOP"
}
export declare function buildingNameZh(kind: BuildingKind): string;
export declare function buildingDescription(kind: BuildingKind): string;
export declare function buildingMaxLevel(kind: BuildingKind): number;
/** 升级到下一级所需的金币。 */
export declare function buildingUpgradeCost(kind: BuildingKind, level: number): number;
/** 每级提供的效果值。 */
export declare function buildingEffectValue(kind: BuildingKind, level: number): number;
