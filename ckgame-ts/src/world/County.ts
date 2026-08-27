import { NONE_ID } from '../core/Constants.js';
import { Terrain, terrainSupplyLimit } from './Terrain.js';

/** 伯爵领（省份）。 */
export class County {
  id: number;
  name: string;
  terrain: Terrain;
  key: string = '';
  development: number = 10;
  control: number = 100;
  prosperity: number = 50;
  culture: number = 0;
  faith: number = 0;
  ownerTitle: number = NONE_ID;
  holder: number = NONE_ID;
  fortLevel: number = 1;
  buildings: string[] = [];
  levies: number = 200;
  tax: number = 1;
  neighbors: number[] = [];

  // 贸易扩展
  tradeRouteProtected: boolean = false;
  tradeRouteProtectionLevel: number = 0;
  tradeRouteMaintenanceCost: number = 0;
  hasPort: boolean = false;
  portLevel: number = 0;
  portIncome: number = 0;
  tradeRouteMaintenanceLevel: number = 0;
  tradeRouteUpgradeCost: number = 0;

  constructor(id: number, name: string, terrain: Terrain) {
    this.id = id;
    this.name = name;
    this.terrain = terrain;
  }

  static newCounty(countyId: number, name: string, terrain: Terrain): County {
    return new County(countyId, name, terrain);
  }

  monthlyTax(): number {
    const base = this.tax * (1.0 + this.development * 0.02);
    return base * (this.control / 100.0) * terrainSupplyLimit(this.terrain);
  }

  /** 升级贸易路线维护等级，返回升级成本；已达上限返回 0。 */
  upgradeTradeRouteMaintenance(): number {
    if (this.tradeRouteMaintenanceLevel >= 5) {
      return 0;
    }
    this.tradeRouteMaintenanceLevel += 1;
    this.tradeRouteUpgradeCost = 100.0 * this.tradeRouteMaintenanceLevel;
    this.tradeRouteMaintenanceCost = 10.0 * this.tradeRouteMaintenanceLevel;
    return this.tradeRouteUpgradeCost;
  }

  /** 贸易路线维护加成：每级减少 10% 维护成本。 */
  getTradeRouteMaintenanceBonus(): number {
    return 1.0 - (this.tradeRouteMaintenanceLevel * 0.1);
  }

  monthlyLevies(): number {
    const base = this.levies * (1.0 + this.development * 0.01);
    return Math.floor(base * (this.control / 100.0));
  }

  upkeep(): number {
    let cost = 0;
    for (const b of this.buildings) {
      if (b === '城堡') cost += 0.5;
      else if (b === '市场') cost += 0.3;
      else if (b === '庄园') cost += 0.2;
    }
    return cost;
  }

  /** 港口收入：按港口等级与省份发展度计算。 */
  calculatePortIncome(): number {
    if (!this.hasPort) return 0;
    const baseIncome = 50.0 * this.portLevel;
    const developmentBonus = 1.0 + (this.development / 100.0);
    return baseIncome * developmentBonus;
  }

  /** 升级港口，返回升级成本；已达上限返回 0。 */
  upgradePort(): number {
    if (this.portLevel >= 5) {
      return 0;
    }
    this.portLevel += 1;
    const upgradeCost = 200.0 * this.portLevel;
    this.portIncome = this.calculatePortIncome();
    return upgradeCost;
  }
}
