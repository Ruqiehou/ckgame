"""重大决策系统。

君主可执行的战略级决策（加冕、迁都、圣战、改革等）。
决策有代价/前提/效果，执行后进入冷却并触发事件链。
"""

from __future__ import annotations

from dataclasses import dataclass, field
from enum import Enum, auto
from typing import Callable, Dict, List, Optional, Tuple

from ck_engine.core import NONE_ID, GameDate


class DecisionCategory(Enum):
    DYNASTY = auto()      # 王朝相关
    TERRITORY = auto()    # 领土相关
    WAR = auto()          # 战争相关
    FAITH = auto()        # 信仰相关
    COURT = auto()        # 宫廷相关


@dataclass
class DecisionEffect:
    """决策效果（与事件共用 Effect 语义，避免循环依赖）。"""
    kind: str
    amount: float = 0.0
    text: str = ""

    def apply(self, world, who: int) -> None:
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
class Decision:
    id: str
    title: str
    category: DecisionCategory
    description: str
    cost_gold: float = 0.0
    cost_prestige: float = 0.0
    cooldown_years: int = 5
    requires_ruler: bool = True
    requires_adult: bool = True
    requires_duchy: bool = False   # 至少需公爵头衔
    requires_kingdom: bool = False # 至少需国王头衔
    min_gold: float = 0.0
    min_prestige: float = 0.0
    effects: List[DecisionEffect] = field(default_factory=list)
    log_text: str = ""
    # 可选：额外的自定义前提检查（返回 (ok, reason)）
    checker: Optional[Callable] = None

    def can_execute(self, world, who: int) -> Tuple[bool, str]:
        c = world.character(who)
        if not c or not c.is_alive():
            return False, "君主无效"
        if self.requires_ruler and not c.is_ruler:
            return False, "非领主"
        if self.requires_adult and not c.is_adult(world.date):
            return False, "未成年"
        if c.gold < self.min_gold:
            return False, f"金币不足（需 {self.min_gold:.0f}）"
        if c.prestige < self.min_prestige:
            return False, f"威望不足（需 {self.min_prestige:.0f}）"
        if self.requires_duchy and not _has_title_tier(world, who, 2):
            return False, "需公爵及以上头衔"
        if self.requires_kingdom and not _has_title_tier(world, who, 3):
            return False, "需国王及以上头衔"
        if self.checker:
            ok, reason = self.checker(world, who)
            if not ok:
                return False, reason
        return True, ""

    def execute(self, world, who: int) -> None:
        c = world.character(who)
        if not c:
            return
        if self.cost_gold:
            c.add_gold(-self.cost_gold)
        if self.cost_prestige:
            c.add_prestige(-self.cost_prestige)
        for eff in self.effects:
            eff.apply(world, who)
        if self.log_text:
            world.push_log(self.log_text)


def _has_title_tier(world, who: int, tier: int) -> bool:
    c = world.character(who)
    if not c:
        return False
    for tid in c.held_titles:
        t = world.title(tid)
        if t and t.tier >= tier:
            return True
    return False


# ---------- 内置决策 ----------

def _check_has_duchy(world, who: int) -> Tuple[bool, str]:
    if _has_title_tier(world, who, 2):
        return True, ""
    return False, "需至少一个公爵头衔"


def _check_has_multiple_counties(world, who: int) -> Tuple[bool, str]:
    c = world.character(who)
    if not c:
        return False, "君主无效"
    counties = []
    for tid in c.held_titles:
        t = world.title(tid)
        if t and t.tier == 1:
            counties.extend(t.counties)
    if len(set(counties)) >= 2:
        return True, ""
    return False, "需至少持有两处领地"


def _check_has_kingdom(world, who: int) -> Tuple[bool, str]:
    if _has_title_tier(world, who, 3):
        return True, ""
    return False, "需国王头衔"


def _check_christian(world, who: int) -> Tuple[bool, str]:
    c = world.character(who)
    if c and c.faith in (0, 1):  # 0/1 视为基督教系
        return True, ""
    return False, "该决策仅限基督教君主"


def _exec_declare_kingdom(world, who: int) -> None:
    """加冕为王：将主头衔提升为王国级（若已有公爵头衔）。"""
    c = world.character(who)
    if not c:
        return
    # 找最高公爵头衔并尝试升级为王国
    ducal = None
    for tid in c.held_titles:
        t = world.title(tid)
        if t and t.tier == 2:
            ducal = t
            break
    if ducal:
        ducal.tier = 3
        ducal.name = f"{ducal.name}王国"
        world.push_log(f"{c.name} 加冕为王，「{ducal.name}」升格为王国")


def _exec_move_capital(world, who: int) -> None:
    """迁都：将主头衔的首都移至持有郡中发展度最高者。"""
    c = world.character(who)
    if not c or c.primary_title == NONE_ID:
        return
    t = world.title(c.primary_title)
    if not t or not t.counties:
        return
    best = max(t.counties, key=lambda cid: world.map.get(cid).development if world.map.get(cid) else 0)
    t.capital = best
    world.push_log(f"{c.name} 迁都至 {world.map.get(best).name if world.map.get(best) else best}")


