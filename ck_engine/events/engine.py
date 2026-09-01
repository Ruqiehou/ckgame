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
    period: str = "all"


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

    @staticmethod
    def _period_matches(world: World, period: str) -> bool:
        if period == "all":
            return True
        year = world.date.year
        if period == "viking" and year < 900:
            return True
        if period == "feudal" and 900 <= year < 1100:
            return True
        if period == "crusade" and 1096 <= year < 1300:
            return True
        if period == "plague" and 1300 <= year < 1450:
            return True
        if period == "rose" and 1450 <= year < 1520:
            return True
        return False

    def _eligible(self, world: World, who: int, ev: EventDef) -> bool:
        c = world.character(who)
        if not c or not c.is_alive():
            return False
        if not self._period_matches(world, ev.period):
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
        EventDef(
            37,
            "维京长船",
            "海岸烽火接连燃起，满载战士的长船正向港口逼近。",
            1.5,
            360,
            True,
            [
                choice(0, "召集军队迎战", [Effect("gold", -30), Effect("prestige", 22), Effect("health", -0.2)], 9),
                choice(1, "支付赎金换取撤退", [Effect("gold", -55), Effect("prestige", -8), Effect("stress", -5)], 5),
                choice(2, "疏散沿海居民", [Effect("gold", -18), Effect("piety", 10), Effect("stress", 8)], 7),
            ],
        ),
        EventDef(
            38,
            "重铸王室货币",
            "财政官建议重铸银币，以充实日渐空虚的国库。",
            1.6,
            420,
            False,
            [
                choice(0, "维持足色银币", [Effect("gold", -15), Effect("prestige", 15)], 7),
                choice(1, "降低银币成色", [Effect("gold", 45), Effect("prestige", -18), Effect("stress", 6)], 6),
                choice(2, "暂不干预", [], 5),
            ],
        ),
        EventDef(
            39,
            "封臣私斗",
            "两位封臣为边界和继承权大打出手，请求你出面裁决。",
            2.0,
            240,
            False,
            [
                choice(0, "举行公开审判", [Effect("gold", -12), Effect("prestige", 14), Effect("stress", 5)], 8),
                choice(1, "命令双方立即停战", [Effect("prestige", 5), Effect("stress", 10)], 6),
                choice(2, "坐视他们互相削弱", [Effect("prestige", -8), Effect("trait", 2)], 4),
            ],
        ),
        EventDef(
            40,
            "信仰之争",
            "传教士与本地祭司在集市公开辩论，引发民众对立。",
            1.5,
            390,
            True,
            [
                choice(0, "支持正统教士", [Effect("piety", 28), Effect("gold", -15), Effect("stress", 5)], 8),
                choice(1, "允许双方和平传道", [Effect("prestige", 10), Effect("piety", -5)], 6),
                choice(2, "驱逐所有煽动者", [Effect("prestige", 5), Effect("stress", 8)], 5),
            ],
        ),
        EventDef(
            41,
            "继承人的导师",
            "宫廷中两位学者争相请求担任你子女的导师。",
            1.7,
            300,
            False,
            [
                choice(0, "聘请博学神职人员", [Effect("gold", -25), Effect("piety", 12), Effect("prestige", 5)], 8),
                choice(1, "选择久经沙场的骑士", [Effect("gold", -20), Effect("prestige", 12)], 7),
                choice(2, "由我亲自教导", [Effect("stress", 12), Effect("prestige", 8)], 5),
            ],
            requires_children=True,
        ),
        EventDef(
            42,
            "严冬围困",
            "暴雪封锁道路，宫廷的粮食和柴火正在迅速减少。",
            1.8,
            260,
            False,
            [
                choice(0, "高价采购补给", [Effect("gold", -35), Effect("health", 0.2), Effect("stress", -5)], 8),
                choice(1, "与百姓共同节衣缩食", [Effect("prestige", 15), Effect("health", -0.2), Effect("stress", 8)], 6),
                choice(2, "征用附近村庄储备", [Effect("gold", 12), Effect("prestige", -15)], 4),
            ],
        ),
        EventDef(
            43,
            "流亡贵族",
            "一名被邻国放逐的贵族请求庇护，并声称掌握故国的秘密。",
            1.5,
            380,
            False,
            [
                choice(0, "给予庇护", [Effect("gold", -20), Effect("prestige", 12), Effect("log", text="一名流亡贵族加入了宫廷")], 8),
                choice(1, "索取情报后驱逐", [Effect("gold", 10), Effect("prestige", -3)], 6),
                choice(2, "将其交还邻国", [Effect("gold", 25), Effect("piety", -10)], 4),
            ],
        ),
        EventDef(
            44,
            "港口走私",
            "税吏发现商人利用夜色走私盐、酒和异国武器。",
            2.0,
            220,
            False,
            [
                choice(0, "没收全部货物", [Effect("gold", 30), Effect("prestige", 5)], 8),
                choice(1, "收取罚金后放行", [Effect("gold", 18), Effect("prestige", -4)], 7),
                choice(2, "借机建立秘密渠道", [Effect("gold", 25), Effect("stress", 8), Effect("trait", 2)], 4),
            ],
        ),
        EventDef(
            45,
            "古代遗迹",
            "农民在修路时发现一处古代墓穴，里面可能藏有珍宝与文献。",
            1.3,
            520,
            True,
            [
                choice(0, "组织学者发掘", [Effect("gold", -25), Effect("prestige", 18), Effect("piety", 8)], 8),
                choice(1, "取走陪葬财宝", [Effect("gold", 50), Effect("piety", -18), Effect("stress", 5)], 5),
                choice(2, "重新封闭墓穴", [Effect("piety", 15), Effect("prestige", 3)], 6),
            ],
        ),
        EventDef(
            46,
            "军队哗变",
            "拖欠军饷的士兵包围营帐，要求立即得到报酬。",
            1.4,
            420,
            True,
            [
                choice(0, "补发全部军饷", [Effect("gold", -45), Effect("prestige", 8), Effect("stress", -5)], 9),
                choice(1, "处决带头者", [Effect("prestige", 12), Effect("stress", 18), Effect("health", -0.1)], 5),
                choice(2, "许诺战后分配战利品", [Effect("prestige", -5), Effect("stress", 10)], 6),
            ],
            min_gold=10,
        ),
        EventDef(
            47,
            "龙首船来袭",
            "瞭望台发出警报：刻着狰狞龙首的长船正顺风逼近港口。",
            1.8,
            300,
            True,
            [
                choice(0, "沿海岸列阵迎敌", [Effect("gold", -30), Effect("prestige", 20), Effect("health", -0.2)], 9),
                choice(1, "退入内陆坚城", [Effect("stress", 10), Effect("prestige", -5)], 4),
                choice(2, "献上财物求和", [Effect("gold", -45), Effect("stress", -5)], 5),
            ],
            period="viking",
        ),
        EventDef(
            48,
            "俘获战利品",
            "突袭得手，士兵带回成捆的皮毛、蜂蜜与战俘。",
            1.7,
            260,
            False,
            [
                choice(0, "分赏全军", [Effect("gold", -15), Effect("prestige", 15)], 8),
                choice(1, "充入国库", [Effect("gold", 40), Effect("prestige", -3)], 5),
                choice(2, "赎回战俘", [Effect("gold", -20), Effect("piety", 20)], 6),
            ],
            period="viking",
        ),
        EventDef(
            49,
            "卢恩石碑",
            "猎人发现一块刻满古文字的石碑，传言埋藏着先王的宝藏。",
            1.4,
            420,
            True,
            [
                choice(0, "招募解读者", [Effect("gold", -25), Effect("prestige", 15)], 8),
                choice(1, "当作祭品焚毁", [Effect("piety", -5), Effect("stress", 5)], 4),
                choice(2, "建塔保护", [Effect("gold", -10), Effect("piety", 10)], 6),
            ],
            period="viking",
        ),
        EventDef(
            50,
            "萨迦诗人",
            "年迈的诗人用长诗吟唱你祖先的传奇，请求你续写新章。",
            1.6,
            320,
            False,
            [
                choice(0, "赞助传颂", [Effect("gold", -15), Effect("prestige", 20)], 9),
                choice(1, "请其记录法典", [Effect("gold", -10), Effect("piety", 5), Effect("prestige", 8)], 6),
                choice(2, "婉言谢绝", [], 3),
            ],
            period="viking",
        ),
        EventDef(
            51,
            "结冰海峡",
            "严冬使海峡结冰，一支敌军竟从冰面径直走来。",
            1.5,
            360,
            True,
            [
                choice(0, "破冰阻敌", [Effect("gold", -20), Effect("prestige", 15)], 8),
                choice(1, "冰上迎战", [Effect("health", -0.3), Effect("prestige", 25)], 6),
                choice(2, "坚守不出", [Effect("stress", 10)], 5),
            ],
            period="viking",
        ),
        EventDef(
            52,
            "奴隶市场",
            "战俘在集市上公开出售，你的总管询问处置之法。",
            1.5,
            300,
            False,
            [
                choice(0, "全部出售", [Effect("gold", 50), Effect("piety", -12)], 6),
                choice(1, "留为农奴", [Effect("gold", 25), Effect("prestige", -5)], 5),
                choice(2, "释放以示仁德", [Effect("piety", 20), Effect("prestige", 8)], 7),
            ],
            period="viking",
        ),
        EventDef(
            53,
            "渡鸦信使",
            "一只信鸽被绑上渡鸦的羽毛送来情报——邻部正在密会。",
            1.3,
            380,
            False,
            [
                choice(0, "召集长老议事", [Effect("stress", 8), Effect("prestige", 5)], 7),
                choice(1, "抢先袭击", [Effect("gold", -25), Effect("prestige", 18), Effect("health", -0.2)], 8),
                choice(2, "派使和解", [Effect("gold", -10), Effect("prestige", 5)], 5),
            ],
            period="viking",
        ),
        EventDef(
            54,
            "牧草之争",
            "两个氏族为一片夏季牧场争执不休，各自拉起人马。",
            1.8,
            240,
            False,
            [
                choice(0, "亲自划界", [Effect("prestige", 12), Effect("stress", 5)], 8),
                choice(1, "居中调解", [Effect("prestige", 8), Effect("gold", -5)], 6),
                choice(2, "暗中支持强族", [Effect("gold", 10), Effect("stress", 10), Effect("trait", 2)], 4),
            ],
            period="viking",
        ),
        EventDef(
            55,
            "造船工匠",
            "一位老匠人声称能造出快如海燕的新式长船。",
            1.7,
            400,
            False,
            [
                choice(0, "拨付木材金银", [Effect("gold", -40), Effect("prestige", 12)], 8),
                choice(1, "先看图纸", [Effect("gold", -10), Effect("prestige", 3)], 5),
                choice(2, "令其随军服役", [Effect("prestige", -5)], 4),
            ],
            period="viking",
        ),
        EventDef(
            56,
            "火葬之礼",
            "一位功勋首领离世，族人询问该以何种礼仪安葬。",
            1.2,
            500,
            False,
            [
                choice(0, "隆重火葬", [Effect("gold", -30), Effect("piety", 15), Effect("prestige", 10)], 8),
                choice(1, "土葬立碑", [Effect("gold", -15), Effect("prestige", 5)], 6),
                choice(2, "简单安葬", [], 4),
            ],
            period="viking",
        ),
        EventDef(
            57,
            "骑士受封",
            "一位勇猛的侍从在战场上护你周全，请求获封骑士。",
            1.8,
            300,
            False,
            [
                choice(0, "亲自授剑", [Effect("gold", -15), Effect("prestige", 15)], 9),
                choice(1, "赏金代赏", [Effect("gold", -10), Effect("prestige", 5)], 6),
                choice(2, "命其再立军功", [Effect("stress", 5)], 4),
            ],
            period="feudal",
        ),
        EventDef(
            58,
            "堡垒工程",
            "工匠提议在险要关隘修建一座石质堡垒。",
            1.7,
            420,
            False,
            [
                choice(0, "倾力修建", [Effect("gold", -60), Effect("prestige", 20)], 9),
                choice(1, "修建简易壁垒", [Effect("gold", -25), Effect("prestige", 6)], 6),
                choice(2, "搁置计划", [], 3),
            ],
            period="feudal",
        ),
        EventDef(
            59,
            "领主忠诚",
            "一封领主来信措辞恭顺，却请求免除全部兵役。",
            1.6,
            260,
            False,
            [
                choice(0, "严辞拒绝", [Effect("prestige", 5), Effect("stress", 8)], 6),
                choice(1, "同意减免", [Effect("prestige", -8), Effect("gold", -10)], 5),
                choice(2, "索取质押", [Effect("gold", 15), Effect("prestige", 8)], 7),
            ],
            period="feudal",
        ),
        EventDef(
            60,
            "法袍之争",
            "两位领主为谁有权审判而争吵，围观的百姓越来越多。",
            2.0,
            220,
            False,
            [
                choice(0, "命其携证据上庭", [Effect("gold", -10), Effect("prestige", 10)], 8),
                choice(1, "各打五十大板", [Effect("prestige", 3), Effect("stress", 5)], 5),
                choice(2, "收编为王庭案件", [Effect("gold", 10), Effect("stress", 8)], 4),
            ],
            period="feudal",
        ),
        EventDef(
            61,
            "巡回法庭",
            "王庭巡游至此，百姓排长队等候裁断积年纠纷。",
            1.8,
            280,
            False,
            [
                choice(0, "亲自主审", [Effect("stress", 15), Effect("prestige", 18)], 9),
                choice(1, "委派法官", [Effect("gold", -15), Effect("prestige", 6)], 6),
                choice(2, "快速结案", [Effect("stress", 5), Effect("prestige", 3)], 5),
            ],
            period="feudal",
        ),
        EventDef(
            62,
            "封地继承",
            "年迈的封臣去世，其领地该由长子还是亲信继承引发争议。",
            1.5,
            400,
            True,
            [
                choice(0, "维持长子继承", [Effect("prestige", 10), Effect("gold", -10)], 8),
                choice(1, "收归直辖", [Effect("gold", 20), Effect("stress", 10)], 5),
                choice(2, "让族中推举", [Effect("stress", 5)], 6),
            ],
            period="feudal",
        ),
        EventDef(
            63,
            "城堡宴会",
            "各路贵族应邀赴宴，觥筹交错间暗流涌动。",
            1.9,
            200,
            False,
            [
                choice(0, "豪掷一场", [Effect("gold", -35), Effect("prestige", 18), Effect("stress", -8)], 9),
                choice(1, "暗中观察", [Effect("stress", 5)], 5),
                choice(2, "趁机结盟", [Effect("gold", -15), Effect("prestige", 12)], 7),
            ],
            period="feudal",
        ),
        EventDef(
            64,
            "比武较技",
            "竞技场上你的骑士与邻国冠军一决高下。",
            1.6,
            350,
            True,
            [
                choice(0, "亲自下场", [Effect("health", -0.3), Effect("prestige", 22)], 6),
                choice(1, "押注自家骑士", [Effect("gold", -10), Effect("prestige", 12)], 8),
                choice(2, "拒绝应战", [Effect("prestige", -8)], 3),
            ],
            period="feudal",
        ),
        EventDef(
            65,
            "教士改革",
            "一位改革派教士呼吁重建教区、整饬纪律。",
            1.5,
            380,
            False,
            [
                choice(0, "支持改革", [Effect("gold", -20), Effect("piety", 25)], 8),
                choice(1, "维持旧制", [Effect("piety", -5), Effect("stress", 5)], 5),
                choice(2, "两派并立", [Effect("prestige", 5), Effect("stress", 8)], 6),
            ],
            period="feudal",
        ),
        EventDef(
            66,
            "诺曼工匠",
            "来自海峡对岸的工匠展示石拱与彩色玻璃的技艺。",
            1.4,
            320,
            False,
            [
                choice(0, "聘请来造大教堂", [Effect("gold", -45), Effect("piety", 20), Effect("prestige", 10)], 8),
                choice(1, "资助开设工坊", [Effect("gold", -25), Effect("prestige", 6)], 6),
                choice(2, "打发离去", [], 4),
            ],
            period="feudal",
        ),
        EventDef(
            67,
            "十字军征召",
            "圣战号召传来，贵族与农夫都为远征而骚动。",
            2.2,
            500,
            True,
            [
                choice(0, "亲自领兵", [Effect("gold", -60), Effect("piety", 45), Effect("health", -0.3)], 8),
                choice(1, "出资资助", [Effect("gold", -30), Effect("piety", 20)], 6),
                choice(2, "婉拒号召", [Effect("piety", -10), Effect("prestige", -3)], 4),
            ],
            period="crusade",
        ),
        EventDef(
            68,
            "圣地商路",
            "商人们声称打通了通往圣地的香料商路。",
            1.6,
            400,
            False,
            [
                choice(0, "投资护航商队", [Effect("gold", -35), Effect("prestige", 10)], 8),
                choice(1, "征收通行税", [Effect("gold", 20), Effect("prestige", -3)], 6),
                choice(2, "敬而远之", [], 4),
            ],
            period="crusade",
        ),
        EventDef(
            69,
            "圣殿骑士",
            "一位圣殿骑士携银行凭据造访，愿以土地抵押借款。",
            1.5,
            360,
            False,
            [
                choice(0, "慷慨放贷", [Effect("gold", -40), Effect("prestige", 12)], 8),
                choice(1, "只借小额", [Effect("gold", -15), Effect("piety", 5)], 6),
                choice(2, "婉拒", [], 4),
            ],
            period="crusade",
        ),
        EventDef(
            70,
            "清真学者",
            "一名来自安达卢西亚的学者带来天文与医术抄本。",
            1.6,
            380,
            False,
            [
                choice(0, "延请讲学", [Effect("gold", -20), Effect("piety", 5), Effect("trait", 8)], 8),
                choice(1, "抄录典籍", [Effect("gold", -25), Effect("trait", 8)], 7),
                choice(2, "逐客", [Effect("piety", -5)], 4),
            ],
            period="crusade",
        ),
        EventDef(
            71,
            "圣地遗物",
            "朝圣者带回传说中圣人的遗骨，请求供奉。",
            1.5,
            420,
            True,
            [
                choice(0, "兴建圣龛", [Effect("gold", -35), Effect("piety", 30)], 9),
                choice(1, "就地供奉", [Effect("piety", 15)], 6),
                choice(2, "疑为赝品", [Effect("piety", -5), Effect("stress", 5)], 4),
            ],
            period="crusade",
        ),
        EventDef(
            72,
            "骑士团内讧",
            "医院骑士与圣殿骑士为一座城堡的管辖权争执不下。",
            1.7,
            320,
            False,
            [
                choice(0, "主持公道", [Effect("gold", -15), Effect("piety", 10), Effect("prestige", 8)], 8),
                choice(1, "让教会仲裁", [Effect("gold", -10), Effect("piety", 8)], 6),
                choice(2, "渔翁得利", [Effect("gold", 10), Effect("piety", -8)], 4),
            ],
            period="crusade",
        ),
        EventDef(
            73,
            "东方使团",
            "拜占庭的使团带着紫绸与秘术卷轴来访。",
            1.7,
            400,
            False,
            [
                choice(0, "结为同盟", [Effect("gold", -20), Effect("prestige", 15)], 8),
                choice(1, "礼赠遣返", [Effect("gold", -10), Effect("prestige", 5)], 6),
                choice(2, "婉言相拒", [Effect("stress", 3)], 4),
            ],
            period="crusade",
        ),
        EventDef(
            74,
            "疫后重建",
            "瘟疫渐退，幸存者们渴望重新开垦荒地。",
            1.9,
            300,
            False,
            [
                choice(0, "减免新垦赋税", [Effect("gold", -20), Effect("prestige", 15)], 8),
                choice(1, "招募流民", [Effect("gold", -25), Effect("prestige", 10)], 6),
                choice(2, "维持旧制", [Effect("gold", 5), Effect("prestige", -5)], 4),
            ],
            period="plague",
        ),
        EventDef(
            75,
            "隔离令",
            "港口出现疑似疫病，船长请求隐瞒靠岸。",
            1.6,
            350,
            True,
            [
                choice(0, "严令隔离", [Effect("gold", -20), Effect("prestige", 10)], 9),
                choice(1, "暗中放行", [Effect("gold", 15), Effect("stress", 12), Effect("trait", 2)], 4),
                choice(2, "焚毁疫船", [Effect("gold", -30), Effect("piety", 8)], 6),
            ],
            period="plague",
        ),
        EventDef(
            76,
            "行医禁令",
            "医生要求取缔民间偏方，只准官府行医。",
            1.4,
            280,
            False,
            [
                choice(0, "支持官方医生", [Effect("gold", -15), Effect("health", 0.2)], 7),
                choice(1, "任其自由行医", [Effect("health", 0.1), Effect("stress", 5)], 5),
                choice(2, "全部禁绝", [Effect("piety", 5), Effect("health", -0.1)], 4),
            ],
            period="plague",
        ),
        EventDef(
            77,
            "空置庄园",
            "疫病夺走许多庄园主性命，大量田地无人耕种。",
            1.8,
            260,
            False,
            [
                choice(0, "低价收地", [Effect("gold", -30), Effect("prestige", 12)], 8),
                choice(1, "赐予幸存者", [Effect("prestige", 15), Effect("piety", 8)], 7),
                choice(2, "任其荒芜", [Effect("gold", 10), Effect("prestige", -8)], 4),
            ],
            period="plague",
        ),
        EventDef(
            78,
            "疫病谣言",
            "有人散播瘟疫系贵族投毒所致，民众群情激愤。",
            1.7,
            300,
            True,
            [
                choice(0, "公开辟谣", [Effect("gold", -10), Effect("prestige", 8)], 8),
                choice(1, "追查谣言源头", [Effect("stress", 10), Effect("prestige", 5)], 6),
                choice(2, "无视流言", [Effect("stress", 15)], 4),
            ],
            period="plague",
        ),
        EventDef(
            79,
            "黑死病祭",
            "教士提议举行盛大游行祈求瘟疫消退。",
            1.5,
            400,
            False,
            [
                choice(0, "亲自主持", [Effect("gold", -25), Effect("piety", 25)], 9),
                choice(1, "出资支持", [Effect("gold", -10), Effect("piety", 10)], 6),
                choice(2, "拒绝集会", [Effect("stress", 5), Effect("piety", -5)], 4),
            ],
            period="plague",
        ),
        EventDef(
            80,
            "疫后劳工",
            "幸存的劳工要求提高工钱，否则拒绝下田。",
            1.8,
            240,
            False,
            [
                choice(0, "同意加薪", [Effect("gold", -30), Effect("prestige", 5)], 7),
                choice(1, "强行征调", [Effect("gold", 10), Effect("prestige", -12)], 4),
                choice(2, "许诺来年", [Effect("stress", 8)], 5),
            ],
            period="plague",
        ),
        EventDef(
            81,
            "玫瑰之争",
            "两大家族同时展示相同的王冠纹章，争夺继承权。",
            2.0,
            300,
            True,
            [
                choice(0, "主持仲裁", [Effect("prestige", 15), Effect("stress", 10)], 8),
                choice(1, "押注一方", [Effect("gold", -20), Effect("prestige", 12)], 6),
                choice(2, "坐山观虎斗", [Effect("stress", 5), Effect("prestige", -5)], 4),
            ],
            period="rose",
        ),
        EventDef(
            82,
            "王冠借款",
            "一位国王特使登门，请求借贷以支撑战事。",
            1.6,
            360,
            False,
            [
                choice(0, "慷慨借贷", [Effect("gold", -50), Effect("prestige", 20)], 8),
                choice(1, "高价放贷", [Effect("gold", -30), Effect("prestige", 8)], 6),
                choice(2, "婉拒", [], 4),
            ],
            period="rose",
        ),
        EventDef(
            83,
            "印刷机到货",
            "一台新式印刷机抵达，匠人演示活字排版。",
            1.6,
            400,
            False,
            [
                choice(0, "资助设坊", [Effect("gold", -30), Effect("prestige", 15), Effect("trait", 8)], 8),
                choice(1, "购买样书", [Effect("gold", -10), Effect("prestige", 3)], 6),
                choice(2, "视为新奇", [], 4),
            ],
            period="rose",
        ),
        EventDef(
            84,
            "摄政之争",
            "国王病弱，几位权臣争相要求摄政之位。",
            1.7,
            380,
            True,
            [
                choice(0, "亲自摄政", [Effect("stress", 15), Effect("prestige", 15)], 8),
                choice(1, "推举中立者", [Effect("prestige", 5), Effect("stress", 8)], 6),
                choice(2, "坐视相争", [Effect("stress", 10), Effect("prestige", -8)], 4),
            ],
            period="rose",
        ),
        EventDef(
            85,
            "猎巫疑云",
            "村民指控老妪施咒导致牲畜暴毙。",
            1.5,
            350,
            True,
            [
                choice(0, "下令彻查", [Effect("prestige", 5), Effect("stress", 10)], 7),
                choice(1, "判定无事", [Effect("piety", 5), Effect("stress", 5)], 6),
                choice(2, "将其驱逐", [Effect("gold", 5), Effect("piety", -8)], 4),
            ],
            period="rose",
        ),
        EventDef(
            86,
            "火绳枪手",
            "雇佣兵展示新式火绳枪，声如惊雷、硝烟弥漫。",
            1.7,
            420,
            True,
            [
                choice(0, "招募成军", [Effect("gold", -45), Effect("prestige", 15)], 9),
                choice(1, "只购少数试用", [Effect("gold", -15), Effect("prestige", 5)], 6),
                choice(2, "斥为妖术", [Effect("prestige", -8)], 3),
            ],
            period="rose",
        ),
        EventDef(
            87,
            "同盟背盟",
            "一个盟国暗中与你的敌手谈判的消息传来。",
            1.8,
            320,
            False,
            [
                choice(0, "遣使质询", [Effect("stress", 8), Effect("prestige", 5)], 7),
                choice(1, "抢先另结同盟", [Effect("gold", -20), Effect("prestige", 8)], 8),
                choice(2, "静观其变", [Effect("stress", 10)], 4),
            ],
            period="rose",
        ),
        EventDef(
            88,
            "商人议会",
            "城市商人请求组建议会以管理贸易与市政。",
            1.6,
            340,
            False,
            [
                choice(0, "允其自治", [Effect("gold", 25), Effect("prestige", 10)], 8),
                choice(1, "保留监督", [Effect("gold", 15), Effect("prestige", 5)], 6),
                choice(2, "驳回请求", [Effect("stress", 5), Effect("prestige", -5)], 4),
            ],
            period="rose",
        ),
        EventDef(
            89,
            "王朝联姻",
            "敌对的家族提议以联姻结束多年宿怨。",
            1.7,
            400,
            True,
            [
                choice(0, "欣然同意", [Effect("gold", -15), Effect("prestige", 20), Effect("stress", -5)], 9),
                choice(1, "要求更多条件", [Effect("prestige", 8), Effect("stress", 5)], 6),
                choice(2, "严辞拒绝", [Effect("stress", 10), Effect("prestige", -10)], 3),
            ],
            period="rose",
        ),
        EventDef(
            90,
            "王冠加冕",
            "大主教提议为你举行盛大加冕，重塑正统形象。",
            1.5,
            500,
            True,
            [
                choice(0, "倾力操办", [Effect("gold", -50), Effect("piety", 20), Effect("prestige", 25)], 9),
                choice(1, "简朴加冕", [Effect("gold", -20), Effect("prestige", 10)], 6),
                choice(2, "暂缓", [Effect("prestige", -5)], 4),
            ],
            period="rose",
        ),
        EventDef(
            91,
            "北方海盗",
            "北方的掠夺者趁夜袭扰沿海村落。",
            1.8,
            280,
            False,
            [
                choice(0, "派船巡海", [Effect("gold", -20), Effect("prestige", 12)], 8),
                choice(1, "沿岸筑垒", [Effect("gold", -30), Effect("prestige", 8)], 6),
                choice(2, "缴纳保护费", [Effect("gold", -15), Effect("prestige", -5)], 4),
            ],
            period="rose",
        ),
        EventDef(
            92,
            "异端审问",
            "城市里流行起新教义，教士请求镇压。",
            1.6,
            380,
            True,
            [
                choice(0, "支持镇压", [Effect("piety", 15), Effect("stress", 10)], 7),
                choice(1, "召集辩论", [Effect("gold", -10), Effect("prestige", 5)], 6),
                choice(2, "放任自流", [Effect("piety", -8), Effect("stress", 5)], 4),
            ],
            period="rose",
        ),
        EventDef(
            93,
            "玫瑰王冠",
            "花匠献上一顶用红白玫瑰编成的花冠。",
            1.2,
            500,
            False,
            [
                choice(0, "收下戴起", [Effect("prestige", 8), Effect("stress", -5)], 7),
                choice(1, "转赠教会", [Effect("piety", 8)], 5),
                choice(2, "令人烧掉", [Effect("prestige", -5)], 3),
            ],
            period="rose",
        ),
        EventDef(
            94,
            "远洋地图",
            "一位航海家展示标注着未知大陆的海图。",
            1.6,
            450,
            False,
            [
                choice(0, "资助远航", [Effect("gold", -45), Effect("prestige", 18)], 8),
                choice(1, "抄录地图", [Effect("gold", -10), Effect("prestige", 3)], 6),
                choice(2, "斥为妄谈", [], 4),
            ],
            period="rose",
        ),
        EventDef(
            95,
            "宫廷诗人",
            "诗人以玫瑰战争为题材创作长篇史诗，引发热议。",
            1.5,
            320,
            False,
            [
                choice(0, "重赏传颂", [Effect("gold", -15), Effect("prestige", 15)], 8),
                choice(1, "命其改稿", [Effect("gold", -5), Effect("prestige", 5)], 6),
                choice(2, "不予理会", [], 4),
            ],
            period="rose",
        ),
        EventDef(
            96,
            "敌营间谍",
            "你的密探发回情报：敌军即将集结于边境。",
            1.9,
            300,
            True,
            [
                choice(0, "抢先出击", [Effect("gold", -30), Effect("prestige", 20), Effect("health", -0.2)], 9),
                choice(1, "加强守备", [Effect("gold", -15), Effect("stress", 5)], 6),
                choice(2, "派使求和", [Effect("gold", -20), Effect("prestige", -5)], 4),
            ],
            period="rose",
        ),
        EventDef(
            97,
            "商队商人",
            "远道而来的商队请求在城内设立永久货栈。",
            1.7,
            360,
            False,
            [
                choice(0, "欢迎设栈", [Effect("gold", 20), Effect("prestige", 8)], 8),
                choice(1, "征收特许费", [Effect("gold", 30), Effect("prestige", 3)], 6),
                choice(2, "拒绝", [Effect("prestige", -5)], 4),
            ],
            period="rose",
        ),
        EventDef(
            98,
            "大教堂钟声",
            "新钟铸造完成，工匠请你在铭文上题字。",
            1.4,
            380,
            False,
            [
                choice(0, "题写自己名讳", [Effect("gold", -10), Effect("prestige", 12)], 8),
                choice(1, "题写祈愿", [Effect("gold", -10), Effect("piety", 10)], 6),
                choice(2, "任工匠自定", [], 4),
            ],
            period="rose",
        ),
        EventDef(
            99,
            "河流冰封",
            "严冬把河流冻成通途，敌军或许会踏冰来攻。",
            1.6,
            340,
            False,
            [
                choice(0, "破冰设障", [Effect("gold", -15), Effect("prestige", 8)], 8),
                choice(1, "加固渡口", [Effect("gold", -20), Effect("stress", 5)], 6),
                choice(2, "静观其变", [Effect("stress", 10)], 4),
            ],
            period="rose",
        ),
        EventDef(
            100,
            "玫瑰骑士",
            "一位神秘骑士自称为失落的王族后裔，请求效忠。",
            1.5,
            420,
            True,
            [
                choice(0, "收为亲随", [Effect("gold", -20), Effect("prestige", 15)], 8),
                choice(1, "求证血统", [Effect("stress", 8), Effect("prestige", 5)], 6),
                choice(2, "拒之门外", [Effect("stress", 5)], 4),
            ],
            period="rose",
        ),
        EventDef(
            101,
            "丰收感恩",
            "丰收在即，农夫请求举办感恩节庆。",
            2.0,
            240,
            False,
            [
                choice(0, "大办节庆", [Effect("gold", -20), Effect("prestige", 10), Effect("stress", -8)], 9),
                choice(1, "赏赐酒粮", [Effect("gold", -10), Effect("piety", 8)], 6),
                choice(2, "节省开支", [], 4),
            ],
        ),
        EventDef(
            102,
            "村井干涸",
            "连续干旱使村中水井见底，百姓忧心忡忡。",
            1.7,
            260,
            False,
            [
                choice(0, "拨款打井", [Effect("gold", -15), Effect("prestige", 8)], 8),
                choice(1, "引渠供水", [Effect("gold", -25), Effect("prestige", 5)], 6),
                choice(2, "听天由命", [Effect("stress", 8)], 4),
            ],
        ),
        EventDef(
            103,
            "野猪肆虐",
            "一头硕大野猪频繁毁坏庄稼，猎人束手无策。",
            1.4,
            300,
            False,
            [
                choice(0, "亲率围猎", [Effect("health", -0.2), Effect("prestige", 12)], 8),
                choice(1, "悬赏猎手", [Effect("gold", -8), Effect("prestige", 5)], 6),
                choice(2, "任其横行", [Effect("stress", 5)], 4),
            ],
        ),
        EventDef(
            104,
            "磨坊工匠",
            "老磨坊主声称发明了更省力的风车。",
            1.6,
            400,
            False,
            [
                choice(0, "资助建造", [Effect("gold", -30), Effect("prestige", 10)], 8),
                choice(1, "让各村自建", [Effect("gold", -5), Effect("prestige", 5)], 6),
                choice(2, "不以为意", [], 4),
            ],
        ),
        EventDef(
            105,
            "商旅住宿",
            "商人们在城中设宴，感谢你保护商路。",
            1.7,
            320,
            False,
            [
                choice(0, "出席宴席", [Effect("gold", -10), Effect("prestige", 10), Effect("stress", -5)], 8),
                choice(1, "派使赴宴", [Effect("prestige", 5)], 5),
                choice(2, "婉拒", [], 3),
            ],
        ),
        EventDef(
            106,
            "桥梁坍塌",
            "年久失修的石桥在暴雨中垮塌，阻断商路。",
            1.5,
            360,
            False,
            [
                choice(0, "重建石桥", [Effect("gold", -30), Effect("prestige", 10)], 9),
                choice(1, "架设浮桥", [Effect("gold", -15), Effect("prestige", 3)], 6),
                choice(2, "设置渡船", [Effect("gold", -8), Effect("prestige", 2)], 5),
            ],
        ),
        EventDef(
            107,
            "集市斗殴",
            "两个商贩因缺斤短两在集市大打出手。",
            1.4,
            260,
            False,
            [
                choice(0, "当众断案", [Effect("gold", -5), Effect("prestige", 8)], 8),
                choice(1, "各罚一笔", [Effect("gold", 10), Effect("prestige", 3)], 6),
                choice(2, "逐出集市", [Effect("stress", 3), Effect("prestige", 5)], 5),
            ],
        ),
        EventDef(
            108,
            "粮仓失火",
            "储粮的谷仓在深夜失火，火光冲天。",
            1.5,
            400,
            True,
            [
                choice(0, "连夜救火", [Effect("gold", -15), Effect("prestige", 8)], 8),
                choice(1, "严防哄抢", [Effect("gold", -10), Effect("stress", 10)], 6),
                choice(2, "追究守仓人", [Effect("stress", 5), Effect("prestige", -3)], 4),
            ],
        ),
        EventDef(
            109,
            "药草商贩",
            "一位游方药商兜售能治百病的草药。",
            1.3,
            320,
            False,
            [
                choice(0, "重金购买", [Effect("gold", -15), Effect("health", 0.2)], 7),
                choice(1, "试用一半", [Effect("gold", -8), Effect("health", 0.1)], 6),
                choice(2, "斥其行骗", [Effect("gold", 3), Effect("health", -0.1)], 4),
            ],
        ),
        EventDef(
            110,
            "远行信使",
            "一位信使带来家乡亲族的问候与一封家书。",
            1.2,
            500,
            False,
            [
                choice(0, "回赠厚礼", [Effect("gold", -10), Effect("prestige", 5)], 7),
                choice(1, "只回书信", [Effect("stress", -5)], 6),
                choice(2, "置之不理", [Effect("stress", 3)], 4),
            ],
        ),
        EventDef(
            111,
            "天鹅湖畔",
            "一场风暴后，湖面漂来一只受伤的白天鹅。",
            1.1,
            520,
            False,
            [
                choice(0, "悉心救治", [Effect("piety", 5), Effect("stress", -5)], 8),
                choice(1, "交给猎人处置", [Effect("gold", 2)], 5),
                choice(2, "不管不顾", [Effect("stress", 3)], 3),
            ],
        ),
        EventDef(
            112,
            "老兵归乡",
            "一位退伍的老兵返乡，请求在领地耕种养老。",
            1.5,
            300,
            False,
            [
                choice(0, "赐地养老", [Effect("gold", -10), Effect("prestige", 8)], 8),
                choice(1, "任用为守卫", [Effect("gold", -5), Effect("prestige", 5)], 6),
                choice(2, "打发走", [Effect("stress", 3)], 4),
            ],
        ),
        EventDef(
            113,
            "橡树林间",
            "护林人报告有人在林中偷伐橡树。",
            1.4,
            320,
            False,
            [
                choice(0, "严惩偷伐者", [Effect("gold", 8), Effect("prestige", 5)], 7),
                choice(1, "从轻发落", [Effect("piety", 5)], 5),
                choice(2, "加强巡林", [Effect("gold", -8), Effect("prestige", 3)], 6),
            ],
        ),
        EventDef(
            114,
            "晚祷钟声",
            "傍晚的钟声里，修道院的修士们为领地祈福。",
            1.3,
            480,
            False,
            [
                choice(0, "慷慨布施", [Effect("gold", -20), Effect("piety", 20)], 8),
                choice(1, "捐一袋粮", [Effect("gold", -8), Effect("piety", 8)], 6),
                choice(2, "路过而已", [Effect("piety", 2)], 4),
            ],
        ),
        EventDef(
            115,
            "集市珍宝",
            "一名老妇在集市出售祖传的古老饰物。",
            1.4,
            360,
            False,
            [
                choice(0, "高价买下", [Effect("gold", -12), Effect("prestige", 5)], 7),
                choice(1, "就地典当", [Effect("gold", 8)], 5),
                choice(2, "劝其留藏", [Effect("piety", 5)], 6),
            ],
        ),
        EventDef(
            116,
            "远山信烟",
            "山间升起信烟——邻村请求紧急支援。",
            1.5,
            380,
            True,
            [
                choice(0, "即刻驰援", [Effect("gold", -15), Effect("prestige", 12)], 9),
                choice(1, "先派斥候", [Effect("stress", 8)], 6),
                choice(2, "视而不见", [Effect("prestige", -10), Effect("stress", 5)], 4),
            ],
        ),
        EventDef(
            117,
            "丰收麦垛",
            "金黄的麦垛堆满晒场，农夫们唱着歌谣。",
            1.9,
            220,
            False,
            [
                choice(0, "下令免税一年", [Effect("gold", -20), Effect("prestige", 15), Effect("piety", 8)], 9),
                choice(1, "照常收税", [Effect("gold", 15), Effect("stress", 3)], 5),
                choice(2, "只免徭役", [Effect("gold", -8), Effect("prestige", 5)], 6),
            ],
        ),
        EventDef(
            118,
            "蜂蜜丰收",
            "养蜂人献来成桶的黄金般蜂蜜。",
            1.3,
            420,
            False,
            [
                choice(0, "分赏臣下", [Effect("prestige", 8), Effect("stress", -5)], 7),
                choice(1, "酿蜜酒宴客", [Effect("gold", -5), Effect("prestige", 10)], 6),
                choice(2, "卖出换钱", [Effect("gold", 12)], 5),
            ],
        ),
        EventDef(
            119,
            "古桥夜话",
            "守桥人在桥头发现一封署名为“夜鸦”的信。",
            1.4,
            400,
            True,
            [
                choice(0, "追查写信人", [Effect("stress", 8), Effect("prestige", 5)], 7),
                choice(1, "公开宣读", [Effect("prestige", 8)], 5),
                choice(2, "付之一炬", [Effect("stress", 3)], 5),
            ],
        ),
        EventDef(
            120,
            "集市日",
            "恰逢集市日，商贩、艺人挤满街道。",
            1.8,
            200,
            False,
            [
                choice(0, "亲临巡市", [Effect("gold", -5), Effect("prestige", 8), Effect("stress", -5)], 8),
                choice(1, "令人收市税", [Effect("gold", 15), Effect("prestige", 3)], 6),
                choice(2, "闭门不出", [Effect("stress", 3)], 4),
            ],
        ),
        EventDef(
            121,
            "炉边夜话",
            "冬夜炉火旁，老臣讲述先王征战的故事。",
            1.2,
            450,
            False,
            [
                choice(0, "聆听至深夜", [Effect("stress", -8), Effect("prestige", 5)], 7),
                choice(1, "问计治国", [Effect("prestige", 8), Effect("stress", 5)], 6),
                choice(2, "令其退下", [Effect("stress", 3)], 4),
            ],
        ),
        EventDef(
            122,
            "边界碑石",
            "护林人发现界碑被移动，邻国声称多占了田地。",
            1.7,
            320,
            False,
            [
                choice(0, "据理力争", [Effect("stress", 8), Effect("prestige", 8)], 8),
                choice(1, "恢复原界", [Effect("gold", -10), Effect("prestige", 5)], 6),
                choice(2, "搁置争议", [Effect("stress", 5)], 4),
            ],
        ),
        EventDef(
            123,
            "修道院访客",
            "一位云游修士带着一卷预言书请求解读。",
            1.5,
            400,
            False,
            [
                choice(0, "请学者解读", [Effect("gold", -10), Effect("piety", 10)], 7),
                choice(1, "亲自占卜", [Effect("piety", 5), Effect("stress", 5)], 5),
                choice(2, "斥为异端", [Effect("piety", -5), Effect("stress", 3)], 4),
            ],
        ),
        EventDef(
            124,
            "渔港夜灯",
            "渔港的灯塔年久失修，渔民请求修缮。",
            1.6,
            340,
            False,
            [
                choice(0, "拨款修缮", [Effect("gold", -20), Effect("prestige", 8)], 9),
                choice(1, "征民间工匠", [Effect("gold", -5), Effect("prestige", 3)], 6),
                choice(2, "暂缓", [Effect("stress", 5)], 4),
            ],
        ),
        EventDef(
            125,
            "盐路驼队",
            "一支盐路驼队抵达，盐价随之下跌。",
            1.5,
            360,
            False,
            [
                choice(0, "压低盐价购入", [Effect("gold", 15), Effect("prestige", 3)], 7),
                choice(1, "维持市价", [Effect("prestige", 5)], 5),
                choice(2, "囤积居奇", [Effect("gold", 25), Effect("prestige", -8)], 4),
            ],
        ),
        EventDef(
            126,
            "春日开犁",
            "开春时节，农夫们举行仪式祈求丰收。",
            1.4,
            400,
            False,
            [
                choice(0, "亲自扶犁", [Effect("prestige", 8), Effect("piety", 5)], 7),
                choice(1, "赐予种子", [Effect("gold", -10), Effect("prestige", 5)], 6),
                choice(2, "下令开工", [Effect("stress", 3)], 4),
            ],
        ),
        EventDef(
            127,
            "流民潮",
            "邻国战乱带来大批流民，聚集在城外。",
            1.8,
            300,
            True,
            [
                choice(0, "开门接纳", [Effect("gold", -20), Effect("prestige", 12)], 8),
                choice(1, "设营安置", [Effect("gold", -15), Effect("prestige", 8)], 6),
                choice(2, "拒绝入城", [Effect("prestige", -10), Effect("stress", 5)], 4),
            ],
        ),
        EventDef(
            128,
            "酒馆流言",
            "酒馆里流传着关于你身世的离奇传闻。",
            1.5,
            280,
            False,
            [
                choice(0, "一笑置之", [Effect("stress", -5)], 5),
                choice(1, "查明源头", [Effect("stress", 8), Effect("prestige", 3)], 7),
                choice(2, "下令禁谈", [Effect("prestige", -3), Effect("stress", 5)], 4),
            ],
        ),
        EventDef(
            129,
            "石匠大会",
            "各地石匠集会，展示新的拱券技艺。",
            1.3,
            420,
            False,
            [
                choice(0, "资助办学", [Effect("gold", -20), Effect("trait", 8), Effect("prestige", 8)], 8),
                choice(1, "只聘高手", [Effect("gold", -10), Effect("prestige", 3)], 6),
                choice(2, "不感兴趣", [], 4),
            ],
        ),
        EventDef(
            130,
            "夜枭报讯",
            "宫廷猎鹰带回一只脚环上有字的信鸽。",
            1.4,
            380,
            True,
            [
                choice(0, "解读密信", [Effect("stress", 8), Effect("prestige", 8)], 8),
                choice(1, "交予情报官", [Effect("gold", -5), Effect("prestige", 3)], 6),
                choice(2, "放其飞走", [Effect("stress", 5)], 4),
            ],
        ),
        EventDef(
            131,
            "教堂落成",
            "新建的教堂终于完工，钟声回荡山谷。",
            1.6,
            450,
            False,
            [
                choice(0, "亲自主持祝圣", [Effect("gold", -15), Effect("piety", 20), Effect("prestige", 8)], 9),
                choice(1, "委托主教", [Effect("gold", -8), Effect("piety", 10)], 6),
                choice(2, "只派代表", [Effect("piety", 3)], 4),
            ],
        ),
        EventDef(
            132,
            "古堡幽灵",
            "侍从们传言废弃塔楼里闹鬼，人心惶惶。",
            1.3,
            360,
            True,
            [
                choice(0, "亲自探访", [Effect("stress", 10), Effect("prestige", 8)], 6),
                choice(1, "请神父驱邪", [Effect("gold", -8), Effect("piety", 8)], 7),
                choice(2, "封锁塔楼", [Effect("stress", 3)], 5),
            ],
        ),
        EventDef(
            133,
            "葡萄丰收",
            "山坡上的葡萄成熟，酿出的酒香飘满庭院。",
            1.5,
            400,
            False,
            [
                choice(0, "酿成佳酿珍藏", [Effect("gold", -10), Effect("prestige", 8)], 7),
                choice(1, "售出获利", [Effect("gold", 15), Effect("prestige", 3)], 6),
                choice(2, "分给百姓", [Effect("prestige", 5), Effect("stress", -5)], 5),
            ],
        ),
        EventDef(
            134,
            "老臣告老",
            "效力多年的老臣请求告老还乡。",
            1.5,
            320,
            False,
            [
                choice(0, "厚赐金帛", [Effect("gold", -15), Effect("prestige", 10)], 8),
                choice(1, "挽留一年", [Effect("stress", 5)], 5),
                choice(2, "准其返乡", [Effect("prestige", 3)], 6),
            ],
        ),
        EventDef(
            135,
            "水磨坊旁",
            "水磨坊的齿轮卡住，磨坊主请求检修。",
            1.3,
            280,
            False,
            [
                choice(0, "拨款检修", [Effect("gold", -10), Effect("prestige", 5)], 8),
                choice(1, "让村民自修", [Effect("stress", 3)], 5),
                choice(2, "置若罔闻", [Effect("stress", 5)], 4),
            ],
        ),
        EventDef(
            136,
            "冬猎雪原",
            "雪后初晴，正适合围猎野鹿与野猪。",
            1.4,
            360,
            False,
            [
                choice(0, "亲率出猎", [Effect("health", -0.2), Effect("prestige", 10)], 7),
                choice(1, "派臣代猎", [Effect("gold", -5), Effect("prestige", 5)], 6),
                choice(2, "闭门休养", [Effect("stress", -5)], 5),
            ],
        ),
        EventDef(
            137,
            "铁匠铺",
            "老铁匠打造出一柄锋利的新式长剑。",
            1.4,
            400,
            False,
            [
                choice(0, "重金购下", [Effect("gold", -15), Effect("prestige", 5)], 7),
                choice(1, "订购一批", [Effect("gold", -20), Effect("prestige", 8)], 8),
                choice(2, "只看看", [], 4),
            ],
        ),
        EventDef(
            138,
            "秋收后",
            "秋收结束，百姓们举办篝火晚会。",
            1.6,
            300,
            False,
            [
                choice(0, "与民同乐", [Effect("gold", -8), Effect("prestige", 8), Effect("stress", -8)], 8),
                choice(1, "赐酒庆贺", [Effect("gold", -10), Effect("prestige", 5)], 6),
                choice(2, "回宫休息", [Effect("stress", 3)], 4),
            ],
        ),
        EventDef(
            139,
            "边境移民",
            "边境的荒地被新移民开垦，人口渐增。",
            1.5,
            380,
            False,
            [
                choice(0, "赐地鼓励", [Effect("gold", -15), Effect("prestige", 8)], 8),
                choice(1, "征收新税", [Effect("gold", 15), Effect("prestige", -3)], 5),
                choice(2, "不闻不问", [Effect("stress", 3)], 4),
            ],
        ),
        EventDef(
            140,
            "观星之夜",
            "夜观星象，宫廷占星家预言领地将有大事。",
            1.3,
            420,
            True,
            [
                choice(0, "听取详解", [Effect("stress", 5), Effect("prestige", 3)], 6),
                choice(1, "问计应对", [Effect("gold", -10), Effect("stress", -5)], 7),
                choice(2, "斥其迷信", [Effect("piety", -3)], 4),
            ],
        ),
        EventDef(
            141,
            "旧衣新织",
            "宫廷织女用旧衣料织出崭新挂毯。",
            1.1,
            460,
            False,
            [
                choice(0, "赏赐织女", [Effect("gold", -5), Effect("prestige", 5)], 7),
                choice(1, "悬挂厅堂", [Effect("prestige", 3)], 5),
                choice(2, "入库收藏", [Effect("prestige", 2)], 4),
            ],
        ),
        EventDef(
            142,
            "春汛",
            "融雪使河水暴涨，威胁沿河田地。",
            1.7,
            340,
            True,
            [
                choice(0, "组织筑堤", [Effect("gold", -25), Effect("prestige", 10)], 9),
                choice(1, "疏散村民", [Effect("gold", -10), Effect("prestige", 5)], 6),
                choice(2, "置之不理", [Effect("gold", -15), Effect("stress", 10)], 3),
            ],
        ),
        EventDef(
            143,
            "老树新芽",
            "百年的古树竟抽出新芽，被视为吉兆。",
            1.2,
            500,
            False,
            [
                choice(0, "设宴庆贺", [Effect("gold", -8), Effect("prestige", 5), Effect("stress", -5)], 7),
                choice(1, "献祭祈愿", [Effect("gold", -5), Effect("piety", 5)], 6),
                choice(2, "当作平常", [Effect("stress", 3)], 4),
            ],
        ),
        EventDef(
            144,
            "雨后彩虹",
            "暴雨过后，田野上空挂起双彩虹。",
            1.0,
            520,
            False,
            [
                choice(0, "与民共赏", [Effect("stress", -8), Effect("prestige", 3)], 7),
                choice(1, "独自观赏", [Effect("stress", -5)], 6),
                choice(2, "无暇顾及", [Effect("stress", 3)], 4),
            ],
        ),
        EventDef(
            145,
            "山货集市",
            "山民带来皮毛、松脂和药材赶集。",
            1.4,
            320,
            False,
            [
                choice(0, "收购山货", [Effect("gold", -15), Effect("prestige", 5)], 7),
                choice(1, "减免山货税", [Effect("gold", -5), Effect("prestige", 5)], 6),
                choice(2, "只收皮毛", [Effect("gold", 10)], 5),
            ],
        ),
        EventDef(
            146,
            "深夜灯火",
            "深夜宫廷灯火通明，侍从们仍在为明日大典忙碌。",
            1.2,
            440,
            False,
            [
                choice(0, "慰劳侍从", [Effect("gold", -5), Effect("prestige", 5), Effect("stress", -5)], 8),
                choice(1, "亲自督导", [Effect("stress", 8)], 5),
                choice(2, "回房安寝", [Effect("stress", -3)], 6),
            ],
        ),
    ]
