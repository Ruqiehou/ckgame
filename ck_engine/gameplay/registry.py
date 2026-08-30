"""玩法目录：集中注册所有玩法。

新增玩法的步骤见 docs/python/gameplay.md：
1. 在本包下新建模块，实现 Gameplay 子类；
2. 在 GAMEPLAYS 中登记 (entry, 构造器)；仅规划中的玩法放入 PLANNED_GAMEPLAYS；
3. 若有运行时状态，实现 save_state / load_state（存档会自动带上 gameplays 键）。
"""

from __future__ import annotations

from typing import Any, Callable, Dict, List, Tuple

from ck_engine.gameplay.base import Gameplay, GameplayEntry
from ck_engine.gameplay.hunting import HuntingGameplay
from ck_engine.gameplay.wellness import WellnessGameplay

# 已实现玩法目录：(条目, 构造器)
GAMEPLAYS: List[Tuple[GameplayEntry, Callable[[], Gameplay]]] = [
    (HuntingGameplay.entry, HuntingGameplay),
    (WellnessGameplay.entry, WellnessGameplay),
]

# 规划中的玩法（尚未实现，仅目录占位）
PLANNED_GAMEPLAYS: List[GameplayEntry] = [
    GameplayEntry(
        id="pilgrimage",
        name="朝圣",
        category="信仰",
        description="远赴圣地朝拜，换取虔诚与声望，路途或有风险。",
        status="planned",
    ),
    GameplayEntry(
        id="monument",
        name="营造奇观",
        category="建设",
        description="投入金币营造传世建筑，持续提供威望与领地繁荣。",
        status="planned",
    ),
]


def catalog() -> List[Dict[str, Any]]:
    """返回完整玩法目录（含规划中条目）。"""
    rows = [entry.to_dict() for entry, _factory in GAMEPLAYS]
    rows.extend(entry.to_dict() for entry in PLANNED_GAMEPLAYS)
    return rows


def create_active() -> List[Gameplay]:
    """实例化所有已实现玩法。"""
    return [factory() for _entry, factory in GAMEPLAYS]
