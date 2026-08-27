import { BuildingKind } from './BuildingKind.js';
import { CountyBuilding } from './CountyBuilding.js';
/** 建筑系统：管理省份建筑与升级。 */
export declare class BuildingSystem {
    private buildings;
    getBuildings(countyId: number): CountyBuilding[];
    getBuilding(countyId: number, kind: BuildingKind): CountyBuilding | null;
    addBuilding(countyId: number, kind: BuildingKind): CountyBuilding;
    upgrade(countyId: number, kind: BuildingKind): boolean;
    /** 获取省份所有建筑的效果总和。 */
    getEffects(countyId: number): Map<string, number>;
    /** 序列化建筑状态（供存档使用）。 */
    saveState(): Record<string, Array<{
        kind: string;
        level: number;
    }>>;
    /** 从存档恢复建筑状态。 */
    loadState(s: Record<string, Array<{
        kind: string;
        level: number;
    }>>): void;
}
