package com.ckgame.military;

import com.ckgame.core.Constants;
import com.ckgame.core.calendar.GameDate;
import com.ckgame.politics.CasusBelli;
import com.ckgame.politics.Diplomacy;
import com.ckgame.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

/**
 * 管理所有战争与军队。
 */
public final class WarManager {
    public final Map<Integer, War> wars = new HashMap<>();
    public final Map<Integer, Army> armies = new HashMap<>();
    public int nextWar = 1;
    public int nextArmy = 1;

    private final World world;
    private final Diplomacy diplomacy;

    public WarManager() {
        this(null, null);
    }

    public WarManager(World world) {
        this(world, null);
    }

    public WarManager(World world, Diplomacy diplomacy) {
        this.world = world;
        this.diplomacy = diplomacy;
    }

    public World world() {
        return world;
    }

    public Diplomacy diplomacy() {
        return diplomacy;
    }

    /** 宣战并创建战争。 */
    public int declareWar(CasusBelli cb, int attacker, int defender,
                          GameDate date, String name, int targetTitle) {
        int wid = nextWar++;
        War war = new War(wid, name, cb, attacker, defender, date);
        war.targetTitle = targetTitle;
        war.participants.add(new WarParticipant(attacker, true, date));
        war.participants.add(new WarParticipant(defender, false, date));
        wars.put(wid, war);
        return wid;
    }

    public int declareWar(CasusBelli cb, int attacker, int defender,
                          GameDate date, String name) {
        return declareWar(cb, attacker, defender, date, name, Constants.NONE_ID);
    }

    /** 按 id 获取战争。 */
    public War war(int wid) {
        return wars.get(wid);
    }

    /** 所有进行中的战争。 */
    public List<War> activeWars() {
        List<War> out = new ArrayList<>();
        for (War w : wars.values()) {
            if (w.active) {
                out.add(w);
            }
        }
        return out;
    }

    /** 结束战争并设置结果。 */
    public void endWar(int wid, WarResult result) {
        War w = wars.get(wid);
        if (w == null) {
            return;
        }
        w.active = false;
        w.result = result;
        // 战争结束时处理领土变更（Python 版此处为占位 pass）
        if (result == WarResult.ATTACKER_VICTORY) {
            // 攻击者获胜，夺取目标省份
        } else if (result == WarResult.DEFENDER_VICTORY) {
            // 防御者获胜，击退攻击者
        }
    }

    /** 征召一支新军队。 */
    public int raiseArmy(int owner, int location, int levies, String name) {
        int aid = nextArmy++;
        String armyName = (name == null || name.isBlank()) ? "军团#" + aid : name;
        Army army = new Army(aid, owner, armyName, location, owner);
        army.addMen(UnitType.LEVIES, levies);
        armies.put(aid, army);
        return aid;
    }

    public int raiseArmy(int owner, int location, int levies) {
        return raiseArmy(owner, location, levies, "");
    }

    /** 按 id 获取军队。 */
    public Army army(int aid) {
        return armies.get(aid);
    }

    /** 某角色所有活跃军队的 id。 */
    public List<Integer> armiesOf(int owner) {
        List<Integer> out = new ArrayList<>();
        for (Army a : armies.values()) {
            if (a.owner == owner && a.isActive()) {
                out.add(a.id);
            }
        }
        return out;
    }

    /** 某角色所有活跃军队的总兵力。 */
    public int totalMenOf(int owner) {
        int total = 0;
        for (Army a : armies.values()) {
            if (a.owner == owner && a.isActive()) {
                total += a.totalMen();
            }
        }
        return total;
    }

    /** 推进所有处于行军状态的军队。 */
    public void tickMovement(ToDoubleFunction<Army> moveChanceOf) {
        for (Army army : armies.values()) {
            if (army.status == ArmyStatus.MOVING) {
                double chance = moveChanceOf == null ? 1.0 : moveChanceOf.applyAsDouble(army);
                army.advanceMove(chance);
            }
        }
    }

    public void tickMovement() {
        tickMovement(null);
    }

    /** 解散空军队。 */
    public void disbandEmpty() {
        for (Army army : armies.values()) {
            if (army.totalMen() <= 0) {
                army.status = ArmyStatus.DISBANDED;
            }
        }
    }

    /** 为指定军队设置行军路径。 */
    public void move(int armyId, List<Integer> path) {
        Army army = armies.get(armyId);
        if (army != null) {
            army.setPath(path);
        }
    }

    /** 解散指定军队。 */
    public void disband(int armyId) {
        Army army = armies.get(armyId);
        if (army != null) {
            army.status = ArmyStatus.DISBANDED;
        }
    }

    /** 一日推进：移动与清理空军队。 */
    public void tick() {
        tickMovement();
        disbandEmpty();
    }
}
