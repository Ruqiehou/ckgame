package com.ckgame.events;

import com.ckgame.world.Character;
import com.ckgame.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 事件引擎：维护事件目录、冷却、待处理队列与历史记录。
 */
public final class EventEngine {
    public final List<EventDef> catalog;
    public final List<EventInstance> pending = new ArrayList<>();
    private final Map<CooldownKey, Integer> cooldowns = new HashMap<>();
    public final List<String> history = new ArrayList<>();
    private final Random rng = new Random();

    public EventEngine() {
        this.catalog = builtinEvents();
    }

    public EventEngine(List<EventDef> catalog) {
        this.catalog = catalog != null && !catalog.isEmpty() ? catalog : builtinEvents();
    }

    /** 所有冷却减一天，移除到期的条目。 */
    public void tickCooldowns() {
        Map<CooldownKey, Integer> next = new HashMap<>();
        for (Map.Entry<CooldownKey, Integer> e : cooldowns.entrySet()) {
            int v = e.getValue() - 1;
            if (v > 0) {
                next.put(e.getKey(), v);
            }
        }
        cooldowns.clear();
        cooldowns.putAll(next);
    }

    private boolean eligible(World world, int who, EventDef ev) {
        Character c = world.character(who);
        if (c == null || !c.isAlive()) {
            return false;
        }
        if (ev.requiresRuler && !c.isRuler) {
            return false;
        }
        if (ev.requiresAdult && !c.isAdult(world.date)) {
            return false;
        }
        if (c.gold < ev.minGold) {
            return false;
        }
        if (ev.requiresMarried && !c.isMarried()) {
            return false;
        }
        return true;
    }

    /** 每日检查，可能为每个角色生成事件。 */
    public void dailyCheck(World world, List<Integer> characters) {
        for (int who : characters) {
            for (EventDef ev : catalog) {
                CooldownKey key = new CooldownKey(ev.id, who);
                if (cooldowns.containsKey(key)) {
                    continue;
                }
                if (!eligible(world, who, ev)) {
                    continue;
                }
                double p = Math.max(0.005, Math.min(0.35, ev.weight * 0.02));
                if (rng.nextDouble() < p) {
                    pending.add(new EventInstance(
                            ev.id,
                            who,
                            ev.title,
                            ev.description,
                            new ArrayList<>(ev.choices)
                    ));
                    cooldowns.put(key, ev.cooldownDays);
                    if (ev.major) {
                        break;
                    }
                }
            }
        }
    }

    /** 根据选择 ID 结算事件。 */
    public void resolveChoice(World world, EventInstance instance, int choiceId) {
        EventChoice choice = null;
        for (EventChoice c : instance.choices) {
            if (c.id == choiceId) {
                choice = c;
                break;
            }
        }
        if (choice == null) {
            return;
        }
        for (Effect eff : choice.effects) {
            eff.apply(world, instance.character);
        }
        Character c = world.character(instance.character);
        String name = c != null ? c.name : "?";
        String line = name + " 事件「" + instance.title + "」选择：" + choice.text;
        history.add(line);
        world.pushLog(line);
    }

    /** AI 自动按最高权重选择并清空待处理队列。 */
    public void autoResolveAll(World world) {
        List<EventInstance> toResolve = new ArrayList<>(pending);
        pending.clear();
        for (EventInstance inst : toResolve) {
            EventChoice best = Collections.max(inst.choices,
                    Comparator.comparingDouble((EventChoice c) -> c.aiWeight));
            resolveChoice(world, inst, best.id);
        }
    }

    private record CooldownKey(int eventId, int character) {
    }

