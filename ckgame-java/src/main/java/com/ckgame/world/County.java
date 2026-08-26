package com.ckgame.world;

import com.ckgame.core.Constants;

import java.util.ArrayList;
import java.util.List;

/** 伯爵领（省份）。 */
public final class County {
    public int id;
    public String name;
    public Terrain terrain;
    public String key = "";
    public int development = 10;
    public double control = 100.0;
    public double prosperity = 50.0;
    public int culture = 0;
    public int faith = 0;
    public int ownerTitle = Constants.NONE_ID;
    public int holder = Constants.NONE_ID;
    public int fortLevel = 1;
    public List<String> buildings = new ArrayList<>();
    public int levies = 200;
    public double tax = 1.0;
    public List<Integer> neighbors = new ArrayList<>();

    // 贸易扩展
    public boolean tradeRouteProtected = false;
    public int tradeRouteProtectionLevel = 0;
    public double tradeRouteMaintenanceCost = 0.0;
    public boolean hasPort = false;
    public int portLevel = 0;
    public double portIncome = 0.0;
    public int tradeRouteMaintenanceLevel = 0;
    public double tradeRouteUpgradeCost = 0.0;

    public County(int id, String name, Terrain terrain) {
        this.id = id;
        this.name = name;
        this.terrain = terrain;
    }

    public static County newCounty(int countyId, String name, Terrain terrain) {
        return new County(countyId, name, terrain);
    }

    public double monthlyTax() {
        double base = tax * (1.0 + development * 0.02);
        return base * (control / 100.0) * terrain.supplyLimit();
    }

    /** 升级贸易路线维护等级，返回升级成本；已达上限返回 0。 */
    public double upgradeTradeRouteMaintenance() {
        if (tradeRouteMaintenanceLevel >= 5) {
            return 0.0;
        }
        tradeRouteMaintenanceLevel += 1;
        tradeRouteUpgradeCost = 100.0 * tradeRouteMaintenanceLevel;
        tradeRouteMaintenanceCost = 10.0 * tradeRouteMaintenanceLevel;
        return tradeRouteUpgradeCost;
    }

    /** 贸易路线维护加成：每级减少 10% 维护成本。 */
    public double getTradeRouteMaintenanceBonus() {
        return 1.0 - (tradeRouteMaintenanceLevel * 0.1);
    }

    public int monthlyLevies() {
        double base = levies * (1.0 + development * 0.01);
        return (int) (base * (control / 100.0));
    }

    public double upkeep() {
        double cost = 0.0;
        for (String b : buildings) {
            if (b.equals("城堡")) cost += 0.5;
            else if (b.equals("市场")) cost += 0.3;
            else if (b.equals("庄园")) cost += 0.2;
        }
        return cost;
    }

    /** 港口收入：按港口等级与省份发展度计算。 */
    public double calculatePortIncome() {
        if (!hasPort) return 0.0;
        double baseIncome = 50.0 * portLevel;
        double developmentBonus = 1.0 + (development / 100.0);
        return baseIncome * developmentBonus;
    }

    /** 升级港口，返回升级成本；已达上限返回 0。 */
    public double upgradePort() {
        if (portLevel >= 5) {
            return 0.0;
        }
        portLevel += 1;
        double upgradeCost = 200.0 * portLevel;
        portIncome = calculatePortIncome();
        return upgradeCost;
    }
}
