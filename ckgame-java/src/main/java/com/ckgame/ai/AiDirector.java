package com.ckgame.ai;

import com.ckgame.core.Constants;
import com.ckgame.core.balance.Balance;
import com.ckgame.core.stats.AttributeSet;
import com.ckgame.military.Army;
import com.ckgame.military.ArmyStatus;
import com.ckgame.military.UnitType;
import com.ckgame.military.War;
import com.ckgame.military.WarManager;
import com.ckgame.military.WarParticipant;
import com.ckgame.politics.CasusBelli;
import com.ckgame.politics.Claim;
import com.ckgame.politics.Diplomacy;
import com.ckgame.politics.DiplomacyFlags;
import com.ckgame.politics.Scheme;
import com.ckgame.politics.SchemeKind;
import com.ckgame.politics.SchemeManager;
import com.ckgame.world.Character;
import com.ckgame.world.County;
import com.ckgame.world.Title;
import com.ckgame.world.World;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * AI 导演：为所有统治者生成并执行月度行动。
 */
public final class AiDirector {
    private static final Random RNG = new Random();

    /** 为所有统治者生成月度行动列表。 */
    public static List<AiAction> monthlyActions(
            World world,
            WarManager wars,
            Diplomacy diplomacy,
            SchemeManager schemes,
            Set<Integer> skipIds) {
        Set<Integer> skip = skipIds != null ? skipIds : Collections.emptySet();
        List<AiAction> actions = new ArrayList<>();
        List<Character> rulers = world.rulers();
        for (Character ruler : rulers) {
            int rid = ruler.id;
            if (skip.contains(rid)) {
                continue;
            }
            PersonalityProfile profile = AiPersonality.profileOf(world, rid);
            boolean atWar = false;
            for (War w : wars.activeWars()) {
                if (w.involves(rid)) {
                    atWar = true;
                    break;
                }
            }
            Character c = world.character(rid);
            if (c == null) {
                continue;
            }

            // 婚姻
            if (!c.isMarried() && c.isAdult(world.date) && RNG.nextDouble() < 0.35) {
                Integer spouse = findSpouseCandidate(world, rid);
                if (spouse != null) {
                    actions.add(new AiAction("marry", rid, spouse));
                }
            }
            for (int childId : c.children) {
                Character ch = world.character(childId);
                if (ch != null
                        && ch.isAlive()
                        && ch.isAdult(world.date)
                        && !ch.isMarried()
                        && RNG.nextDouble() < 0.2) {
                    Integer spouse = findSpouseCandidate(world, childId);
                    if (spouse != null) {
                        actions.add(new AiAction("marry", childId, spouse));
                    }
                }
            }

            // 战争调度
            if (atWar) {
                Integer enemy = null;
                for (War w : wars.activeWars()) {
                    if (w.involves(rid)) {
                        enemy = w.isAttacker(rid) ? w.defenderPrimary : w.attackerPrimary;
                        break;
                    }
                }
                if (enemy != null) {
                    if (wars.armiesOf(rid).isEmpty()) {
                        Integer loc = capitalOf(world, rid);
                        if (loc != null) {
                            actions.add(new AiAction("raise_army", rid, loc,
                                    estimateLocalLevies(world, rid)));
                        }
                    } else {
                        Integer targetLoc = capitalOf(world, enemy);
                        if (targetLoc != null) {
                            for (int aid : wars.armiesOf(rid)) {
                                Army army = wars.army(aid);
                                if (army != null
                                        && army.status == ArmyStatus.IDLE
                                        && army.location != targetLoc) {
                                    actions.add(new AiAction("move_army", rid, 0, 0, aid, targetLoc, 0, 0.0, null, null));
                                }
                            }
                        }
                    }
                }
            }

            // 宣战
            if (!atWar
                    && profile.aggression > 0.55
                    && RNG.nextDouble() < profile.aggression * 0.12) {
                Integer target = findWarTarget(world, wars, diplomacy, rid, skip);
                if (target != null && !skip.contains(target)) {
                    CasusBelli cb;
                    if (diplomacy.flags(rid, target).rival) {
                        cb = CasusBelli.RIVALRY;
                    } else if (!diplomacy.claimsOf(rid).isEmpty()) {
                        cb = CasusBelli.CLAIM;
                    } else {
                        cb = CasusBelli.CONQUEST;
                    }
                    actions.add(new AiAction("declare_war", rid, target, cb));
                    Integer loc = capitalOf(world, rid);
                    if (loc != null) {
                        actions.add(new AiAction("raise_army", rid, loc,
                                estimateLocalLevies(world, rid)));
                    }
                }
            }

            // 同盟
            if (profile.honor > 0.55 && RNG.nextDouble() < 0.08) {
                Integer ally = findAllyCandidate(world, diplomacy, rid);
                if (ally != null) {
                    actions.add(new AiAction("form_alliance", rid, ally));
                }
            }

            // 送礼
            if (profile.compassion > 0.55 && RNG.nextDouble() < 0.1 && c.gold >= 30) {
                List<Integer> others = new ArrayList<>();
                for (Character r : rulers) {
                    if (r.id != rid) {
                        others.add(r.id);
                    }
                }
                if (!others.isEmpty()) {
                    actions.add(new AiAction("send_gift", rid,
                            others.get(RNG.nextInt(others.size())), 15.0));
                }
            }

            // 阴谋
            if (profile.honor < 0.45 && RNG.nextDouble() < 0.12) {
                boolean already = false;
                Collection<Scheme> allSchemes = schemes.schemes();
                for (Scheme s : allSchemes) {
                    if (s.owner == rid) {
                        already = true;
                        break;
                    }
                }
                if (!already) {
                    Integer target = findSchemeTarget(world, rid);
                    if (target != null) {
                        SchemeKind kind;
                        if (profile.aggression > 0.6) {
                            kind = SchemeKind.MURDER;
                        } else if (RNG.nextDouble() < 0.5) {
                            kind = SchemeKind.SWAY;
                        } else {
                            kind = SchemeKind.FABRICATE_HOOK;
                        }
                        actions.add(new AiAction("start_scheme", rid, target, kind));
                    }
                }
            }

            if (RNG.nextDouble() < 0.1 && c.gold >= 20) {
                actions.add(new AiAction("hold_feast", rid));
            }

            if (profile.compassion > 0.45 && RNG.nextDouble() < 0.15) {
                List<Integer> others = new ArrayList<>();
                for (Character r : rulers) {
                    if (r.id != rid) {
                        others.add(r.id);
                    }
                }
                if (!others.isEmpty()) {
                    actions.add(new AiAction("improve_relations", rid,
                            others.get(RNG.nextInt(others.size()))));
                }
            }

            if (profile.greed > 0.5 && RNG.nextDouble() < 0.1) {
                actions.add(new AiAction("develop", rid));
            }

            if (atWar && RNG.nextDouble() < 0.15 && c.gold >= 25) {
                for (int aid : wars.armiesOf(rid)) {
                    actions.add(new AiAction("recruit_knights", rid, 0, 0, aid, 0, 0, 0.0, null, null));
                }
            }
        }
        return actions;
    }

