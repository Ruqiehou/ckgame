import { NONE_ID } from '../core/Constants.js';
import { terrainSupplyLimit } from './Terrain.js';
/** 伯爵领（省份）。 */
export class County {
    id;
    name;
    terrain;
    key = '';
    development = 10;
    control = 100;
    prosperity = 50;
    culture = 0;
    faith = 0;
    ownerTitle = NONE_ID;
    holder = NONE_ID;
    fortLevel = 1;
    buildings = [];
    levies = 200;
    tax = 1;
    neighbors = [];
    // 贸易扩展
    tradeRouteProtected = false;
    tradeRouteProtectionLevel = 0;
    tradeRouteMaintenanceCost = 0;
    hasPort = false;
    portLevel = 0;
    portIncome = 0;
    tradeRouteMaintenanceLevel = 0;
    tradeRouteUpgradeCost = 0;
    constructor(id, name, terrain) {
        this.id = id;
        this.name = name;
        this.terrain = terrain;
    }
    static newCounty(countyId, name, terrain) {
        return new County(countyId, name, terrain);
    }
    monthlyTax() {
        const base = this.tax * (1.0 + this.development * 0.02);
        return base * (this.control / 100.0) * terrainSupplyLimit(this.terrain);
    }
    /** 升级贸易路线维护等级，返回升级成本；已达上限返回 0。 */
    upgradeTradeRouteMaintenance() {
        if (this.tradeRouteMaintenanceLevel >= 5) {
            return 0;
        }
        this.tradeRouteMaintenanceLevel += 1;
        this.tradeRouteUpgradeCost = 100.0 * this.tradeRouteMaintenanceLevel;
        this.tradeRouteMaintenanceCost = 10.0 * this.tradeRouteMaintenanceLevel;
        return this.tradeRouteUpgradeCost;
    }
    /** 贸易路线维护加成：每级减少 10% 维护成本。 */
    getTradeRouteMaintenanceBonus() {
        return 1.0 - (this.tradeRouteMaintenanceLevel * 0.1);
    }
    monthlyLevies() {
        const base = this.levies * (1.0 + this.development * 0.01);
        return Math.floor(base * (this.control / 100.0));
    }
    upkeep() {
        let cost = 0;
        for (const b of this.buildings) {
            if (b === '城堡')
                cost += 0.5;
            else if (b === '市场')
                cost += 0.3;
            else if (b === '庄园')
                cost += 0.2;
        }
        return cost;
    }
    /** 港口收入：按港口等级与省份发展度计算。 */
    calculatePortIncome() {
        if (!this.hasPort)
            return 0;
        const baseIncome = 50.0 * this.portLevel;
        const developmentBonus = 1.0 + (this.development / 100.0);
        return baseIncome * developmentBonus;
    }
    /** 升级港口，返回升级成本；已达上限返回 0。 */
    upgradePort() {
        if (this.portLevel >= 5) {
            return 0;
        }
        this.portLevel += 1;
        const upgradeCost = 200.0 * this.portLevel;
        this.portIncome = this.calculatePortIncome();
        return upgradeCost;
    }
}
//# sourceMappingURL=County.js.map