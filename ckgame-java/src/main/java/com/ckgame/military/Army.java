package com.ckgame.military;

import java.util.ArrayList;
import java.util.List;

public final class Army {
    public int id;
    public int owner;
    public int commander;
    public String name;
    public int location;
    public ArmyStatus status = ArmyStatus.IDLE;
    public final List<UnitStack> stacks = new ArrayList<>();
    public final List<Integer> path = new ArrayList<>();
    public int supply = 100;
    public int morale = 100;

    public Army(int id, int owner, String name, int location, int commander) {
        this.id = id;
        this.owner = owner;
        this.name = name;
        this.location = location;
        this.commander = commander;
    }

    public boolean isActive() {
        return status != ArmyStatus.DISBANDED && totalMen() > 0;
    }

    public int totalMen() {
        int total = 0;
        for (UnitStack stack : stacks) {
            total += Math.max(0, stack.men);
        }
        return total;
    }

    public void addMen(UnitType unitType, int men) {
        if (unitType == null || men <= 0) {
            return;
        }
        for (UnitStack stack : stacks) {
            if (stack.unitType == unitType) {
                stack.men += men;
                stack.maxMen += men;
                return;
            }
        }
        stacks.add(new UnitStack(unitType, men, men));
    }

    public double monthlyMaintenance() {
        double total = 0.0;
        for (UnitStack stack : stacks) {
            total += stack.men * stack.unitType.maintenance();
        }
        return total;
    }

    public void setPath(List<Integer> newPath) {
        path.clear();
        if (newPath == null || newPath.isEmpty()) {
            status = ArmyStatus.IDLE;
            return;
        }
        path.addAll(newPath);
        if (!path.isEmpty() && path.get(0) == location) {
            path.remove(0);
        }
        status = path.isEmpty() ? ArmyStatus.IDLE : ArmyStatus.MOVING;
    }

    public void advanceMove(double moveChance) {
        if ((status != ArmyStatus.MOVING && status != ArmyStatus.RETREATING) || path.isEmpty()) {
            return;
        }
        if (moveChance < 1.0 && Math.random() > Math.max(0.0, moveChance)) {
            return;
        }
        location = path.remove(0);
        if (status == ArmyStatus.RETREATING && !path.isEmpty()) {
            return;
        }
        status = path.isEmpty() ? ArmyStatus.IDLE : ArmyStatus.MOVING;
    }

    public void applySupplyTick(boolean inFriendlyCounty, boolean winter) {
        int delta = inFriendlyCounty ? 5 : -8;
        if (winter) {
            delta -= 4;
        }
        supply = Math.max(0, Math.min(150, supply + delta));
        if (supply < 30) {
            morale = Math.max(10, morale - 3);
        } else if (supply > 80) {
            morale = Math.min(100, morale + 1);
        }
        if (totalMen() <= 0) {
            status = ArmyStatus.DISBANDED;
        }
    }
}
