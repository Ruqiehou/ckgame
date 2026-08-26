package com.ckgame.military;

import com.ckgame.core.Constants;
import com.ckgame.world.Character;
import com.ckgame.world.County;
import com.ckgame.world.Title;

import java.util.ArrayList;
import java.util.List;

/** 军团：军队实体，包含部队和征兵。 */
public final class Army {
    public int id;
    public int commander;
    public int size = 0;
    public String name = "";
    public int morale = 60;
    public int discipline = 50;
    public int training = 0;
    public List<Integer> troops = new ArrayList<>(); // troop types
    public int siegeProgress = 0;
    public int siegeTarget = 0;
    public boolean isSieging = false;
    public int supply = 100;

    public Army(int id, int commander, String name) {
        this.id = id;
        this.commander = commander;
        this.name = name;
    }

    public static Army newArmy(int armyId, int commander, String name) {
        Army a = new Army(armyId, commander, name);
        return a;
    }

    public void addTroop(int type, int count) {
        // type 0=levy, 1=knight, 2=archer, etc.
        troops.add(type);
        size += count;
    }

    public void moveTo(int countyId) {
        // 移动逻辑
        this.supply = Math.max(0, supply - 5);
    }

    public boolean canEngage() {
        return size > 0 && morale > 0;
    }

    public void updateMorale() {
        if (supply < 50) morale -= 2;
        else if (supply > 150) morale += 1;
    }
}
