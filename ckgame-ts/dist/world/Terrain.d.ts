/** 地形，影响补给、战斗宽度与开发上限。 */
export declare enum Terrain {
    PLAINS = "PLAINS",
    HILLS = "HILLS",
    MOUNTAINS = "MOUNTAINS",
    FOREST = "FOREST",
    DESERT = "DESERT",
    WETLAND = "WETLAND",
    FARMLAND = "FARMLAND",
    COASTAL = "COASTAL"
}
export declare function terrainSupplyLimit(t: Terrain): number;
export declare function terrainCombatWidth(t: Terrain): number;
export declare function terrainDevelopmentCap(t: Terrain): number;
