from __future__ import annotations

import random
from dataclasses import dataclass, field
from typing import Dict, List, Tuple

from ck_engine.world.world_state import World


@dataclass
class Effect:
    kind: str
    amount: float = 0.0
    text: str = ""

    def apply(self, world: World, who: int) -> None:
        c = world.character(who)
        if not c:
            return
        if self.kind == "gold":
            c.add_gold(self.amount)
        elif self.kind == "prestige":
            c.add_prestige(self.amount)
        elif self.kind == "piety":
            c.piety = max(0.0, c.piety + self.amount)
        elif self.kind == "stress":
            c.add_stress(int(self.amount))
        elif self.kind == "health":
            c.health += self.amount
        elif self.kind == "trait":
            tid = int(self.amount)
            if tid not in c.traits:
                c.traits.append(tid)
        elif self.kind == "log" and self.text:
            world.push_log(self.text)


@dataclass
class EventChoice:
    id: int
    text: str
    effects: List[Effect]
    ai_weight: float = 5.0


@dataclass
class EventDef:
    id: int
    title: str
    description: str
    weight: float
    cooldown_days: int
    major: bool
    choices: List[EventChoice]
    requires_ruler: bool = True
    requires_adult: bool = True
    min_gold: float = 0.0
    requires_married: bool = False
    requires_children: bool = False


@dataclass
class EventInstance:
    event_id: int
    character: int
    title: str
    description: str
    choices: List[EventChoice]


@dataclass
class EventEngine:
    catalog: List[EventDef] = field(default_factory=list)
    pending: List[EventInstance] = field(default_factory=list)
    cooldowns: Dict[Tuple[int, int], int] = field(default_factory=dict)
    history: List[str] = field(default_factory=list)

    def __post_init__(self) -> None:
        if not self.catalog:
            self.catalog = builtin_events()

    def tick_cooldowns(self) -> None:
        self.cooldowns = {k: v - 1 for k, v in self.cooldowns.items() if v > 1}

    def _eligible(self, world: World, who: int, ev: EventDef) -> bool:
        c = world.character(who)
        if not c or not c.is_alive():
            return False
        if ev.requires_ruler and not c.is_ruler:
            return False
        if ev.requires_adult and not c.is_adult(world.date):
            return False
        if c.gold < ev.min_gold:
            return False
        if ev.requires_married and not c.is_married():
            return False
        if ev.requires_children and not any(
            child and child.is_alive() for child in (world.character(cid) for cid in c.children)
        ):
            return False
        return True

    def daily_check(self, world: World, characters: List[int]) -> None:
        for who in characters:
            for ev in self.catalog:
                key = (ev.id, who)
                if key in self.cooldowns:
                    continue
                if not self._eligible(world, who, ev):
                    continue
                p = max(0.005, min(0.35, ev.weight * 0.02))
                if random.random() < p:
                    self.pending.append(
                        EventInstance(
                            event_id=ev.id,
                            character=who,
                            title=ev.title,
                            description=ev.description,
                            choices=list(ev.choices),
                        )
                    )
                    self.cooldowns[key] = ev.cooldown_days
                    if ev.major:
                        break

    def resolve_choice(self, world: World, instance: EventInstance, choice_id: int) -> None:
        choice = next((c for c in instance.choices if c.id == choice_id), None)
        if not choice:
            return
        for eff in choice.effects:
            eff.apply(world, instance.character)
        c = world.character(instance.character)
        name = c.name if c else "?"
        line = f"{name} 事件「{instance.title}」选择：{choice.text}"
        self.history.append(line)
        world.push_log(line)

    def auto_resolve_all(self, world: World) -> None:
        pending = list(self.pending)
        self.pending.clear()
        for inst in pending:
            best = max(inst.choices, key=lambda c: c.ai_weight)
            self.resolve_choice(world, inst, best.id)