    /** 执行 AI 行动列表。 */
    public static void applyActions(
            World world,
            WarManager wars,
            Diplomacy diplomacy,
            SchemeManager schemes,
            List<AiAction> actions) {
        for (AiAction act : actions) {
            switch (act.kind) {
                case "raise_army" -> {
                    if (!wars.armiesOf(act.owner).isEmpty()) {
                        continue;
                    }
                    int aid = wars.raiseArmy(act.owner, act.location, act.levies);
                    Army army = wars.army(aid);
                    if (army != null) {
                        army.addMen(UnitType.HEAVY_INFANTRY, act.levies / 10);
                        army.addMen(UnitType.ARCHERS, act.levies / 12);
                        army.addMen(UnitType.LIGHT_CAVALRY, act.levies / 20);
                    }
                    Character c = world.character(act.owner);
                    if (c != null) {
                        world.pushLog(c.name + " 征召了 " + act.levies + " 人 (军团 " + aid + ")");
                    }
                }
                case "move_army" -> {
                    Army army = wars.army(act.armyId);
                    if (army == null) {
                        continue;
                    }
                    List<Integer> path = world.map.path(army.location, act.destination);
                    if (path != null) {
                        army.setPath(path);
                        County dest = world.map.get(act.destination);
                        String dname = dest != null ? dest.name : "?";
                        world.pushLog(army.name + " 向 " + dname + " 进军");
                    }
                }
                case "declare_war" -> {
                    if (act.cb == null) {
                        continue;
                    }
                    if (!diplomacy.canDeclareWar(act.owner, act.target, world.date.year())) {
                        continue;
                    }
                    if (diplomacy.areAllied(act.owner, act.target)) {
                        continue;
                    }
                    boolean any = false;
                    for (War w : wars.activeWars()) {
                        if (w.involves(act.owner) || w.involves(act.target)) {
                            any = true;
                            break;
                        }
                    }
                    if (any) {
                        continue;
                    }
                    Character attacker = world.character(act.owner);
                    if (attacker == null || attacker.prestige < act.cb.prestigeCost()) {
                        continue;
                    }
                    attacker.addPrestige(-act.cb.prestigeCost());
                    String an = attacker.name;
                    Character def = world.character(act.target);
                    String dn = def != null ? def.name : "?";
                    int wid = wars.declareWar(
                            act.cb,
                            act.owner,
                            act.target,
                            world.date,
                            an + " 对 " + dn + " 的" + act.cb.nameZh()
                    );
                    diplomacy.setAtWar(act.owner, act.target, true);
                    diplomacy.addWarExhaustion(act.owner);
                    diplomacy.addWarExhaustion(act.target);
                    War war = wars.war(wid);
                    if (war != null) {
                        for (int ally : diplomacy.alliesOf(act.owner)) {
                            if (ally != act.target && !war.involves(ally)) {
                                war.participants.add(new WarParticipant(ally, true, world.date));
                                diplomacy.setAtWar(ally, act.target, true);
                            }
                        }
                    }
                    world.pushLog("宣战！" + act.cb.nameZh() + " (战争 " + wid + ")");
                }
                case "marry" -> {
                    if (world.marry(act.owner, act.target)) {
                        diplomacy.flagsMut(act.owner, act.target).marriagePact = true;
                    }
                }
                case "improve_relations" -> {
                    world.modifyOpinion(act.owner, act.target, 10);
                    world.modifyOpinion(act.target, act.owner, 5);
                }
                case "form_alliance" -> {
                    if (diplomacy.flags(act.owner, act.target).allied) {
                        continue;
                    }
                    diplomacy.formAlliance(act.owner, act.target, world.date);
                    world.modifyOpinion(act.owner, act.target, 20);
                    world.modifyOpinion(act.target, act.owner, 20);
                    Character a = world.character(act.owner);
                    Character b = world.character(act.target);
                    if (a != null && b != null) {
                        world.pushLog(a.name + " 与 " + b.name + " 结成同盟");
                    }
                }
                case "send_gift" -> {
                    Character c = world.character(act.owner);
                    if (c != null && c.gold >= act.amount) {
                        c.addGold(-act.amount);
                        int gain = Diplomacy.giftOpinionGain(act.amount);
                        world.modifyOpinion(act.target, act.owner, gain);
                        Character t = world.character(act.target);
                        if (t != null) {
                            world.pushLog(String.format("%s 向 %s 赠礼 %.0f 金", c.name, t.name, act.amount));
                        }
                    }
                }
                case "start_scheme" -> {
                    if (act.schemeKind == null) {
                        continue;
                    }
                    int sid = schemes.start(act.schemeKind, act.owner, act.target, world.date);
                    Character o = world.character(act.owner);
                    Character t = world.character(act.target);
                    if (o != null && t != null) {
                        world.pushLog(o.name + " 对 " + t.name + " 启动阴谋「"
                                + act.schemeKind.nameZh() + "」( #" + sid + ")");
                    }
                }
                case "hold_feast" -> {
                    Character c = world.character(act.owner);
                    if (c != null && c.gold >= 20) {
                        c.addGold(-20);
                        c.addPrestige(15);
                        c.addStress(-10);
                        world.pushLog(c.name + " 举办了宴会");
                    }
                }
                case "develop" -> {
                    Character c = world.character(act.owner);
                    if (c != null && c.gold >= 10) {
                        c.addGold(-10);
                        c.addPrestige(5);
                    }
                    Integer cap = capitalOf(world, act.owner);
                    if (cap != null) {
                        County county = world.map.get(cap);
                        if (county != null && county.development < county.terrain.developmentCap()) {
                            county.development += 1;
                            world.pushLog("领地 " + county.name + " 发展度提升");
                        }
                    }
                }
                case "recruit_knights" -> {
                    Character c = world.character(act.owner);
                    Army army = wars.army(act.armyId);
                    if (c != null && army != null && c.gold >= 25) {
                        c.addGold(-25);
                        army.addMen(UnitType.HEAVY_CAVALRY, 40);
                        army.addMen(UnitType.HEAVY_INFANTRY, 80);
                        world.pushLog(c.name + " 为军团补充了精锐");
                    }
                }
                default -> {
                    // 未知动作类型，忽略
                }
            }
        }
    }

