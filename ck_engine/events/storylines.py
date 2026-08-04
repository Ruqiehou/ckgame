"""剧情线系统：管理长期剧情弧。"""

from __future__ import annotations

from dataclasses import dataclass, field
from enum import Enum, auto
from typing import List, Optional


class StorylineStatus(Enum):
    AVAILABLE = auto()
    ACTIVE = auto()
    COMPLETED = auto()
    FAILED = auto()


@dataclass
class StorylineStage:
    stage_id: int
    title: str
    description: str
    event_ids: List[int] = field(default_factory=list)
    next_stage: Optional[int] = None
    requires_condition: Optional[str] = None


@dataclass
class Storyline:
    id: int
    title: str
    description: str
    character_id: int
    status: StorylineStatus = StorylineStatus.AVAILABLE
    current_stage: int = 0
    stages: List[StorylineStage] = field(default_factory=list)
    start_year: int = 0
    end_year: int = 0
    tags: List[str] = field(default_factory=list)

    def is_active(self) -> bool:
        return self.status == StorylineStatus.ACTIVE

    def is_completed(self) -> bool:
        return self.status == StorylineStatus.COMPLETED

    def is_failed(self) -> bool:
        return self.status == StorylineStatus.FAILED


@dataclass
class StorylineSystem:
    storylines: List[Storyline] = field(default_factory=list)
    active_per_character: dict = field(default_factory=dict)

    def create_storyline(self, storyline: Storyline) -> None:
        self.storylines.append(storyline)

    def start_storyline(self, character_id: int, storyline_id: int) -> Optional[Storyline]:
        for s in self.storylines:
            if s.id == storyline_id and s.character_id == character_id:
                if s.status == StorylineStatus.AVAILABLE:
                    s.status = StorylineStatus.ACTIVE
                    s.current_stage = 0
                    self.active_per_character[character_id] = s
                    return s
        return None

    def advance_stage(self, character_id: int) -> Optional[Storyline]:
        storyline = self.active_per_character.get(character_id)
        if not storyline or not storyline.is_active():
            return None
        current = next((s for s in storyline.stages if s.stage_id == storyline.current_stage), None)
        if current and current.next_stage is not None:
            next_stage = next((s for s in storyline.stages if s.stage_id == current.next_stage), None)
            if next_stage:
                storyline.current_stage = next_stage.stage_id
                return storyline
        return None

    def complete_storyline(self, character_id: int) -> Optional[Storyline]:
        storyline = self.active_per_character.get(character_id)
        if storyline:
            storyline.status = StorylineStatus.COMPLETED
            return storyline
        return None

    def fail_storyline(self, character_id: int) -> Optional[Storyline]:
        storyline = self.active_per_character.get(character_id)
        if storyline:
            storyline.status = StorylineStatus.FAILED
            return storyline
        return None

    def get_active_storyline(self, character_id: int) -> Optional[Storyline]:
        return self.active_per_character.get(character_id)

    def get_available_storylines(self, character_id: int) -> List[Storyline]:
        return [s for s in self.storylines if s.character_id == character_id and s.status == StorylineStatus.AVAILABLE]


