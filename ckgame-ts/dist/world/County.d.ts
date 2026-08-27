import { Terrain } from './Terrain.js';
/** 伯爵领（省份）。 */
export declare class County {
    id: number;
    name: string;
    terrain: Terrain;
    key: string;
    development: number;
    control: number;
    prosperity: number;
    culture: number;
    faith: number;
    ownerTitle: number;
    holder: number;
    fortLevel: number;
    buildings: string[];
    levies: number;
    tax: number;
    neighbors: number[];
    tradeRouteProtected: boolean;
    tradeRouteProtectionLevel: number;
    tradeRouteMaintenanceCost: number;
    hasPort: boolean;
    portLevel: number;
    portIncome: number;
    tradeRouteMaintenanceLevel: number;
    tradeRouteUpgradeCost: number;
    constructor(id: number, name: string, terrain: Terrain);
    static newCounty(countyId: number, name: string, terrain: Terrain): County;
    monthlyTax(): number;
    /** 升级贸易路线维护等级，返回升级成本；已达上限返回 0。 */
    upgradeTradeRouteMaintenance(): number;
    /** 贸易路线维护加成：每级减少 10% 维护成本。 */
    getTradeRouteMaintenanceBonus(): number;
    monthlyLevies(): number;
    upkeep(): number;
    /** 港口收入：按港口等级与省份发展度计算。 */
    calculatePortIncome(): number;
    /** 升级港口，返回升级成本；已达上限返回 0。 */
    upgradePort(): number;
}