    /** 寻找最佳配偶候选人（异性、成年、未婚、年龄<40、非近亲）。 */
    public static Integer findSpouseCandidate(World world, int who) {
        Character c = world.character(who);
        if (c == null) {
            return null;
        }
        var need = c.gender == com.ckgame.core.Gender.MALE
                ? com.ckgame.core.Gender.FEMALE
                : com.ckgame.core.Gender.MALE;
        List<int[]> candidates = new ArrayList<>();
        for (Character o : world.aliveCharacters()) {
            if (o.id != who
                    && o.gender == need
                    && !o.isMarried()
                    && o.isAdult(world.date)
                    && o.ageAt(world.date) < 40
                    && o.dynasty != c.dynasty) {
                AttributeSet attrs = world.effectiveAttrs(o.id);
                int score = attrs != null ? attrs.total() : 0;
                candidates.add(new int[]{score, o.id});
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        candidates.sort((a, b) -> Integer.compare(b[0], a[0]));
        return candidates.get(0)[1];
    }

    /** 寻找战争目标：比自己弱、非同盟/非停战、不在战争中的统治者。 */
    public static Integer findWarTarget(
            World world,
            WarManager wars,
            Diplomacy diplomacy,
            int who,
            Set<Integer> skipIds) {
        Set<Integer> skip = skipIds != null ? skipIds : Collections.emptySet();
        int myPower = estimateLevies(world, who) + wars.totalMenOf(who);
        Integer best = null;
        double bestP = 1e9;
        for (Character r : world.rulers()) {
            if (r.id == who || skip.contains(r.id)) {
                continue;
            }
            if (!diplomacy.canDeclareWar(who, r.id, world.date.year())) {
                continue;
            }
            if (diplomacy.areAllied(who, r.id)) {
                continue;
            }
            boolean atWar = false;
            for (War w : wars.activeWars()) {
                if (w.involves(r.id)) {
                    atWar = true;
                    break;
                }
            }
            if (atWar) {
                continue;
            }
            int their = estimateLevies(world, r.id);
            if (myPower > their * 0.85 && their < bestP) {
                bestP = their;
                best = r.id;
            }
        }
        return best;
    }

    /** 寻找最佳同盟候选人（关系最好且未结盟/未交战）。 */
    public static Integer findAllyCandidate(World world, Diplomacy diplomacy, int who) {
        Integer best = null;
        int bestOp = -999;
        for (Character r : world.rulers()) {
            if (r.id == who) {
                continue;
            }
            DiplomacyFlags f = diplomacy.flags(who, r.id);
            if (f.allied || f.atWar) {
                continue;
            }
            int op = world.opinion(who, r.id);
            if (op >= 0 && op > bestOp) {
                bestOp = op;
                best = r.id;
            }
        }
        return best;
    }

    /** 寻找阴谋目标：关系最差的统治者或富裕目标。 */
    public static Integer findSchemeTarget(World world, int who) {
        Integer best = null;
        int bestOp = 999;
        for (Character c : world.aliveCharacters()) {
            if (c.id == who || !c.isAdult(world.date)) {
                continue;
            }
            if (!(c.isRuler || c.gold > 80)) {
                continue;
            }
            int op = world.opinion(who, c.id);
            if (op < bestOp) {
                bestOp = op;
                best = c.id;
            }
        }
        return best;
    }

    /** 返回角色的首都省份 ID。 */
    public static Integer capitalOf(World world, int who) {
        Character c = world.character(who);
        if (c == null) {
            return null;
        }
        Title t = world.title(c.primaryTitle);
        if (t == null) {
            return null;
        }
        if (t.capital != Constants.NONE_ID) {
            return t.capital;
        }
        return t.counties.isEmpty() ? null : t.counties.get(0);
    }

    /** 估算首都郡的征召兵力（3 个月量）。 */
    public static int estimateLocalLevies(World world, int who) {
        Integer loc = capitalOf(world, who);
        if (loc == null) {
            return estimateLevies(world, who);
        }
        County county = world.map.get(loc);
        if (county == null) {
            return estimateLevies(world, who);
        }
        return Math.max(100, county.monthlyLevies() * 3);
    }

    /** 估算角色全部领地的月度征召兵力总和。 */
    public static int estimateLevies(World world, int who) {
        Character c = world.character(who);
        if (c == null) {
            return 0;
        }
        Set<Integer> seen = new HashSet<>();
        int total = 0;
        for (int tid : c.heldTitles) {
            Title t = world.title(tid);
            if (t == null) {
                continue;
            }
            for (int cid : t.counties) {
                if (!seen.add(cid)) {
                    continue;
                }
                County county = world.map.get(cid);
                if (county != null) {
                    total += county.monthlyLevies();
                }
            }
        }
        return Math.max(100, total);
    }
}
