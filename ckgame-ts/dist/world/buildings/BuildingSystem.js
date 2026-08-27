import { BuildingKind, buildingEffectValue } from './BuildingKind.js';
import { CountyBuilding } from './CountyBuilding.js';
/** 建筑系统：管理省份建筑与升级。 */
export class BuildingSystem {
    buildings = new Map();
    getBuildings(countyId) {
        return this.buildings.get(countyId) ?? [];
    }
    getBuilding(countyId, kind) {
        for (const b of this.getBuildings(countyId)) {
            if (b.kind === kind)
                return b;
        }
        return null;
    }
    addBuilding(countyId, kind) {
        const b = new CountyBuilding(kind);
        const list = this.buildings.get(countyId);
        if (list) {
            list.push(b);
        }
        else {
            this.buildings.set(countyId, [b]);
        }
        return b;
    }
    upgrade(countyId, kind) {
        const b = this.getBuilding(countyId, kind);
        if (b !== null && b.canUpgrade()) {
            b.level += 1;
            return true;
        }
        return false;
    }
    /** 获取省份所有建筑的效果总和。 */
    getEffects(countyId) {
        const effects = new Map();
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
    saveState() {
        const s = {};
        for (const [countyId, bList] of this.buildings.entries()) {
            s[String(countyId)] = bList.map(b => ({ kind: b.kind, level: b.level }));
        }
        return s;
    }
    /** 从存档恢复建筑状态。 */
    loadState(s) {
        this.buildings.clear();
        for (const [key, bl] of Object.entries(s)) {
            const countyId = parseInt(key, 10);
            const list = [];
            for (const bm of bl) {
                const kind = bm.kind;
                const level = bm.level;
                list.push(new CountyBuilding(kind, level));
            }
            this.buildings.set(countyId, list);
        }
    }
}
function addEffect(m, key, v) {
    m.set(key, (m.get(key) ?? 0) + v);
}
//# sourceMappingURL=BuildingSystem.js.map