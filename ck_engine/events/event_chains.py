"""历史事件链系统。

多阶段的剧情线（诺曼征服、继承危机、王朝奠基等），
每个阶段有触发条件、可选分支和后续影响。
"""

from __future__ import annotations

import copy
from dataclasses import dataclass, field
from typing import Callable, Dict, List, Optional, Tuple

from ck_engine.core import NONE_ID, GameDate


@dataclass
class ChainStage:
    """事件链的一个阶段。"""
    id: str
    title: str
    description: str
    weight: float = 1.0
    days_to_next: int = 180  # 自动推进到下一阶段所需天数
    choices: List = field(default_factory=list)  # 与 EventChoice 同构
    on_enter: Optional[Callable] = None
    on_timeout: Optional[Callable] = None


@dataclass
class EventChain:
    """一条事件链。"""
    id: str
    title: str
    description: str
    stages: List[ChainStage] = field(default_factory=list)
    participants: List[int] = field(default_factory=list)
    start_date: Optional[GameDate] = None
    current_stage: int = 0
    active: bool = False
    completed: bool = False
    checker: Optional[Callable] = None  # 是否可触发

    def can_trigger(self, world, who: int) -> bool:
        if self.active or self.completed:
            return False
        if self.checker:
            return self.checker(world, who)
        return True

    def start(self, world, who: int) -> None:
        self.active = True
        self.start_date = world.date
        self.participants = [who]
        self.current_stage = 0
        if self.stages and self.stages[0].on_enter:
            self.stages[0].on_enter(world, who)

    def advance(self, world, who: int) -> None:
        if not self.active or self.completed:
            return
        if self.current_stage >= len(self.stages) - 1:
            self.complete(world, who)
            return
        self.current_stage += 1
        stage = self.stages[self.current_stage]
        if stage.on_enter:
            stage.on_enter(world, who)

    def complete(self, world, who: int) -> None:
        self.active = False
        self.completed = True
        world.push_log(f"事件链「{self.title}」完成")


# ---------- 内置事件链 ----------

def _norman_conquest_chain() -> EventChain:
    """诺曼征服：英格兰的权力更迭。"""
    def check(world, who: int) -> bool:
        c = world.character(who)
        if not c or not c.is_ruler:
            return False
        # 英格兰地区存在且玩家为诺曼/法兰西文化
        return c.culture in (0, 1)

    def stage1_enter(world, who: int) -> None:
        world.push_log("【诺曼征服】诺曼底公爵的使者抵达宫廷，商议联姻与继承安排。")

    def stage2_enter(world, who: int) -> None:
        world.push_log("【诺曼征服】英格兰国王无嗣而终，王位继承陷入纷争。")

    def stage3_enter(world, who: int) -> None:
        world.push_log("【诺曼征服】诺曼军队跨海东征，征服之战爆发！")

    return EventChain(
        id="norman_conquest",
        title="诺曼征服",
        description="英格兰王位更迭，诺曼底公爵跨海争夺王位。",
        checker=check,
        stages=[
            ChainStage(
                id="nc_1",
                title="联姻与盟约",
                description="诺曼底公爵的使者来访，商议联姻以巩固联盟。",
                days_to_next=365,
                on_enter=stage1_enter,
            ),
            ChainStage(
                id="nc_2",
                title="王位空虚",
                description="英格兰国王骤逝，王位继承悬而未决，群雄逐鹿。",
                days_to_next=365,
                on_enter=stage2_enter,
            ),
            ChainStage(
                id="nc_3",
                title="跨海征服",
                description="诺曼大军跨海东征，黑斯廷斯决战在即。",
                days_to_next=730,
                on_enter=stage3_enter,
            ),
        ],
    )


