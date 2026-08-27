/** 地形，影响补给、战斗宽度与开发上限。 */
export var Terrain;
(function (Terrain) {
    Terrain["PLAINS"] = "PLAINS";
    Terrain["HILLS"] = "HILLS";
    Terrain["MOUNTAINS"] = "MOUNTAINS";
    Terrain["FOREST"] = "FOREST";
    Terrain["DESERT"] = "DESERT";
    Terrain["WETLAND"] = "WETLAND";
    Terrain["FARMLAND"] = "FARMLAND";
    Terrain["COASTAL"] = "COASTAL";
})(Terrain || (Terrain = {}));
const TERRAIN_DATA = {
    [Terrain.PLAINS]: { supplyLimit: 1.0, combatWidth: 1.0, developmentCap: 80 },
    [Terrain.HILLS]: { supplyLimit: 0.8, combatWidth: 0.8, developmentCap: 60 },
    [Terrain.MOUNTAINS]: { supplyLimit: 0.5, combatWidth: 0.6, developmentCap: 40 },
    [Terrain.FOREST]: { supplyLimit: 0.7, combatWidth: 0.8, developmentCap: 60 },
    [Terrain.DESERT]: { supplyLimit: 0.4, combatWidth: 0.9, developmentCap: 40 },
    [Terrain.WETLAND]: { supplyLimit: 0.6, combatWidth: 0.6, developmentCap: 50 },
    [Terrain.FARMLAND]: { supplyLimit: 1.3, combatWidth: 1.0, developmentCap: 100 },
    [Terrain.COASTAL]: { supplyLimit: 1.1, combatWidth: 0.95, developmentCap: 80 },
};
export function terrainSupplyLimit(t) {
    return TERRAIN_DATA[t].supplyLimit;
}
export function terrainCombatWidth(t) {
    return TERRAIN_DATA[t].combatWidth;
}
export function terrainDevelopmentCap(t) {
    return TERRAIN_DATA[t].developmentCap;
}
//# sourceMappingURL=Terrain.js.map