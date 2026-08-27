/**
 * Unit types with combat stats.
 */
export declare enum UnitType {
    LEVIES = "LEVIES",
    LIGHT_INFANTRY = "LIGHT_INFANTRY",
    HEAVY_INFANTRY = "HEAVY_INFANTRY",
    PIKEMEN = "PIKEMEN",
    ARCHERS = "ARCHERS",
    LIGHT_CAVALRY = "LIGHT_CAVALRY",
    HEAVY_CAVALRY = "HEAVY_CAVALRY",
    KNIGHTS = "KNIGHTS"
}
export declare function unitDamageValue(u: UnitType): number;
export declare function unitToughnessValue(u: UnitType): number;
export declare function unitMaintenanceValue(u: UnitType): number;