def _succession_crisis_chain() -> EventChain:
    """继承危机：君主年老，继承人弱势。"""
    def check(world, who: int) -> bool:
        c = world.character(who)
        if not c or not c.is_ruler:
            return False
        if c.age_at(world.date) < 45:
            return False
        heirs = [ch for ch in c.children if world.character(ch) and world.character(ch).is_alive()]
        if not heirs:
            return True
        heir = world.character(heirs[0])
        # 继承人能力平庸且年龄小
        attrs = world.effective_attrs(heir.id)
        if attrs and (attrs.diplomacy + attrs.martial + attrs.stewardship) < 12:
            return True
        return False

    def stage1_enter(world, who: int) -> None:
        world.push_log("【继承危机】年迈的君主健康恶化，继承安排迫在眉睫。")

    def stage2_enter(world, who: int) -> None:
        world.push_log("【继承危机】权臣结党，意图架空幼主。")

    def stage3_enter(world, who: int) -> None:
        world.push_log("【继承危机】各地领主拥兵自重，内战阴云密布。")

    return EventChain(
        id="succession_crisis",
        title="继承危机",
        description="君主年老，继承人弱势，王朝面临分裂危机。",
        checker=check,
        stages=[
            ChainStage(
                id="sc_1",
                title="君主病笃",
                description="君主健康每况愈下，需尽快确定继承人。",
                days_to_next=180,
                on_enter=stage1_enter,
            ),
            ChainStage(
                id="sc_2",
                title="权臣窥伺",
                description="朝中权臣结党，图谋不轨。",
                days_to_next=365,
                on_enter=stage2_enter,
            ),
            ChainStage(
                id="sc_3",
                title="群雄割据",
                description="各地领主自立，内战一触即发。",
                days_to_next=540,
                on_enter=stage3_enter,
            ),
        ],
    )


def _dynasty_founding_chain() -> EventChain:
    """王朝奠基：首任君主创立新王朝。"""
    def check(world, who: int) -> bool:
        c = world.character(who)
        if not c or not c.is_ruler:
            return False
        # 玩家为新王朝的创立者（无父无母，或父辈非统治者）
        if c.father != NONE_ID:
            return False
        if c.is_ruler and c.age_at(world.date) >= 30:
            return True
        return False

    def stage1_enter(world, who: int) -> None:
        world.push_log("【王朝奠基】你白手起家，创立了属于自己的王朝。")

    def stage2_enter(world, who: int) -> None:
        world.push_log("【王朝奠基】王朝初立，需确立继承法与典章制度。")

    def stage3_enter(world, who: int) -> None:
        world.push_log("【王朝奠基】王朝稳固，青史留名。")

    return EventChain(
        id="dynasty_founding",
        title="王朝奠基",
        description="白手起家，创立传世王朝。",
        checker=check,
        stages=[
            ChainStage(
                id="df_1",
                title="白手起家",
                description="你从卑微中崛起，王朝的基石由你奠定。",
                days_to_next=730,
                on_enter=stage1_enter,
            ),
            ChainStage(
                id="df_2",
                title="制度初立",
                description="王朝初具规模，需确立继承与法律。",
                days_to_next=1095,
                on_enter=stage2_enter,
            ),
            ChainStage(
                id="df_3",
                title="青史留名",
                description="王朝稳固，你的名字被后人传颂。",
                days_to_next=1460,
                on_enter=stage3_enter,
            ),
        ],
    )


BUILTIN_CHAINS: List[EventChain] = [
    _norman_conquest_chain(),
    _succession_crisis_chain(),
    _dynasty_founding_chain(),
]


@dataclass
class ChainEngine:
    chains: List[EventChain] = field(default_factory=list)
    active_chains: List[EventChain] = field(default_factory=list)
    history: List[str] = field(default_factory=list)

    def __post_init__(self) -> None:
        if not self.chains:
            self.chains = [c for c in BUILTIN_CHAINS]

    def check_triggers(self, world, who: int) -> List[EventChain]:
        """检查并触发可触发的事件链。"""
        triggered = []
        for chain in self.chains:
            if chain.active or chain.completed:
                continue
            if chain.can_trigger(world, who):
                chain.start(world, who)
                self.active_chains.append(chain)
                triggered.append(chain)
        return triggered

    def tick(self, world, who: int) -> None:
        """推进活跃事件链。"""
        still_active = []
        for chain in self.active_chains:
            if chain.completed:
                continue
            # 每月检查一次推进
            if world.date.day == 1:
                chain.advance(world, who)
            if not chain.completed:
                still_active.append(chain)
                if chain.completed:
                    self.history.append(f"{world.date}: 事件链「{chain.title}」完成")
        self.active_chains = still_active