def builtin_events() -> List[EventDef]:
    def choice(cid: int, text: str, effects: List[Effect], w: float = 5.0) -> EventChoice:
        return EventChoice(id=cid, text=text, effects=effects, ai_weight=w)

    return [
        EventDef(
            1,
            "丰收之年",
            "领地迎来大丰收，农民献上额外税赋。",
            3.0,
            365,
            False,
            [
                choice(0, "收下贡赋", [Effect("gold", 25), Effect("prestige", 5)], 10),
                choice(1, "减免租税以收买民心", [Effect("prestige", 15), Effect("piety", 10)], 5),
            ],
        ),
        EventDef(
            2,
            "宫廷丑闻",
            "朝中流传关于你的闲言碎语。",
            2.0,
            180,
            False,
            [
                choice(0, "置之不理", [Effect("stress", 15)], 5),
                choice(1, "严厉追查", [Effect("stress", 5), Effect("gold", -10), Effect("prestige", 5)], 8),
            ],
        ),
        EventDef(
            3,
            "狩猎意外",
            "狩猎时坐骑受惊，你险些摔伤。",
            1.5,
            400,
            True,
            [
                choice(0, "强撑着继续", [Effect("health", -0.5), Effect("prestige", 10)], 4),
                choice(1, "立即返回疗伤", [Effect("health", -0.2), Effect("stress", 5)], 10),
            ],
        ),
        EventDef(
            4,
            "虔诚的捐赠",
            "主教请求你资助修建教堂。",
            2.0,
            300,
            False,
            [
                choice(0, "慷慨解囊", [Effect("gold", -30), Effect("piety", 40), Effect("prestige", 10)], 6),
                choice(1, "婉拒", [Effect("piety", -10)], 4),
            ],
            min_gold=30,
        ),
        EventDef(
            5,
            "封臣的抱怨",
            "一位封臣公开抱怨赋税过重。",
            2.5,
            200,
            False,
            [
                choice(0, "安抚许诺", [Effect("gold", -15), Effect("stress", 5)], 7),
                choice(1, "威胁镇压", [Effect("prestige", 5), Effect("stress", 10)], 5),
            ],
        ),
        EventDef(
            6,
            "婚姻生活",
            "与配偶共度宁静的夜晚。",
            2.0,
            120,
            False,
            [
                choice(0, "享受温存", [Effect("stress", -10), Effect("prestige", 2)], 10),
                choice(1, "讨论政务", [Effect("prestige", 5)], 6),
            ],
            requires_married=True,
        ),
        EventDef(
            7,
            "流浪骑士求职",
            "一位骑士请求加入你的宫廷。",
            2.2,
            240,
            False,
            [
                choice(0, "收为家臣", [Effect("gold", -20), Effect("prestige", 8), Effect("log", text="宫廷迎来新骑士")], 8),
                choice(1, "打发走", [], 3),
            ],
            min_gold=20,
        ),
        EventDef(
            8,
            "瘟疫阴影",
            "附近村落出现疫病。",
            1.2,
            500,
            True,
            [
                choice(0, "封闭宫廷", [Effect("gold", -25), Effect("stress", 10)], 9),
                choice(1, "组织救治", [Effect("gold", -40), Effect("piety", 20), Effect("health", -0.3)], 6),
                choice(2, "置若罔闻", [Effect("health", -0.8), Effect("stress", 20)], 2),
            ],
        ),
        EventDef(
            9,
            "边境劫掠",
            "强盗侵扰边境村庄。",
            2.3,
            180,
            False,
            [
                choice(0, "派兵清剿", [Effect("gold", -15), Effect("prestige", 12)], 9),
                choice(1, "提高通行税补偿", [Effect("gold", 10), Effect("prestige", -5)], 4),
            ],
        ),
        EventDef(
            11,
            "子女的求教",
            "你的孩子在宫廷中向你请教治国之道。",
            1.8,
            240,
            False,
            [
                choice(0, "亲自教导", [Effect("stress", 5), Effect("prestige", 8)], 8),
                choice(1, "交给宫廷导师", [Effect("gold", -15), Effect("prestige", 4)], 6),
            ],
            requires_children=True,
        ),
        EventDef(
            12,
            "家族聚会",
            "亲族齐聚宫廷，这正是维系家族感情的机会。",
            1.2,
            365,
            False,
            [
                choice(0, "设宴款待", [Effect("gold", -20), Effect("stress", -10), Effect("prestige", 10)], 8),
                choice(1, "简朴相聚", [Effect("stress", -5), Effect("piety", 5)], 6),
            ],
            requires_children=True,
        ),
        EventDef(
            10,
            "学者来访",
            "一位学者带来稀有抄本。",
            1.8,
            320,
            False,
            [
                choice(0, "购入抄本", [Effect("gold", -25), Effect("piety", 5), Effect("trait", 8)], 7),
                choice(1, "婉言谢绝", [], 4),
            ],
            min_gold=25,
        ),
        EventDef(
            31,
            "节日庆典",
            "臣民请求举办丰年节。",
            2.0,
            200,
            False,
            [
                choice(0, "大办特办", [Effect("gold", -20), Effect("prestige", 15), Effect("stress", -8)], 8),
                choice(1, "象征性支持", [Effect("gold", -5), Effect("prestige", 5)], 6),
            ],
            min_gold=15,
        ),
        EventDef(
            32,
            "决斗挑战",
            "一位傲慢贵族要求决斗。",
            1.3,
            450,
            True,
            [
                choice(0, "亲自应战", [Effect("health", -0.4), Effect("prestige", 20), Effect("trait", 1)], 5),
                choice(1, "派骑士代战", [Effect("gold", -10), Effect("prestige", 5)], 9),
                choice(2, "息事宁人", [Effect("prestige", -10), Effect("stress", 8)], 3),
            ],
        ),
        EventDef(
            13,
            "商路开通",
            "商会提议开辟新商路。",
            1.7,
            360,
            False,
            [
                choice(0, "投资商路", [Effect("gold", -40), Effect("prestige", 8)], 7),
                choice(1, "征收路权税", [Effect("gold", 15), Effect("prestige", -3)], 5),
            ],
            min_gold=40,
        ),
        EventDef(
            14,
            "神秘访客",
            "戴兜帽的陌生人带来可疑情报。",
            1.6,
            280,
            False,
            [
                choice(0, "购买情报", [Effect("gold", -15), Effect("prestige", 5)], 7),
                choice(1, "逮捕审讯", [Effect("stress", 5), Effect("trait", 2)], 4),
                choice(2, "驱逐出城", [], 5),
            ],
        ),
        EventDef(
            15,
            "诗人献辞",
            "游吟诗人创作了赞美你的史诗。",
            1.8,
            300,
            False,
            [
                choice(0, "重金赏赐", [Effect("gold", -12), Effect("prestige", 18)], 8),
                choice(1, "口头嘉奖", [Effect("prestige", 6)], 5),
            ],
        ),
        EventDef(
            16,
            "继承危机",
            "远亲声称对你的领地有继承权。",
            1.5,
            400,
            True,
            [
                choice(0, "贿赂其放弃 claim", [Effect("gold", -30), Effect("prestige", 5)], 7),
                choice(1, "公开驳斥", [Effect("prestige", 10), Effect("stress", 5)], 6),
                choice(2, "暗地威胁", [Effect("stress", 10), Effect("gold", -5)], 4),
            ],
        ),
        EventDef(
            17,
            "丰收祭典",
            "秋季丰收，农民请求举办祭典。",
            2.0,
            250,
            False,
            [
                choice(0, "举办盛大祭典", [Effect("gold", -25), Effect("prestige", 12), Effect("stress", -5)], 8),
                choice(1, "简单庆祝", [Effect("gold", -8), Effect("prestige", 5)], 6),
                choice(2, "取消祭典", [Effect("prestige", -8), Effect("stress", 5)], 3),
            ],
        ),
        EventDef(
            18,
            "密谋败露",
            "你发现了一起针对你的暗杀阴谋。",
            1.2,
            500,
            True,
            [
                choice(0, "公开审判", [Effect("prestige", 15), Effect("stress", 5)], 7),
                choice(1, "秘密处决", [Effect("stress", 10), Effect("gold", -10)], 6),
                choice(2, "宽恕并监视", [Effect("stress", 5)], 5),
            ],
        ),
        EventDef(
            19,
            "商队遇袭",
            "你的商队在途中遭到劫掠。",
            1.8,
            220,
            False,
            [
                choice(0, "派兵护卫", [Effect("gold", -20), Effect("prestige", 8)], 8),
                choice(1, "赔偿损失", [Effect("gold", -15)], 5),
                choice(2, "追捕劫匪", [Effect("gold", -10), Effect("prestige", 12)], 6),
            ],
        ),
        EventDef(
            20,
            "贵族求婚",
            "一位贵族向你或你的子女求婚。",
            1.6,
            350,
            False,
            [
                choice(0, "同意联姻", [Effect("prestige", 10), Effect("gold", 20)], 7),
                choice(1, "婉拒", [Effect("stress", 5)], 5),
                choice(2, "提出 counter-offer", [Effect("gold", -10), Effect("prestige", 5)], 4),
            ],
        ),
        EventDef(
            21,
            "建筑火灾",
            "你领内的一座建筑突发火灾。",
            1.4,
            260,
            False,
            [
                choice(0, "拨款重建", [Effect("gold", -20), Effect("prestige", 5)], 8),
                choice(1, "责令领主自费", [Effect("stress", 8), Effect("prestige", -3)], 4),
            ],
        ),
        EventDef(
            22,
            "外敌入侵",
            "边境传来警报，敌军正在劫掠村庄。",
            1.8,
            180,
            True,
            [
                choice(0, "亲征讨伐", [Effect("gold", -30), Effect("prestige", 20), Effect("health", -0.2)], 9),
                choice(1, "派兵拦截", [Effect("gold", -15), Effect("prestige", 8)], 7),
                choice(2, "加固城防", [Effect("gold", -10), Effect("stress", 5)], 5),
            ],
        ),
        EventDef(
            27,
            "瘟疫蔓延",
            "领地内爆发瘟疫，百姓流离失所。",
            1.3,
            600,
            True,
            [
                choice(0, "封锁疫区", [Effect("gold", -35), Effect("stress", 15)], 8),
                choice(1, "施药救济", [Effect("gold", -50), Effect("piety", 25), Effect("health", -0.3)], 6),
                choice(2, "逃离领地", [Effect("prestige", -15), Effect("stress", 20)], 2),
            ],
        ),
        EventDef(
            28,
            "比武大会",
            "领地举办比武大会，各地骑士云集。",
            1.9,
            300,
            False,
            [
                choice(0, "赞助大赛", [Effect("gold", -30), Effect("prestige", 20), Effect("stress", -5)], 9),
                choice(1, "亲自参赛", [Effect("health", -0.3), Effect("prestige", 15)], 7),
                choice(2, "仅观看", [Effect("stress", -3)], 5),
            ],
        ),
        EventDef(
            29,
            "密信截获",
            "你截获了一封密信，揭露了邻国的军事计划。",
            1.5,
            400,
            False,
            [
                choice(0, "备战", [Effect("gold", -20), Effect("prestige", 5)], 8),
                choice(1, "泄露给盟友", [Effect("prestige", 10), Effect("stress", 3)], 6),
                choice(2, "销毁信件", [Effect("stress", 5)], 4),
            ],
        ),
        EventDef(
            30,
            "饥荒预警",
            "连续干旱可能导致饥荒。",
            1.7,
            320,
            False,
            [
                choice(0, "开仓放粮", [Effect("gold", -40), Effect("prestige", 10), Effect("stress", -5)], 9),
                choice(1, "进口粮食", [Effect("gold", -25), Effect("prestige", 3)], 7),
                choice(2, "加征粮税", [Effect("gold", 20), Effect("prestige", -10), Effect("stress", 10)], 3),
            ],
        ),
        EventDef(
            33,
            "矿脉发现",
            "勘探队在领内发现了一处银矿脉。",
            1.5,
            400,
            False,
            [
                choice(0, "投资开采", [Effect("gold", -50), Effect("prestige", 8)], 8),
                choice(1, "出售矿权", [Effect("gold", 40), Effect("prestige", -3)], 6),
                choice(2, "暂缓开发", [], 4),
            ],
        ),
        EventDef(
            34,
            "朝圣之旅",
            "一位主教邀请你前往圣地朝圣。",
            1.6,
            500,
            False,
            [
                choice(0, "欣然前往", [Effect("gold", -40), Effect("piety", 40), Effect("stress", -10)], 8),
                choice(1, "派人代为朝圣", [Effect("gold", -20), Effect("piety", 15)], 6),
                choice(2, "婉言谢绝", [Effect("piety", -5)], 4),
            ],
            min_gold=20,
        ),
        EventDef(
            35,
            "异国使节",
            "远方国度的使节携礼物来访。",
            1.7,
            360,
            False,
            [
                choice(0, "盛情款待", [Effect("gold", -25), Effect("prestige", 15)], 8),
                choice(1, "冷淡应对", [Effect("stress", 3)], 4),
                choice(2, "趁机结盟", [Effect("prestige", 8), Effect("gold", -10)], 6),
            ],
        ),
        EventDef(
            36,
            "宫廷医师",
            "一位名医来到宫廷，愿为你调理身体。",
            1.8,
            320,
            False,
            [
                choice(0, "重金聘请", [Effect("gold", -30), Effect("health", 0.5), Effect("stress", -5)], 8),
                choice(1, "试用偏方", [Effect("health", 0.2), Effect("gold", -10)], 6),
                choice(2, "婉拒", [], 4),
            ],
        ),
    ]
