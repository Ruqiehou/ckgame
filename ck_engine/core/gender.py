"""性别枚举。

独立成模块，供 politics.laws 与 world.character 共用，避免跨包循环导入。
"""

from __future__ import annotations

from enum import auto, Enum


class Gender(Enum):
    MALE = auto()
    FEMALE = auto()
