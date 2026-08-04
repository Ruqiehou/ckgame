"""建筑系统：管理省份建筑与升级。"""

from __future__ import annotations

from dataclasses import dataclass, field
from enum import Enum, auto
from typing import Dict, List, Optional


class BuildingKind(Enum):
    FARM = auto()           # 农田：增加税收
    BARRACKS = auto()       # 兵营：增加征兵上限
    WALLS = auto()          # 城墙：增加防御
    MARKET = auto()         # 市场：增加贸易收入
    CHURCH = auto()         # 教堂：增加稳定度
    WATCH_TOWER = auto()    # 瞭望塔：增加视野
    STABLE = auto()         # 马厩：增加骑兵效率
    WORKSHOP = auto()       # 工坊：增加生产效率

    def name_zh(self) -> str:
        return {
            BuildingKind.FARM: "农田",
            BuildingKind.BARRACKS: "兵营",
            BuildingKind.WALLS: "城墙",
            BuildingKind.MARKET: "市场",
            BuildingKind.CHURCH: "教堂",
            BuildingKind.WATCH_TOWER: "瞭望塔",
            BuildingKind.STABLE: "马厩",
            BuildingKind.WORKSHOP: "工坊",
        }[self]

    def description(self) -> str:
        return {
            BuildingKind.FARM: "增加省份税收收入",
            BuildingKind.BARRACKS: "增加征兵上限和训练速度",
            BuildingKind.WALLS: "增加守军数量和防御加成",
            BuildingKind.MARKET: "增加贸易和税收收入",
            BuildingKind.CHURCH: "增加稳定度和民众忠诚",
            BuildingKind.WATCH_TOWER: "增加视野范围，提前预警",
            BuildingKind.STABLE: "提升骑兵战斗效率",
            BuildingKind.WORKSHOP: "提升装备生产效率",
        }[self]

    def max_level(self) -> int:
        return {
            BuildingKind.FARM: 5,
            BuildingKind.BARRACKS: 3,
            BuildingKind.WALLS: 5,
            BuildingKind.MARKET: 3,
            BuildingKind.CHURCH: 3,
            BuildingKind.WATCH_TOWER: 2,
            BuildingKind.STABLE: 2,
            BuildingKind.WORKSHOP: 2,
        }[self]

    def upgrade_cost(self, level: int) -> int:
        """升级到下一级所需的金币。"""
        base = {
            BuildingKind.FARM: 50,
            BuildingKind.BARRACKS: 100,
            BuildingKind.WALLS: 80,
            BuildingKind.MARKET: 120,
            BuildingKind.CHURCH: 100,
            BuildingKind.WATCH_TOWER: 60,
            BuildingKind.STABLE: 150,
            BuildingKind.WORKSHOP: 100,
        }[self]
        return int(base * (1.5 ** level))

    def effect_value(self, level: int) -> float:
        """每级提供的效果值。"""
        return {
            BuildingKind.FARM: 5.0 * level,
            BuildingKind.BARRACKS: 20.0 * level,
            BuildingKind.WALLS: 15.0 * level,
            BuildingKind.MARKET: 8.0 * level,
            BuildingKind.CHURCH: 1.0 * level,
            BuildingKind.WATCH_TOWER: 2.0 * level,
            BuildingKind.STABLE: 0.1 * level,
            BuildingKind.WORKSHOP: 0.05 * level,
        }[self]


@dataclass
class CountyBuilding:
    kind: BuildingKind
    level: int = 0

    def can_upgrade(self) -> bool:
        return self.level < self.kind.max_level()

    def upgrade_cost(self) -> int:
        return self.kind.upgrade_cost(self.level)


@dataclass
class BuildingSystem:
    buildings: Dict[int, List[CountyBuilding]] = field(default_factory=dict)
    # county_id -> List[CountyBuilding]

    def get_buildings(self, county_id: int) -> List[CountyBuilding]:
        return self.buildings.get(county_id, [])

    def get_building(self, county_id: int, kind: BuildingKind) -> Optional[CountyBuilding]:
        for b in self.get_buildings(county_id):
            if b.kind == kind:
                return b
        return None

    def add_building(self, county_id: int, kind: BuildingKind) -> CountyBuilding:
        b = CountyBuilding(kind=kind, level=0)
        self.buildings.setdefault(county_id, []).append(b)
        return b

    def upgrade(self, county_id: int, kind: BuildingKind) -> bool:
        b = self.get_building(county_id, kind)
        if b and b.can_upgrade():
            b.level += 1
            return True
        return False

    def get_effects(self, county_id: int) -> Dict[str, float]:
        """获取省份所有建筑的效果总和。"""
        effects: Dict[str, float] = {
            "tax_income": 0.0,
            "levy_max": 0.0,
            "garrison": 0.0,
            "defense": 0.0,
            "stability": 0.0,
            "vision": 0.0,
            "cavalry_bonus": 0.0,
            "production": 0.0,
        }
        for b in self.get_buildings(county_id):
            if b.kind == BuildingKind.FARM:
                effects["tax_income"] += b.kind.effect_value(b.level)
            elif b.kind == BuildingKind.BARRACKS:
                effects["levy_max"] += b.kind.effect_value(b.level)
            elif b.kind == BuildingKind.WALLS:
                effects["garrison"] += b.kind.effect_value(b.level)
                effects["defense"] += b.kind.effect_value(b.level) * 0.5
            elif b.kind == BuildingKind.MARKET:
                effects["tax_income"] += b.kind.effect_value(b.level)
            elif b.kind == BuildingKind.CHURCH:
                effects["stability"] += b.kind.effect_value(b.level)
            elif b.kind == BuildingKind.WATCH_TOWER:
                effects["vision"] += b.kind.effect_value(b.level)
            elif b.kind == BuildingKind.STABLE:
                effects["cavalry_bonus"] += b.kind.effect_value(b.level)
            elif b.kind == BuildingKind.WORKSHOP:
                effects["production"] += b.kind.effect_value(b.level)
        return effects