def _exec_call_crusade(world, who: int) -> None:
    """号召圣战：对随机邻国发动圣战。"""
    from ck_engine.politics.diplomacy import CasusBelli
    c = world.character(who)
    if not c:
        return
    # 找一个敌对或邻近的统治者
    targets = [r for r in world.rulers() if r.id != who]
    if not targets:
        return
    tgt = random.choice(targets)
    world.push_log(f"{c.name} 号召十字军，向 {world.character(tgt.id).name if world.character(tgt.id) else tgt.id} 宣战！")


def _exec_hold_tournament(world, who: int) -> None:
    c = world.character(who)
    if not c:
        return
    c.add_prestige(50)
    c.add_stress(-10)
    world.push_log(f"{c.name} 举办盛大比武大会，名望飙升")


def _exec_reform_faith(world, who: int) -> None:
    c = world.character(who)
    if not c:
        return
    c.piety += 100
    c.add_prestige(50)
    world.push_log(f"{c.name} 推动信仰改革，虔诚度大幅提升")


def _exec_restore_empire(world, who: int) -> None:
    c = world.character(who)
    if not c:
        return
    # 将最高头衔升为帝国级
    king = None
    for tid in c.held_titles:
        t = world.title(tid)
        if t and t.tier == 3:
            king = t
            break
    if king:
        king.tier = 4
        king.name = f"{king.name}帝国"
        c.add_prestige(200)
        world.push_log(f"{c.name} 恢复帝国！「{king.name}」")


BUILTIN_DECISIONS: List[Decision] = [
    Decision(
        id="declare_kingdom",
        title="加冕为王",
        category=DecisionCategory.DYNASTY,
        description="将公爵头衔升格为王国，建立自己的王朝。",
        cost_gold=500,
        cost_prestige=200,
        cooldown_years=10,
        requires_duchy=True,
        min_gold=500,
        min_prestige=200,
        effects=[DecisionEffect("log", text="加冕为王")],
        log_text="加冕为王，王朝建立",
        checker=_check_has_duchy,
        execute=_exec_declare_kingdom,
    ),
    Decision(
        id="move_capital",
        title="迁都",
        category=DecisionCategory.TERRITORY,
        description="将主头衔的首都迁至更富庶的领地。",
        cost_gold=200,
        cooldown_years=5,
        min_gold=200,
        requires_duchy=True,
        effects=[],
        log_text="迁都完成",
        checker=_check_has_multiple_counties,
        execute=_exec_move_capital,
    ),
    Decision(
        id="call_crusade",
        title="号召圣战",
        category=DecisionCategory.WAR,
        description="发动一场圣战，征服异教徒的领地。",
        cost_gold=300,
        cost_prestige=100,
        cooldown_years=10,
        min_gold=300,
        min_prestige=300,
        min_piety=200,
        requires_kingdom=True,
        effects=[],
        log_text="号令十字军",
        checker=_check_christian,
        execute=_exec_call_crusade,
    ),
    Decision(
        id="hold_tournament",
        title="举办比武大会",
        category=DecisionCategory.COURT,
        description="在领内举办盛大的比武大会，提升名望与士气。",
        cost_gold=100,
        cooldown_years=2,
        min_gold=100,
        effects=[],
        log_text="举办比武大会",
        execute=_exec_hold_tournament,
    ),
    Decision(
        id="reform_faith",
        title="改革信仰",
        category=DecisionCategory.FAITH,
        description="推动教义改革，提升虔诚与威望。",
        cost_gold=200,
        cost_prestige=100,
        cooldown_years=20,
        min_gold=200,
        min_prestige=200,
        min_piety=300,
        requires_kingdom=True,
        effects=[],
        log_text="推动信仰改革",
        execute=_exec_reform_faith,
    ),
    Decision(
        id="restore_empire",
        title="重建帝国",
        category=DecisionCategory.DYNASTY,
        description="将王国头衔升格为帝国，重现先祖荣光。",
        cost_gold=1000,
        cost_prestige=500,
        cooldown_years=20,
        min_gold=1000,
        min_prestige=500,
        requires_kingdom=True,
        effects=[],
        log_text="帝国重建",
        execute=_exec_restore_empire,
    ),
]


@dataclass
class DecisionEngine:
    catalog: List[Decision] = field(default_factory=list)
    cooldowns: Dict[str, int] = field(default_factory=dict)
    history: List[str] = field(default_factory=list)

    def __post_init__(self) -> None:
        if not self.catalog:
            self.catalog = list(BUILTIN_DECISIONS)

    def available(self, world, who: int) -> List[Tuple[Decision, str]]:
        """返回可执行的决策及其原因。"""
        result = []
        for d in self.catalog:
            if d.id in self.cooldowns and self.cooldowns[d.id] > world.date.year:
                continue
            ok, reason = d.can_execute(world, who)
            if ok:
                result.append((d, ""))
            else:
                result.append((d, reason))
        return result

    def execute(self, decision_id: str, world, who: int) -> bool:
        d = next((x for x in self.catalog if x.id == decision_id), None)
        if not d:
            return False
        ok, _ = d.can_execute(world, who)
        if not ok:
            return False
        d.execute(world, who)
        self.cooldowns[decision_id] = world.date.year + d.cooldown_years
        self.history.append(f"{world.date}: {who} 执行决策「{d.title}」")
        return True


# 供决策使用的随机数（由 simulation 注入，避免直接 import random 造成耦合）
import random  # noqa: E402