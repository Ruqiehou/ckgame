package com.ckgame.politics;

import com.ckgame.core.balance.Balance;
import com.ckgame.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleSupplier;

/**
 * 派系管理器。
 * 对应 Python 的 FactionSystem。
 */
public final class FactionManager {
    private final World world;
    private final Map<Integer, Faction> factions = new HashMap<>();
    private int nextId = 1;

    public FactionManager(World world) {
        this.world = world;
    }

    public World world() {
        return world;
    }

    /** 返回派系映射（只读视图）。 */
    public Map<Integer, Faction> factions() {
        return Collections.unmodifiableMap(factions);
    }

    /** 创建新派系并返回其 ID。 */
    public int create(FactionKind kind, int liege, Integer claimant) {
        int fid = nextId;
        nextId++;
        factions.put(fid, new Faction(fid, kind, liege, claimant));
        return fid;
    }

    /** 角色加入派系。 */
    public void join(int factionId, int who) {
        Faction f = factions.get(factionId);
        if (f != null) {
            f.addMember(who);
        }
    }

    /** 角色离开派系。 */
    public void leave(int factionId, int who) {
        Faction f = factions.get(factionId);
        if (f != null) {
            f.removeMember(who);
        }
    }

    /** 解散派系。 */
    public void dissolve(int factionId) {
        factions.remove(factionId);
    }

    /**
     * 君主安抚：降低不满；过低则解散。
     *
     * @param amount 若传入 null，使用默认安抚值
     */
    public boolean appease(int factionId, Double amount) {
        double delta = amount != null ? amount : Balance.FACTION_APPEASE_DEFAULT;
        Faction f = factions.get(factionId);
        if (f == null) {
            return false;
        }
        f.discontent = Math.max(0.0, f.discontent - delta);
        f.ultimatumSent = false;
        if (f.discontent <= Balance.FACTION_DISSOLVE_DISCONTENT && f.power < Balance.FACTION_DISSOLVE_POWER) {
            dissolve(factionId);
        }
        return true;
    }

    /** 查找目标领主下指定类型的派系。 */
    public Integer findForLiege(int liege, FactionKind kind) {
        for (Faction f : factions.values()) {
            if (f.targetLiege == liege && f.kind == kind) {
                return f.id;
            }
        }
        return null;
    }

    /** 根据军事实力重新计算各派系力量。 */
    public void recomputePower(Map<Integer, Double> militaryPower, Map<Integer, Double> liegePower) {
        for (Faction f : factions.values()) {
            double memberPower = 0.0;
            for (int m : f.members) {
                memberPower += militaryPower.getOrDefault(m, 10.0);
            }
            double lp = Math.max(1.0, liegePower.getOrDefault(f.targetLiege, 100.0));
            f.power = Math.min(200.0, memberPower / lp * 100.0);
        }
    }

    /** 根据对领主的好感度每月调整不满值。 */
    public void tickDiscontent(Map<Integer, Integer> opinionOfLiege) {
        for (Faction f : factions.values()) {
            double avg;
            if (f.members.isEmpty()) {
                avg = 0.0;
            } else {
                double sum = 0.0;
                for (int m : f.members) {
                    sum += opinionOfLiege.getOrDefault(m, 0);
                }
                avg = sum / f.members.size();
            }
            if (avg < 0) {
                f.discontent = Math.min(100.0, f.discontent + (-avg) * 0.15);
            } else {
                f.discontent = Math.max(0.0, f.discontent - avg * 0.08);
            }
            if (f.members.size() < 2) {
                f.discontent = Math.max(0.0, f.discontent - 5.0);
            }
        }
    }

    /**
     * 派系月度 AI。
     *
     * @param vassals  三元组列表，每个 int[] 为 {vassal, liege, opinion}
     * @param rngRoll  返回 0~1 随机数的函数
     */
    public List<FactionEvent> monthlyAi(List<int[]> vassals, DoubleSupplier rngRoll) {
        List<FactionEvent> events = new ArrayList<>();
        for (int[] row : vassals) {
            int vassal = row[0];
            int liege = row[1];
            int opinion = row[2];
            if (opinion > -10) {
                List<Integer> leaveIds = new ArrayList<>();
                for (Faction f : factions.values()) {
                    if (f.targetLiege == liege && f.members.contains(vassal)) {
                        leaveIds.add(f.id);
                    }
                }
                for (int fid : leaveIds) {
                    leave(fid, vassal);
                }
                continue;
            }
            if (rngRoll.getAsDouble() > 0.25) {
                continue;
            }
            FactionKind kind;
            if (opinion < -50) {
                kind = FactionKind.INDEPENDENCE;
            } else if (opinion < -30) {
                kind = FactionKind.LOWER_CROWN_AUTHORITY;
            } else {
                kind = FactionKind.LIBERTY;
            }
            Integer fid = findForLiege(liege, kind);
            if (fid != null) {
                Faction f = factions.get(fid);
                if (!f.members.contains(vassal)) {
                    join(fid, vassal);
                    events.add(newEvent("joined", fid, liege, 0, vassal, null, null, null));
                }
            } else if (rngRoll.getAsDouble() < 0.4) {
                fid = create(kind, liege, null);
                join(fid, vassal);
                events.add(newEvent("formed", fid, liege, vassal, 0, kind, null, null));
            }
        }

        List<Integer> empty = new ArrayList<>();
        for (Faction f : new ArrayList<>(factions.values())) {
            if (f.members.isEmpty()) {
                empty.add(f.id);
            }
        }
        for (int fid : empty) {
            dissolve(fid);
            events.add(newEvent("dissolved", fid, 0, 0, 0, null, null, "无人支持"));
        }

        for (Faction f : new ArrayList<>(factions.values())) {
            if (f.canSendUltimatum()) {
                f.ultimatumSent = true;
                events.add(newEvent("ultimatum", f.id, f.targetLiege, 0, 0, f.kind, f.members, null));
            } else if (f.isReadyToRevolt()) {
                events.add(newEvent("revolt", f.id, f.targetLiege, 0, 0, f.kind, f.members, null));
            }
        }
        return events;
    }

    private FactionEvent newEvent(String kind, int factionId, int liege, int founder, int who,
                                  FactionKind factionKind, List<Integer> members, String reason) {
        return new FactionEvent(kind, factionId, liege, founder, who, factionKind, members, reason);
    }
}
