"""狩猎玩法。

领主组织围猎：消耗金币、缓解压力、赢得威望，
秋季猎物肥美收获最丰；运气差时也可能坠马受伤。
每月结算时冷却递减，AI 君主在秋季也会自发组织狩猎。
"""

from __future__ import annotations

import random
from typing import Any, Dict, List, Tuple

from ck_engine.core import Season
from ck_engine.gameplay.base import Gameplay, GameplayEntry

COST_GOLD = 60          # 组织一次狩猎的花费
COOLDOWN_MONTHS = 3     # 两次狩猎的最短间隔（月）
STRESS_RELIEF = 20      # 基础减压
INJURY_CHANCE = 0.10    # 坠马受伤概率
BIG_GAME_CHANCE = 0.15  # 猎获大型猎物的概率

AI_COST_GOLD = 40
AI_HUNT_CHANCE = 0.08   # AI 君主每月秋季自发狩猎的概率


class HuntingGameplay(Gameplay):
    entry = GameplayEntry(
        id="hunting",
        name="狩猎",
        category="生活",
        description="组织围猎消遣：缓解压力、赢得威望；秋季收获最丰，但可能坠马受伤。",
    )

    def __init__(self) -> None:
        self.cooldowns: Dict[int, int] = {}  # ruler_id -> 剩余冷却月数

    # ---------- 月度结算 ----------
    def tick_month(self, sim, ruler_id: int) -> None:
        left = self.cooldowns.get(ruler_id, 0)
        if left > 0:
            self.cooldowns[ruler_id] = left - 1
        c = sim.world.character(ruler_id)
        if (
            not c
            or not c.is_alive()
            or ruler_id in sim.player_ids
            or sim.world.date.season() != Season.AUTUMN
            or random.random() > AI_HUNT_CHANCE
        ):
            return
        if c.gold < AI_COST_GOLD:
            return
        c.add_gold(-AI_COST_GOLD)
        c.add_stress(-10)
        c.add_prestige(5)
        sim.world.push_log(f"{c.name} 组织了秋狩，纵马围猎")

    # ---------- 玩家操作 ----------
    def actions(self) -> List[Dict[str, Any]]:
        return [
            {
                "op": "organize",
                "name": "组织狩猎",
                "desc": f"花费 {COST_GOLD:.0f} 金举办围猎，缓解压力、赢得威望",
                "cost_gold": COST_GOLD,
                "cooldown_months": COOLDOWN_MONTHS,
            }
        ]

    def can_perform(self, sim, who: int, op: str) -> Tuple[bool, str]:
        if op != "organize":
            return False, f"未知操作: {op}"
        c = sim.world.character(who)
        if not c or not c.is_alive():
            return False, "角色无效"
        if not c.is_ruler:
            return False, "仅领主可组织狩猎"
        if not c.is_adult(sim.world.date):
            return False, "未成年"
        if self.cooldowns.get(who, 0) > 0:
            return False, f"猎场尚未休整（还需 {self.cooldowns[who]} 个月）"
        if c.gold < COST_GOLD:
            return False, f"金币不足（需 {COST_GOLD:.0f}）"
        return True, ""

    def perform(self, sim, who: int, op: str) -> str:
        ok, reason = self.can_perform(sim, who, op)
        if not ok:
            raise ValueError(reason)
        c = sim.world.character(who)
        c.add_gold(-COST_GOLD)
        c.add_stress(-STRESS_RELIEF)

        season = sim.world.date.season()
        attrs = sim.world.effective_attrs(who)
        prestige = 10.0
        if season == Season.AUTUMN:
            prestige += 8.0
        if attrs:
            prestige += attrs.martial / 4.0

        injured = random.random() < INJURY_CHANCE
        big_game = not injured and random.random() < BIG_GAME_CHANCE
        if big_game:
            prestige += 15.0
        c.add_prestige(prestige)
        if injured:
            c.health -= 1.0
            c.add_stress(5)

        self.cooldowns[who] = COOLDOWN_MONTHS
        sim.world.push_log(f"{c.name} 组织了狩猎，纵马围猎")

        msg = f"狩猎完成：威望 +{prestige:.0f}，压力 -{STRESS_RELIEF}"
        if big_game:
            msg += "，猎获巨鹿！"
        if injured:
            msg += "，不幸坠马受伤（健康 -1）"
        return msg

    # ---------- 快照 / 存档 ----------
    def state(self, sim, who: int) -> Dict[str, Any]:
        return {"cooldown_months": self.cooldowns.get(who, 0)}

    def save_state(self) -> Dict[str, Any]:
        return {"cooldowns": {str(k): v for k, v in self.cooldowns.items()}}

    def load_state(self, data: Dict[str, Any]) -> None:
        self.cooldowns = {
            int(k): int(v) for k, v in data.get("cooldowns", {}).items()
        }
