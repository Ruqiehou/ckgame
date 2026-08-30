"""玩法系统基类与目录条目。

玩法（Gameplay）是围绕角色与领地、可独立扩展的游戏机制（狩猎、养生等）。
每个玩法在 registry.py 中登记，由 GameplayManager 统一调度：
- 每月对每位统治者结算被动效果（tick_month）；
- 通过 GameAPI 暴露玩家主动操作（perform）；
- 通过 save_state / load_state 支持续档。
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Dict, List, Tuple


@dataclass
class GameplayEntry:
    """玩法目录条目（元数据，不含运行时状态）。"""

    id: str
    name: str              # 中文名称
    category: str          # 目录分类：生活 / 信仰 / 建设 …
    description: str
    status: str = "active"  # active=已实现，planned=规划中

    def to_dict(self) -> Dict[str, Any]:
        return {
            "id": self.id,
            "name": self.name,
            "category": self.category,
            "description": self.description,
            "status": self.status,
        }


class Gameplay:
    """玩法基类：子类实现月度结算与（可选）玩家操作。"""

    entry: GameplayEntry

    # ---------- 月度结算 ----------
    def tick_month(self, sim, ruler_id: int) -> None:
        """每月对单个统治者结算被动效果。"""

    # ---------- 玩家操作 ----------
    def actions(self) -> List[Dict[str, Any]]:
        """返回本玩法提供的玩家操作（供 UI 展示）。"""
        return []

    def can_perform(self, sim, who: int, op: str) -> Tuple[bool, str]:
        """检查操作是否可执行，返回 (是否可执行, 不可执行原因)。"""
        return False, f"未知操作: {op}"

    def perform(self, sim, who: int, op: str) -> str:
        """执行玩家操作，返回提示文本；不可执行时抛 ValueError。"""
        raise ValueError(f"未知操作: {op}")

    # ---------- 快照 / 存档 ----------
    def state(self, sim, who: int) -> Dict[str, Any]:
        """返回某角色在本玩法中的状态（供快照展示）。"""
        return {}

    def save_state(self) -> Dict[str, Any]:
        return {}

    def load_state(self, data: Dict[str, Any]) -> None:
        pass
