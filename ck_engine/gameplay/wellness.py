"""养生玩法。

领主可聘请宫廷医师调理身体：支付聘礼后按月付薪，
医师每月帮助恢复少量健康、纾解压力；付不出薪水时医师会离开。
"""

from __future__ import annotations

from typing import Any, Dict, List, Tuple

from ck_engine.gameplay.base import Gameplay, GameplayEntry

HIRE_FEE = 30        # 聘礼（一次性）
MONTHLY_SALARY = 10  # 月薪
HEALTH_RECOVERY = 0.2
STRESS_RELIEF = 3
HEALTH_CAP = 10.0


class WellnessGameplay(Gameplay):
    entry = GameplayEntry(
        id="wellness",
        name="养生",
        category="生活",
        description="聘请宫廷医师调理身体：按月付薪，缓慢恢复健康、纾解压力；欠薪则医师出走。",
    )

    def __init__(self) -> None:
        self.physicians: Dict[int, bool] = {}  # ruler_id -> 是否聘有医师

    # ---------- 月度结算 ----------
    def tick_month(self, sim, ruler_id: int) -> None:
        if not self.physicians.get(ruler_id):
            return
        c = sim.world.character(ruler_id)
        if not c or not c.is_alive():
            self.physicians[ruler_id] = False
            return
        if c.gold < MONTHLY_SALARY:
            self.physicians[ruler_id] = False
            sim.world.push_log(f"{c.name} 付不起医师薪水，医师离开了宫廷")
            return
        c.add_gold(-MONTHLY_SALARY)
        c.health = min(HEALTH_CAP, c.health + HEALTH_RECOVERY)
        c.add_stress(-STRESS_RELIEF)

    # ---------- 玩家操作 ----------
    def actions(self) -> List[Dict[str, Any]]:
        return [
            {
                "op": "hire_physician",
                "name": "聘请宫廷医师",
                "desc": f"支付聘礼 {HIRE_FEE:.0f} 金，此后每月付薪 {MONTHLY_SALARY:.0f} 金，恢复健康、纾解压力",
                "cost_gold": HIRE_FEE,
            },
            {
                "op": "dismiss_physician",
                "name": "辞退医师",
                "desc": "不再续聘医师，停止支付月薪",
            },
        ]

    def can_perform(self, sim, who: int, op: str) -> Tuple[bool, str]:
        c = sim.world.character(who)
        if not c or not c.is_alive():
            return False, "角色无效"
        if op == "hire_physician":
            if not c.is_ruler:
                return False, "仅领主可聘请医师"
            if not c.is_adult(sim.world.date):
                return False, "未成年"
            if self.physicians.get(who):
                return False, "已有医师在宫中"
            if c.gold < HIRE_FEE:
                return False, f"金币不足（需 {HIRE_FEE:.0f}）"
            return True, ""
        if op == "dismiss_physician":
            if not self.physicians.get(who):
                return False, "宫中没有医师"
            return True, ""
        return False, f"未知操作: {op}"

    def perform(self, sim, who: int, op: str) -> str:
        ok, reason = self.can_perform(sim, who, op)
        if not ok:
            raise ValueError(reason)
        c = sim.world.character(who)
        if op == "hire_physician":
            c.add_gold(-HIRE_FEE)
            self.physicians[who] = True
            sim.world.push_log(f"{c.name} 聘请了宫廷医师")
            return f"已聘请宫廷医师（每月薪水 {MONTHLY_SALARY:.0f} 金）"
        self.physicians[who] = False
        sim.world.push_log(f"{c.name} 辞退了宫廷医师")
        return "已辞退医师"

    # ---------- 快照 / 存档 ----------
    def state(self, sim, who: int) -> Dict[str, Any]:
        return {
            "has_physician": bool(self.physicians.get(who)),
            "monthly_salary": MONTHLY_SALARY,
        }

    def save_state(self) -> Dict[str, Any]:
        return {"physicians": [k for k, v in self.physicians.items() if v]}

    def load_state(self, data: Dict[str, Any]) -> None:
        self.physicians = {
            int(rid): True for rid in data.get("physicians", [])
        }
