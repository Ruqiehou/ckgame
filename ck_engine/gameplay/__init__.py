"""玩法目录：狩猎、养生等可扩展玩法系统。

对外导出 Gameplay / GameplayEntry / GameplayManager 与目录查询（catalog）。
"""

from ck_engine.gameplay.base import Gameplay, GameplayEntry
from ck_engine.gameplay.manager import GameplayManager
from ck_engine.gameplay.registry import catalog, create_active

__all__ = [
    "Gameplay",
    "GameplayEntry",
    "GameplayManager",
    "catalog",
    "create_active",
]