def builtin_storylines() -> List[Storyline]:
    """返回 10 条预设剧情线。"""
    return [
        # 1. 继承危机
        Storyline(
            id=1,
            title="继承危机",
            description="你发现远亲对你的领位有野心，必须稳固继承权。",
            character_id=0,  # 0 表示任意角色
            start_year=1066,
            end_year=1070,
            tags=["政治", "继承"],
            stages=[
                StorylineStage(stage_id=0, title="风声鹤唳", description="密探报告远亲在拉拢封臣。", event_ids=[16]),
                StorylineStage(stage_id=1, title="拉拢封臣", description="你需要争取关键封臣的支持。", event_ids=[5]),
                StorylineStage(stage_id=2, title="最后通牒", description="远亲公开提出 claim，你必须做出回应。", event_ids=[24]),
                StorylineStage(stage_id=3, title="尘埃落定", description="危机解除或爆发战争。", event_ids=[]),
            ],
        ),
        # 2. 宗教狂热
        Storyline(
            id=2,
            title="宗教狂热",
            description="教会请求你支持一场圣战，这将考验你的虔诚与外交。",
            character_id=0,
            start_year=1066,
            end_year=1072,
            tags=["宗教", "战争"],
            stages=[
                StorylineStage(stage_id=0, title="教皇的号召", description="教皇来信要求你参与圣战。", event_ids=[4]),
                StorylineStage(stage_id=1, title="备战", description="你需要筹集资金和军队。", event_ids=[22]),
                StorylineStage(stage_id=2, title="圣战开始", description="军队集结完毕，开赴前线。", event_ids=[]),
                StorylineStage(stage_id=3, title="凯旋或陨落", description="战争结束，你获得了荣耀或教训。", event_ids=[]),
            ],
        ),
        # 3. 商路崛起
        Storyline(
            id=3,
            title="商路崛起",
            description="商会提议开辟一条横跨大陆的商路，这将带来巨大财富。",
            character_id=0,
            start_year=1066,
            end_year=1075,
            tags=["经济", "贸易"],
            stages=[
                StorylineStage(stage_id=0, title="商机初现", description="商会代表带来商路计划。", event_ids=[13]),
                StorylineStage(stage_id=1, title="投资建设", description="你需要投入大量资金建设商路。", event_ids=[25]),
                StorylineStage(stage_id=2, title="商路开通", description="商路正式开通，财富开始流入。", event_ids=[]),
                StorylineStage(stage_id=3, title="垄断之争", description="其他领主试图抢夺商路控制权。", event_ids=[]),
            ],
        ),
        # 4. 宫廷阴谋
        Storyline(
            id=4,
            title="宫廷阴谋",
            description="你发现宫廷中有人密谋反对你，必须找出内鬼。",
            character_id=0,
            start_year=1066,
            end_year=1069,
            tags=["阴谋", "宫廷"],
            stages=[
                StorylineStage(stage_id=0, title="蛛丝马迹", description="你收到匿名警告信。", event_ids=[14]),
                StorylineStage(stage_id=1, title="暗中调查", description="你派出密探调查宫廷异动。", event_ids=[29]),
                StorylineStage(stage_id=2, title="阴谋败露", description="你发现了叛徒的身份。", event_ids=[24]),
                StorylineStage(stage_id=3, title="清洗或宽恕", description="你必须决定如何处理叛徒。", event_ids=[]),
            ],
        ),
        # 5. 联姻外交
        Storyline(
            id=5,
            title="联姻外交",
            description="通过联姻巩固与邻国的关系，但政治婚姻往往充满变数。",
            character_id=0,
            start_year=1066,
            end_year=1070,
            tags=["外交", "婚姻"],
            stages=[
                StorylineStage(stage_id=0, title="求婚", description="你收到了一位贵族的求婚。", event_ids=[20]),
                StorylineStage(stage_id=1, title="婚礼", description="盛大的婚礼即将举行。", event_ids=[]),
                StorylineStage(stage_id=2, title="婚后风波", description="婚姻生活中出现波折。", event_ids=[6]),
                StorylineStage(stage_id=3, title="联盟稳固", description="联姻带来了预期的政治利益。", event_ids=[]),
            ],
        ),
        # 6. 瘟疫蔓延
        Storyline(
            id=6,
            title="瘟疫蔓延",
            description="一场可怕的瘟疫席卷你的领地，你必须做出艰难抉择。",
            character_id=0,
            start_year=1066,
            end_year=1068,
            tags=["灾难", "瘟疫"],
            stages=[
                StorylineStage(stage_id=0, title="疫情初现", description="边境村落出现不明疫病。", event_ids=[8]),
                StorylineStage(stage_id=1, title="封锁还是救济", description="你必须决定如何应对疫情。", event_ids=[27]),
                StorylineStage(stage_id=2, title="疫情高峰", description="瘟疫达到顶峰，大量人口死亡。", event_ids=[]),
                StorylineStage(stage_id=3, title="疫情结束", description="瘟疫终于过去，但留下了深刻教训。", event_ids=[]),
            ],
        ),
        # 7. 比武大会
        Storyline(
            id=7,
            title="比武大会",
            description="领地举办盛大的比武大会，各地骑士云集，这是展示武勋的绝佳机会。",
            character_id=0,
            start_year=1066,
            end_year=1067,
            tags=["军事", "荣誉"],
            stages=[
                StorylineStage(stage_id=0, title="筹备", description="你开始筹备比武大会。", event_ids=[28]),
                StorylineStage(stage_id=1, title="比赛日", description="骑士们展开激烈角逐。", event_ids=[]),
                StorylineStage(stage_id=2, title="决赛", description="决赛在两位最强骑士之间展开。", event_ids=[]),
                StorylineStage(stage_id=3, title="加冕", description="冠军产生，你授予其荣誉。", event_ids=[]),
            ],
        ),
        # 8. 外敌入侵
        Storyline(
            id=8,
            title="外敌入侵",
            description="强大的外敌入侵你的边境，你必须组织防御。",
            character_id=0,
            start_year=1066,
            end_year=1068,
            tags=["战争", "防御"],
            stages=[
                StorylineStage(stage_id=0, title="警报", description="边境传来敌军入侵的消息。", event_ids=[22]),
                StorylineStage(stage_id=1, title="动员", description="你召集封臣准备防御。", event_ids=[]),
                StorylineStage(stage_id=2, title="决战", description="两军在主战场相遇。", event_ids=[]),
                StorylineStage(stage_id=3, title="战后", description="战争结束，你评估损失与收获。", event_ids=[]),
            ],
        ),
        # 9. 文化繁荣
        Storyline(
            id=9,
            title="文化繁荣",
            description="你的宫廷成为文化中心，吸引学者和艺术家前来。",
            character_id=0,
            start_year=1066,
            end_year=1075,
            tags=["文化", "发展"],
            stages=[
                StorylineStage(stage_id=0, title="学者来访", description="一位著名学者来到你的宫廷。", event_ids=[10]),
                StorylineStage(stage_id=1, title="赞助艺术", description="你决定赞助艺术家和建筑师。", event_ids=[15]),
                StorylineStage(stage_id=2, title="文化繁荣", description="你的宫廷成为文化中心。", event_ids=[]),
                StorylineStage(stage_id=3, title="遗产", description="你留下了不朽的文化遗产。", event_ids=[]),
            ],
        ),
        # 10. 王朝崛起
        Storyline(
            id=10,
            title="王朝崛起",
            description="你的王朝正在崛起，你需要通过联姻、战争和外交扩张势力。",
            character_id=0,
            start_year=1066,
            end_year=1080,
            tags=["王朝", "扩张"],
            stages=[
                StorylineStage(stage_id=0, title="家族壮大", description="你开始规划家族的扩张。", event_ids=[20]),
                StorylineStage(stage_id=1, title="第一场战争", description="你发动了第一场扩张战争。", event_ids=[]),
                StorylineStage(stage_id=2, title="联姻巩固", description="通过联姻巩固新获得的领土。", event_ids=[6]),
                StorylineStage(stage_id=3, title="王朝建立", description="你的王朝已经崛起为强大势力。", event_ids=[]),
            ],
        ),
    ]
