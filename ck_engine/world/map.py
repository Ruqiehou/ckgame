from __future__ import annotations

from collections import deque
from dataclasses import dataclass, field
from enum import Enum, auto
from typing import Dict, Iterator, List, Optional, Set

from ck_engine.core import NONE_ID


class Terrain(Enum):
    PLAINS = auto()
    HILLS = auto()
    MOUNTAINS = auto()
    FOREST = auto()
    DESERT = auto()
    WETLAND = auto()
    FARMLAND = auto()
    COASTAL = auto()

    def supply_limit(self) -> float:
        return {
            Terrain.PLAINS: 1.0,
            Terrain.HILLS: 0.8,
            Terrain.MOUNTAINS: 0.5,
            Terrain.FOREST: 0.7,
            Terrain.DESERT: 0.4,
            Terrain.WETLAND: 0.6,
            Terrain.FARMLAND: 1.3,
            Terrain.COASTAL: 1.1,
        }[self]

    def combat_width(self) -> float:
        return {
            Terrain.PLAINS: 1.0,
            Terrain.FARMLAND: 1.0,
            Terrain.HILLS: 0.8,
            Terrain.FOREST: 0.8,
            Terrain.MOUNTAINS: 0.6,
            Terrain.WETLAND: 0.6,
            Terrain.DESERT: 0.9,
            Terrain.COASTAL: 0.95,
        }[self]

    def development_cap(self) -> int:
        return {
            Terrain.FARMLAND: 100,
            Terrain.PLAINS: 80,
            Terrain.COASTAL: 80,
            Terrain.HILLS: 60,
            Terrain.FOREST: 60,
            Terrain.WETLAND: 50,
            Terrain.DESERT: 40,
            Terrain.MOUNTAINS: 40,
        }[self]


@dataclass
class County:
    id: int
    name: str
    terrain: Terrain
    key: str = ""
    development: int = 10
    control: float = 100.0
    prosperity: float = 50.0
    culture: int = 0
    faith: int = 0
    owner_title: int = NONE_ID
    holder: int = NONE_ID
    fort_level: int = 1
    buildings: List[str] = field(default_factory=list)
    levies: int = 200
    tax: float = 1.0
    neighbors: List[int] = field(default_factory=list)
    trade_route_protected: bool = False  # 商路保护
    trade_route_protection_level: int = 0  # 保护等级
    trade_route_maintenance_cost: float = 0.0  # 维护成本
    has_port: bool = False  # 是否有港口
    port_level: int = 0  # 港口等级
    port_income: float = 0.0  # 港口收入
    trade_route_maintenance_level: int = 0  # 贸易路线维护等级
    trade_route_upgrade_cost: float = 0.0  # 升级成本

    @staticmethod
    def new(county_id: int, name: str, terrain: Terrain) -> County:
        return County(id=county_id, name=name, terrain=terrain)

    def monthly_tax(self) -> float:
        base = self.tax * (1.0 + self.development * 0.02)
        return base * (self.control / 100.0) * self.terrain.supply_limit()

    def upgrade_trade_route_maintenance(self) -> float:
        """升级贸易路线维护等级"""
        if self.trade_route_maintenance_level >= 5:
            return 0.0  # 已达最高级
        
        self.trade_route_maintenance_level += 1
        self.trade_route_upgrade_cost = 100.0 * self.trade_route_maintenance_level
        self.trade_route_maintenance_cost = 10.0 * self.trade_route_maintenance_level
        
        # 维护等级提升减少维护成本
        return self.trade_route_upgrade_cost

    def get_trade_route_maintenance_bonus(self) -> float:
        """获取贸易路线维护加成"""
        return 1.0 - (self.trade_route_maintenance_level * 0.1)  # 每级减少10%维护成本

    def monthly_levies(self) -> int:
        base = self.levies * (1.0 + self.development * 0.01)
        return int(base * (self.control / 100.0))

    def upkeep(self) -> float:
        cost = 0.0
        for b in self.buildings:
            if b == "城堡":
                cost += 0.5
            elif b == "市场":
                cost += 0.3
            elif b == "庄园":
                cost += 0.2
        return cost

    def calculate_port_income(self) -> float:
        """计算港口收入"""
        if not self.has_port:
            return 0.0
        
        # 港口收入基于港口等级和省份发展度
        base_income = 50.0 * self.port_level
        development_bonus = 1.0 + (self.development / 100.0)
        return base_income * development_bonus

    def upgrade_port(self) -> float:
        """升级港口"""
        if self.port_level >= 5:
            return 0.0  # 已达最高级
        
        self.port_level += 1
        upgrade_cost = 200.0 * self.port_level
        self.port_income = self.calculate_port_income()
        
        return upgrade_cost


@dataclass
class MapGraph:
    counties: Dict[int, County] = field(default_factory=dict)
    county_by_key: Dict[str, County] = field(default_factory=dict)

    def insert(self, county: County) -> None:
        self.counties[county.id] = county
        self.county_by_key[county.name.lower()] = county

    def get(self, county_id: int) -> Optional[County]:
        return self.counties.get(county_id)

    def get_by_key(self, key: str) -> Optional[County]:
        return self.county_by_key.get(key.lower())

    def connect(self, a: int, b: int) -> None:
        ca, cb = self.counties.get(a), self.counties.get(b)
        if ca and b not in ca.neighbors:
            ca.neighbors.append(b)
        if cb and a not in cb.neighbors:
            cb.neighbors.append(a)

    def path(self, frm: int, to: int) -> Optional[List[int]]:
        if frm == to:
            return [frm]
        visited: Set[int] = {frm}
        queue = deque([frm])
        parent: Dict[int, int] = {}
        while queue:
            cur = queue.popleft()
            county = self.counties.get(cur)
            if not county:
                continue
            for n in county.neighbors:
                if n in visited:
                    continue
                visited.add(n)
                parent[n] = cur
                if n == to:
                    path = [to]
                    p = n
                    while p in parent:
                        prev = parent[p]
                        path.append(prev)
                        if prev == frm:
                            break
                        p = prev
                    path.reverse()
                    return path
                queue.append(n)
        return None

    def iter(self) -> Iterator[County]:
        return iter(self.counties.values())
