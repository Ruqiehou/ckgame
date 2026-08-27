/** 地形，影响补给、战斗宽度与开发上限。 */
export enum Terrain {
  PLAINS = 'PLAINS',
  HILLS = 'HILLS',
  MOUNTAINS = 'MOUNTAINS',
  FOREST = 'FOREST',
  DESERT = 'DESERT',
  WETLAND = 'WETLAND',
  FARMLAND = 'FARMLAND',
  COASTAL = 'COASTAL',
}

interface TerrainData {
  supplyLimit: number;
  combatWidth: number;
  developmentCap: number;
}

const TERRAIN_DATA: Record<Terrain, TerrainData> = {
  [Terrain.PLAINS]:   { supplyLimit: 1.0,  combatWidth: 1.0,  developmentCap: 80 },
  [Terrain.HILLS]:    { supplyLimit: 0.8,  combatWidth: 0.8,  developmentCap: 60 },
  [Terrain.MOUNTAINS]:{ supplyLimit: 0.5,  combatWidth: 0.6,  developmentCap: 40 },
  [Terrain.FOREST]:   { supplyLimit: 0.7,  combatWidth: 0.8,  developmentCap: 60 },
  [Terrain.DESERT]:   { supplyLimit: 0.4,  combatWidth: 0.9,  developmentCap: 40 },
  [Terrain.WETLAND]:  { supplyLimit: 0.6,  combatWidth: 0.6,  developmentCap: 50 },
  [Terrain.FARMLAND]: { supplyLimit: 1.3,  combatWidth: 1.0,  developmentCap: 100 },
  [Terrain.COASTAL]:  { supplyLimit: 1.1,  combatWidth: 0.95, developmentCap: 80 },
};

export function terrainSupplyLimit(t: Terrain): number {
  return TERRAIN_DATA[t].supplyLimit;
}

export function terrainCombatWidth(t: Terrain): number {
  return TERRAIN_DATA[t].combatWidth;
}

export function terrainDevelopmentCap(t: Terrain): number {
  return TERRAIN_DATA[t].developmentCap;
}
