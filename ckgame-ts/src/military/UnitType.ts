/**
 * Unit types with combat stats.
 */
export enum UnitType {
  LEVIES = 'LEVIES',
  LIGHT_INFANTRY = 'LIGHT_INFANTRY',
  HEAVY_INFANTRY = 'HEAVY_INFANTRY',
  PIKEMEN = 'PIKEMEN',
  ARCHERS = 'ARCHERS',
  LIGHT_CAVALRY = 'LIGHT_CAVALRY',
  HEAVY_CAVALRY = 'HEAVY_CAVALRY',
  KNIGHTS = 'KNIGHTS',
}

const unitDamage: Record<UnitType, number> = {
  [UnitType.LEVIES]: 5.0,
  [UnitType.LIGHT_INFANTRY]: 8.0,
  [UnitType.HEAVY_INFANTRY]: 14.0,
  [UnitType.PIKEMEN]: 12.0,
  [UnitType.ARCHERS]: 10.0,
  [UnitType.LIGHT_CAVALRY]: 12.0,
  [UnitType.HEAVY_CAVALRY]: 20.0,
  [UnitType.KNIGHTS]: 28.0,
};

const unitToughness: Record<UnitType, number> = {
  [UnitType.LEVIES]: 4.0,
  [UnitType.LIGHT_INFANTRY]: 6.0,
  [UnitType.HEAVY_INFANTRY]: 12.0,
  [UnitType.PIKEMEN]: 14.0,
  [UnitType.ARCHERS]: 5.0,
  [UnitType.LIGHT_CAVALRY]: 8.0,
  [UnitType.HEAVY_CAVALRY]: 14.0,
  [UnitType.KNIGHTS]: 18.0,
};

const unitMaintenance: Record<UnitType, number> = {
  [UnitType.LEVIES]: 0.05,
  [UnitType.LIGHT_INFANTRY]: 0.1,
  [UnitType.HEAVY_INFANTRY]: 0.25,
  [UnitType.PIKEMEN]: 0.2,
  [UnitType.ARCHERS]: 0.15,
  [UnitType.LIGHT_CAVALRY]: 0.3,
  [UnitType.HEAVY_CAVALRY]: 0.5,
  [UnitType.KNIGHTS]: 0.8,
};

export function unitDamageValue(u: UnitType): number {
  return unitDamage[u] ?? 1.0;
}

export function unitToughnessValue(u: UnitType): number {
  return unitToughness[u] ?? 1.0;
}

export function unitMaintenanceValue(u: UnitType): number {
  return unitMaintenance[u] ?? 0.1;
}
