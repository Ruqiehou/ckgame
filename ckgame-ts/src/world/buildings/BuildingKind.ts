/** 建筑类型与数值。 */
export enum BuildingKind {
  FARM = 'FARM',
  BARRACKS = 'BARRACKS',
  WALLS = 'WALLS',
  MARKET = 'MARKET',
  CHURCH = 'CHURCH',
  WATCH_TOWER = 'WATCH_TOWER',
  STABLE = 'STABLE',
  WORKSHOP = 'WORKSHOP',
}

interface BuildingKindData {
  nameZh: string;
  description: string;
  maxLevel: number;
  baseCost: number;
  effectPerLevel: number;
}

const BUILDING_DATA: Record<BuildingKind, BuildingKindData> = {
  [BuildingKind.FARM]:        { nameZh: '农田',   description: '增加省份税收收入',         maxLevel: 5, baseCost: 50,  effectPerLevel: 5 },
  [BuildingKind.BARRACKS]:    { nameZh: '兵营',   description: '增加征兵上限和训练速度',     maxLevel: 3, baseCost: 100, effectPerLevel: 20 },
  [BuildingKind.WALLS]:       { nameZh: '城墙',   description: '增加守军数量和防御加成',     maxLevel: 5, baseCost: 80,  effectPerLevel: 15 },
  [BuildingKind.MARKET]:      { nameZh: '市场',   description: '增加贸易和税收收入',         maxLevel: 3, baseCost: 120, effectPerLevel: 8 },
  [BuildingKind.CHURCH]:      { nameZh: '教堂',   description: '增加稳定度和民众忠诚',       maxLevel: 3, baseCost: 100, effectPerLevel: 1 },
  [BuildingKind.WATCH_TOWER]: { nameZh: '瞭望塔', description: '增加视野范围，提前预警',     maxLevel: 2, baseCost: 60,  effectPerLevel: 2 },
  [BuildingKind.STABLE]:      { nameZh: '马厩',   description: '提升骑兵战斗效率',           maxLevel: 2, baseCost: 150, effectPerLevel: 0.1 },
  [BuildingKind.WORKSHOP]:    { nameZh: '工坊',   description: '提升装备生产效率',           maxLevel: 2, baseCost: 100, effectPerLevel: 0.05 },
};

export function buildingNameZh(kind: BuildingKind): string {
  return BUILDING_DATA[kind].nameZh;
}

export function buildingDescription(kind: BuildingKind): string {
  return BUILDING_DATA[kind].description;
}

export function buildingMaxLevel(kind: BuildingKind): number {
  return BUILDING_DATA[kind].maxLevel;
}

/** 升级到下一级所需的金币。 */
export function buildingUpgradeCost(kind: BuildingKind, level: number): number {
  return Math.floor(BUILDING_DATA[kind].baseCost * Math.pow(1.5, level));
}

/** 每级提供的效果值。 */
export function buildingEffectValue(kind: BuildingKind, level: number): number {
  return BUILDING_DATA[kind].effectPerLevel * level;
}
