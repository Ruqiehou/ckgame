"""教程系统：引导新玩家了解游戏。"""

from __future__ import annotations

from dataclasses import dataclass, field
from enum import Enum, auto
from typing import List, Optional


class TutorialStep(Enum):
    WELCOME = auto()
    SELECT_COUNTY = auto()
    VIEW_CHARACTER = auto()
    RAISE_ARMY = auto()
    MOVE_ARMY = auto()
    DECLARE_WAR = auto()
    BUILD_BUILDING = auto()
    START_SCHEME = auto()
    APPOINT_COUNCIL = auto()
    SAVE_GAME = auto()

    def name_zh(self) -> str:
        return {
            TutorialStep.WELCOME: "欢迎",
            TutorialStep.SELECT_COUNTY: "选择省份",
            TutorialStep.VIEW_CHARACTER: "查看角色",
            TutorialStep.RAISE_ARMY: "征召军队",
            TutorialStep.MOVE_ARMY: "移动军队",
            TutorialStep.DECLARE_WAR: "宣战",
            TutorialStep.BUILD_BUILDING: "建造建筑",
            TutorialStep.START_SCHEME: "开始阴谋",
            TutorialStep.APPOINT_COUNCIL: "任命内阁",
            TutorialStep.SAVE_GAME: "保存游戏",
        }[self]


@dataclass
class TutorialStepDef:
    step: TutorialStep
    title: str
    description: str
    hint: str
    action_hint: Optional[str] = None


@dataclass
class TutorialSystem:
    current_step: TutorialStep = TutorialStep.WELCOME
    completed_steps: List[TutorialStep] = field(default_factory=list)
    enabled: bool = True

    def get_current_step(self) -> Optional[TutorialStepDef]:
        if not self.enabled:
            return None
        steps = BUILTIN_TUTORIAL_STEPS
        for s in steps:
            if s.step == self.current_step:
                return s
        return None

    def advance(self, step: TutorialStep) -> None:
        if step == self.current_step:
            self.completed_steps.append(step)
            # 移动到下一步
            steps = [s.step for s in BUILTIN_TUTORIAL_STEPS]
            idx = steps.index(step)
            if idx + 1 < len(steps):
                self.current_step = steps[idx + 1]
            else:
                self.current_step = step  # 已完成所有步骤

    def reset(self) -> None:
        self.current_step = TutorialStep.WELCOME
        self.completed_steps.clear()

    def is_completed(self) -> bool:
        return len(self.completed_steps) >= len(BUILTIN_TUTORIAL_STEPS)


BUILTIN_TUTORIAL_STEPS: List[TutorialStepDef] = [
    TutorialStepDef(
        step=TutorialStep.WELCOME,
        title="欢迎来到王国风云",
        description="这是一款中世纪领主模拟游戏。作为一方领主，你需要管理领地、发展经济、进行外交和战争。",
        hint="点击地图上的省份可以查看详情。",
        action_hint="select_county",
    ),
    TutorialStepDef(
        step=TutorialStep.SELECT_COUNTY,
        title="选择你的领地",
        description="首先，选择你的一块领地查看详情。了解领地的收入、防御和建筑情况。",
        hint="点击地图上属于你的省份（显示你的颜色）。",
        action_hint="select_county",
    ),
    TutorialStepDef(
        step=TutorialStep.VIEW_CHARACTER,
        title="查看角色信息",
        description="在右侧面板可以查看你的角色属性、金币、威望等信息。这些属性会影响你的决策。",
        hint="查看右侧的角色信息面板。",
        action_hint="view_character",
    ),
    TutorialStepDef(
        step=TutorialStep.RAISE_ARMY,
        title="征召军队",
        description="要扩张领土，你需要军队。选择你的领地，点击'征召军队'按钮。",
        hint="选择你的省份，然后点击'征召军队'。",
        action_hint="raise_army",
    ),
    TutorialStepDef(
        step=TutorialStep.MOVE_ARMY,
        title="移动军队",
        description="军队征召完成后，你可以命令他们移动到目标省份。",
        hint="选择你的军队，然后点击目标省份。",
        action_hint="move_army",
    ),
    TutorialStepDef(
        step=TutorialStep.DECLARE_WAR,
        title="发动战争",
        description="有了军队，你可以对其他国家宣战。选择敌方省份，点击'对领主宣战'。",
        hint="选择敌方省份，点击宣战按钮。",
        action_hint="declare_war",
    ),
    TutorialStepDef(
        step=TutorialStep.BUILD_BUILDING,
        title="建造建筑",
        description="建筑可以增加领地收入、防御等。选择你的省份，选择建筑类型并建造。",
        hint="选择你的省份，使用建筑下拉框建造建筑。",
        action_hint="build_building",
    ),
    TutorialStepDef(
        step=TutorialStep.START_SCHEME,
        title="开始阴谋",
        description="除了正面战争，你还可以使用阴谋手段。在阴谋面板选择目标和类型。",
        hint="切换到阴谋面板，选择目标和阴谋类型。",
        action_hint="start_scheme",
    ),
    TutorialStepDef(
        step=TutorialStep.APPOINT_COUNCIL,
        title="任命内阁",
        description="内阁成员可以帮你处理各种事务。在内阁面板任命你的顾问。",
        hint="切换到内阁面板，任命顾问。",
        action_hint="appoint_council",
    ),
    TutorialStepDef(
        step=TutorialStep.SAVE_GAME,
        title="保存游戏",
        description="最后，记得保存你的进度！在存档面板输入存档名并保存。",
        hint="切换到存档面板，输入名称并保存。",
        action_hint="save_game",
    ),
]