    /** 全部内置事件定义（与 Python 版 engine.builtin_events 一一对应）。 */
    public static List<EventDef> builtinEvents() {
        List<EventDef> events = new ArrayList<>();

        events.add(new EventDef(1, "丰收之年", "领地迎来大丰收，农民献上额外税赋。",
                3.0, 365, false, List.of(
                new EventChoice(0, "收下贡赋", List.of(new Effect("gold", 25), new Effect("prestige", 5)), 10),
                new EventChoice(1, "减免租税以收买民心", List.of(new Effect("prestige", 15), new Effect("piety", 10)), 5)
        )));

        events.add(new EventDef(2, "宫廷丑闻", "朝中流传关于你的闲言碎语。",
                2.0, 180, false, List.of(
                new EventChoice(0, "置之不理", List.of(new Effect("stress", 15)), 5),
                new EventChoice(1, "严厉追查", List.of(new Effect("stress", 5), new Effect("gold", -10), new Effect("prestige", 5)), 8)
        )));

        events.add(new EventDef(3, "狩猎意外", "狩猎时坐骑受惊，你险些摔伤。",
                1.5, 400, true, List.of(
                new EventChoice(0, "强撑着继续", List.of(new Effect("health", -0.5), new Effect("prestige", 10)), 4),
                new EventChoice(1, "立即返回疗伤", List.of(new Effect("health", -0.2), new Effect("stress", 5)), 10)
        )));

        events.add(new EventDef(4, "虔诚的捐赠", "主教请求你资助修建教堂。",
                2.0, 300, false, List.of(
                new EventChoice(0, "慷慨解囊", List.of(new Effect("gold", -30), new Effect("piety", 40), new Effect("prestige", 10)), 6),
                new EventChoice(1, "婉拒", List.of(new Effect("piety", -10)), 4)
        ), true, true, 30.0, false));

        events.add(new EventDef(5, "封臣的抱怨", "一位封臣公开抱怨赋税过重。",
                2.5, 200, false, List.of(
                new EventChoice(0, "安抚许诺", List.of(new Effect("gold", -15), new Effect("stress", 5)), 7),
                new EventChoice(1, "威胁镇压", List.of(new Effect("prestige", 5), new Effect("stress", 10)), 5)
        )));

        events.add(new EventDef(6, "婚姻生活", "与配偶共度宁静的夜晚。",
                2.0, 120, false, List.of(
                new EventChoice(0, "享受温存", List.of(new Effect("stress", -10), new Effect("prestige", 2)), 10),
                new EventChoice(1, "讨论政务", List.of(new Effect("prestige", 5)), 6)
        ), true, true, 0.0, true));

        events.add(new EventDef(7, "流浪骑士求职", "一位骑士请求加入你的宫廷。",
                2.2, 240, false, List.of(
                new EventChoice(0, "收为家臣", List.of(
                        new Effect("gold", -20),
                        new Effect("prestige", 8),
                        new Effect("log", 0, "宫廷迎来新骑士")
                ), 8),
                new EventChoice(1, "打发走", List.of(), 3)
        ), true, true, 20.0, false));

        events.add(new EventDef(8, "瘟疫阴影", "附近村落出现疫病。",
                1.2, 500, true, List.of(
                new EventChoice(0, "封闭宫廷", List.of(new Effect("gold", -25), new Effect("stress", 10)), 9),
                new EventChoice(1, "组织救治", List.of(new Effect("gold", -40), new Effect("piety", 20), new Effect("health", -0.3)), 6),
                new EventChoice(2, "置若罔闻", List.of(new Effect("health", -0.8), new Effect("stress", 20)), 2)
        )));

        events.add(new EventDef(9, "边境劫掠", "强盗侵扰边境村庄。",
                2.3, 180, false, List.of(
                new EventChoice(0, "派兵清剿", List.of(new Effect("gold", -15), new Effect("prestige", 12)), 9),
                new EventChoice(1, "提高通行税补偿", List.of(new Effect("gold", 10), new Effect("prestige", -5)), 4)
        )));

        events.add(new EventDef(10, "学者来访", "一位学者带来稀有抄本。",
                1.8, 320, false, List.of(
                new EventChoice(0, "购入抄本", List.of(new Effect("gold", -25), new Effect("piety", 5), new Effect("trait", 8)), 7),
                new EventChoice(1, "婉言谢绝", List.of(), 4)
        ), true, true, 25.0, false));

        events.add(new EventDef(11, "节日庆典", "臣民请求举办丰年节。",
                2.0, 200, false, List.of(
                new EventChoice(0, "大办特办", List.of(new Effect("gold", -20), new Effect("prestige", 15), new Effect("stress", -8)), 8),
                new EventChoice(1, "象征性支持", List.of(new Effect("gold", -5), new Effect("prestige", 5)), 6)
        ), true, true, 15.0, false));

        events.add(new EventDef(12, "决斗挑战", "一位傲慢贵族要求决斗。",
                1.3, 450, true, List.of(
                new EventChoice(0, "亲自应战", List.of(new Effect("health", -0.4), new Effect("prestige", 20), new Effect("trait", 1)), 5),
                new EventChoice(1, "派骑士代战", List.of(new Effect("gold", -10), new Effect("prestige", 5)), 9),
                new EventChoice(2, "息事宁人", List.of(new Effect("prestige", -10), new Effect("stress", 8)), 3)
        )));

        events.add(new EventDef(13, "商路开通", "商会提议开辟新商路。",
                1.7, 360, false, List.of(
                new EventChoice(0, "投资商路", List.of(new Effect("gold", -40), new Effect("prestige", 8)), 7),
                new EventChoice(1, "征收路权税", List.of(new Effect("gold", 15), new Effect("prestige", -3)), 5)
        ), true, true, 40.0, false));

        events.add(new EventDef(14, "神秘访客", "戴兜帽的陌生人带来可疑情报。",
                1.6, 280, false, List.of(
                new EventChoice(0, "购买情报", List.of(new Effect("gold", -15), new Effect("prestige", 5)), 7),
                new EventChoice(1, "逮捕审讯", List.of(new Effect("stress", 5), new Effect("trait", 2)), 4),
                new EventChoice(2, "驱逐出城", List.of(), 5)
        )));

        events.add(new EventDef(15, "诗人献辞", "游吟诗人创作了赞美你的史诗。",
                1.8, 300, false, List.of(
                new EventChoice(0, "重金赏赐", List.of(new Effect("gold", -12), new Effect("prestige", 18)), 8),
                new EventChoice(1, "口头嘉奖", List.of(new Effect("prestige", 6)), 5)
        )));

        events.add(new EventDef(16, "继承危机", "远亲声称对你的领地有继承权。",
                1.5, 400, true, List.of(
                new EventChoice(0, "贿赂其放弃 claim", List.of(new Effect("gold", -30), new Effect("prestige", 5)), 7),
                new EventChoice(1, "公开驳斥", List.of(new Effect("prestige", 10), new Effect("stress", 5)), 6),
                new EventChoice(2, "暗地威胁", List.of(new Effect("stress", 10), new Effect("gold", -5)), 4)
        )));

        events.add(new EventDef(17, "丰收祭典", "秋季丰收，农民请求举办祭典。",
                2.0, 250, false, List.of(
                new EventChoice(0, "举办盛大祭典", List.of(new Effect("gold", -25), new Effect("prestige", 12), new Effect("stress", -5)), 8),
                new EventChoice(1, "简单庆祝", List.of(new Effect("gold", -8), new Effect("prestige", 5)), 6),
                new EventChoice(2, "取消祭典", List.of(new Effect("prestige", -8), new Effect("stress", 5)), 3)
        )));

        events.add(new EventDef(18, "密谋败露", "你发现了一起针对你的暗杀阴谋。",
                1.2, 500, true, List.of(
                new EventChoice(0, "公开审判", List.of(new Effect("prestige", 15), new Effect("stress", 5)), 7),
                new EventChoice(1, "秘密处决", List.of(new Effect("stress", 10), new Effect("gold", -10)), 6),
                new EventChoice(2, "宽恕并监视", List.of(new Effect("stress", 5)), 5)
        )));

        events.add(new EventDef(19, "商队遇袭", "你的商队在途中遭到劫掠。",
                1.8, 220, false, List.of(
                new EventChoice(0, "派兵护卫", List.of(new Effect("gold", -20), new Effect("prestige", 8)), 8),
                new EventChoice(1, "赔偿损失", List.of(new Effect("gold", -15)), 5),
                new EventChoice(2, "追捕劫匪", List.of(new Effect("gold", -10), new Effect("prestige", 12)), 6)
        )));

        events.add(new EventDef(20, "贵族求婚", "一位贵族向你或你的子女求婚。",
                1.6, 350, false, List.of(
                new EventChoice(0, "同意联姻", List.of(new Effect("gold", 20), new Effect("prestige", 10)), 7),
                new EventChoice(1, "婉拒", List.of(new Effect("stress", 5)), 5),
                new EventChoice(2, "提出 counter-offer", List.of(new Effect("gold", -10), new Effect("prestige", 5)), 4)
        )));

        events.add(new EventDef(21, "建筑火灾", "你领内的一座建筑突发火灾。",
                1.4, 260, false, List.of(
                new EventChoice(0, "拨款重建", List.of(new Effect("gold", -20), new Effect("prestige", 5)), 8),
                new EventChoice(1, "责令领主自费", List.of(new Effect("stress", 8), new Effect("prestige", -3)), 4)
        )));

        events.add(new EventDef(22, "外敌入侵", "边境传来警报，敌军正在劫掠村庄。",
                1.8, 180, true, List.of(
                new EventChoice(0, "亲征讨伐", List.of(new Effect("gold", -30), new Effect("prestige", 20), new Effect("health", -0.2)), 9),
                new EventChoice(1, "派兵拦截", List.of(new Effect("gold", -15), new Effect("prestige", 8)), 7),
                new EventChoice(2, "加固城防", List.of(new Effect("gold", -10), new Effect("stress", 5)), 5)
        )));

        events.add(new EventDef(23, "丰收祭典", "秋季丰收，农民请求举办祭典。",
                2.0, 250, false, List.of(
                new EventChoice(0, "举办盛大祭典", List.of(new Effect("gold", -25), new Effect("prestige", 12), new Effect("stress", -5)), 8),
                new EventChoice(1, "简单庆祝", List.of(new Effect("gold", -8), new Effect("prestige", 5)), 6),
                new EventChoice(2, "取消祭典", List.of(new Effect("prestige", -8), new Effect("stress", 5)), 3)
        )));

        events.add(new EventDef(24, "密谋败露", "你发现了一起针对你的暗杀阴谋。",
                1.2, 500, true, List.of(
                new EventChoice(0, "公开审判", List.of(new Effect("prestige", 15), new Effect("stress", 5)), 7),
                new EventChoice(1, "秘密处决", List.of(new Effect("stress", 10), new Effect("gold", -10)), 6),
                new EventChoice(2, "宽恕并监视", List.of(new Effect("stress", 5)), 5)
        )));

        events.add(new EventDef(25, "商队遇袭", "你的商队在途中遭到劫掠。",
                1.8, 220, false, List.of(
                new EventChoice(0, "派兵护卫", List.of(new Effect("gold", -20), new Effect("prestige", 8)), 8),
                new EventChoice(1, "赔偿损失", List.of(new Effect("gold", -15)), 5),
                new EventChoice(2, "追捕劫匪", List.of(new Effect("gold", -10), new Effect("prestige", 12)), 6)
        )));

        events.add(new EventDef(26, "贵族求婚", "一位贵族向你或你的子女求婚。",
                1.6, 350, false, List.of(
                new EventChoice(0, "同意联姻", List.of(new Effect("gold", 10), new Effect("prestige", 10)), 7),
                new EventChoice(1, "婉拒", List.of(new Effect("stress", 5)), 5),
                new EventChoice(2, "提出 counter-offer", List.of(new Effect("gold", -10), new Effect("prestige", 5)), 4)
        )));

        events.add(new EventDef(27, "瘟疫蔓延", "领地内爆发瘟疫，百姓流离失所。",
                1.3, 600, true, List.of(
                new EventChoice(0, "封锁疫区", List.of(new Effect("gold", -35), new Effect("stress", 15)), 8),
                new EventChoice(1, "施药救济", List.of(new Effect("gold", -50), new Effect("piety", 25), new Effect("health", -0.3)), 6),
                new EventChoice(2, "逃离领地", List.of(new Effect("prestige", -15), new Effect("stress", 20)), 2)
        )));

        events.add(new EventDef(28, "比武大会", "领地举办比武大会，各地骑士云集。",
                1.9, 300, false, List.of(
                new EventChoice(0, "赞助大赛", List.of(new Effect("gold", -30), new Effect("prestige", 20), new Effect("stress", -5)), 9),
                new EventChoice(1, "亲自参赛", List.of(new Effect("health", -0.3), new Effect("prestige", 15)), 7),
                new EventChoice(2, "仅观看", List.of(new Effect("stress", -3)), 5)
        )));

        events.add(new EventDef(29, "密信截获", "你截获了一封密信，揭露了邻国的军事计划。",
                1.5, 400, false, List.of(
                new EventChoice(0, "备战", List.of(new Effect("gold", -20), new Effect("prestige", 5)), 8),
                new EventChoice(1, "泄露给盟友", List.of(new Effect("prestige", 10), new Effect("stress", 3)), 6),
                new EventChoice(2, "销毁信件", List.of(new Effect("stress", 5)), 4)
        )));

        events.add(new EventDef(30, "饥荒预警", "连续干旱可能导致饥荒。",
                1.7, 320, false, List.of(
                new EventChoice(0, "开仓放粮", List.of(new Effect("gold", -40), new Effect("prestige", 10), new Effect("stress", -5)), 9),
                new EventChoice(1, "进口粮食", List.of(new Effect("gold", -25), new Effect("prestige", 3)), 7),
                new EventChoice(2, "加征粮税", List.of(new Effect("gold", 20), new Effect("prestige", -10), new Effect("stress", 10)), 3)
        )));

        return events;
    }
}
