"""玩法调度：统一驱动各玩法的月度结算、玩家操作与存档。

由 GameSimulation 持有（sim.gameplays），每月结算时依统治者逐个调用各玩法；
玩家操作经 GameAPI 的 gameplay_action 分发到对应玩法。
"""

from __future__ import annotations

from typing import Any, Dict, List

from ck_engine.gameplay.base import Gameplay
from ck_engine.gameplay.registry import catalog, create_active


class GameplayManager:
    def __init__(self) -> None:
        self.systems: Dict[str, Gameplay] = {}
        for gp in create_active():
            self.systems[gp.entry.id] = gp

    # ---------- 月度结算 ----------
    def tick_month(self, sim) -> None:
        for rid in [r.id for r in sim.world.rulers()]:
            for gp in self.systems.values():
                gp.tick_month(sim, rid)

    # ---------- 目录 / 快照 ----------
    def catalog(self) -> List[Dict[str, Any]]:
        return catalog()

    def player_state(self, sim, who: int) -> List[Dict[str, Any]]:
        """返回某角色在各玩法中的状态与可用操作（供 UI）。"""
        out: List[Dict[str, Any]] = []
        for gp in self.systems.values():
            out.append(
                {
                    "id": gp.entry.id,
                    "name": gp.entry.name,
                    "description": gp.entry.description,
                    "state": gp.state(sim, who),
                    "actions": gp.actions(),
                }
            )
        return out

    # ---------- 玩家操作 ----------
    def perform(self, sim, who: int, gameplay_id: str, op: str) -> str:
        gp = self.systems.get(gameplay_id)
        if not gp:
            raise ValueError(f"未知玩法: {gameplay_id}")
        return gp.perform(sim, who, op)

    # ---------- 存档 ----------
    def save_state(self) -> Dict[str, Any]:
        return {gid: gp.save_state() for gid, gp in self.systems.items()}

    def load_state(self, data: Dict[str, Any]) -> None:
        for gid, gp in self.systems.items():
            gp.load_state(data.get(gid, {}))
