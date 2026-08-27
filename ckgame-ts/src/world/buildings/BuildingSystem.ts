import { BuildingKind, buildingEffectValue } from './BuildingKind.js';
import { CountyBuilding } from './CountyBuilding.js';

/** 建筑系统：管理省份建筑与升级。 */
export class BuildingSystem {
  private buildings: Map<number, CountyBuilding[]> = new Map();

  getBuildings(countyId: number): CountyBuilding[] {
    return this.buildings.get(countyId) ?? [];
  }

  getBuilding(countyId: number, kind: BuildingKind): CountyBuilding | null {
    for (const b of this.getBuildings(countyId)) {
      if (b.kind === kind) return b;
    }
    return null;
  }

  addBuilding(countyId: number, kind: BuildingKind): CountyBuilding {
    const b = new CountyBuilding(kind);
    const list = this.buildings.get(countyId);
    if (list) {
      list.push(b);
    } else {
      this.buildings.set(countyId, [b]);
    }
    return b;
  }

  upgrade(countyId: number, kind: BuildingKind): boolean {
    const b = this.getBuilding(countyId, kind);
    if (b !== null && b.canUpgrade()) {
      b.level += 1;
      return true;
    }
    return false;
  }

  /** 获取省份所有建筑的效果总和。 */
  getEffects(countyId: number): Map<string, number> {
    const effects = new Map<string, number>();
    effects.set('tax_income', 0);
    effects.set('levy_max', 0);
    effects.set('garrison', 0);
    effects.set('defense', 0);
    effects.set('stability', 0);
    effects.set('vision', 0);
    effects.set('cavalry_bonus', 0);
    effects.set('production', 0);

    for (const b of this.getBuildings(countyId)) {
      switch (b.kind) {
        case BuildingKind.FARM:
          addEffect(effects, 'tax_income', buildingEffectValue(b.kind, b.level));
          break;
        case BuildingKind.BARRACKS:
          addEffect(effects, 'levy_max', buildingEffectValue(b.kind, b.level));
          break;
        case BuildingKind.WALLS:
          addEffect(effects, 'garrison', buildingEffectValue(b.kind, b.level));
          addEffect(effects, 'defense', buildingEffectValue(b.kind, b.level) * 0.5);
          break;
        case BuildingKind.MARKET:
          addEffect(effects, 'tax_income', buildingEffectValue(b.kind, b.level));
          break;
        case BuildingKind.CHURCH:
          addEffect(effects, 'stability', buildingEffectValue(b.kind, b.level));
          break;
        case BuildingKind.WATCH_TOWER:
          addEffect(effects, 'vision', buildingEffectValue(b.kind, b.level));
          break;
        case BuildingKind.STABLE:
          addEffect(effects, 'cavalry_bonus', buildingEffectValue(b.kind, b.level));
          break;
        case BuildingKind.WORKSHOP:
          addEffect(effects, 'production', buildingEffectValue(b.kind, b.level));
          break;
      }
    }
    return effects;
  }

  /** 序列化建筑状态（供存档使用）。 */
  saveState(): Record<string, Array<{ kind: string; level: number }>> {
    const s: Record<string, Array<{ kind: string; level: number }>> = {};
    for (const [countyId, bList] of this.buildings.entries()) {
      s[String(countyId)] = bList.map(b => ({ kind: b.kind, level: b.level }));
    }
    return s;
  }

  /** 从存档恢复建筑状态。 */
  loadState(s: Record<string, Array<{ kind: string; level: number }>>): void {
    this.buildings.clear();
    for (const [key, bl] of Object.entries(s)) {
      const countyId = parseInt(key, 10);
      const list: CountyBuilding[] = [];
      for (const bm of bl) {
        const kind = bm.kind as BuildingKind;
        const level = bm.level;
        list.push(new CountyBuilding(kind, level));
      }
      this.buildings.set(countyId, list);
    }
  }
}

function addEffect(m: Map<string, number>, key: string, v: number): void {
  m.set(key, (m.get(key) ?? 0) + v);
}
