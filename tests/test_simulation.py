"""主循环稳定性测试：按日/按月推进不崩溃，状态保持一致。"""

from __future__ import annotations

import random
import unittest

from ck_engine.game.simulation import GameSimulation


class SimulationTickTest(unittest.TestCase):
    def setUp(self) -> None:
        random.seed(42)

    def test_run_days_advances_date_and_tick(self) -> None:
        sim = GameSimulation()
        start = sim.world.date
        sim.run_days(70)
        self.assertEqual(sim.world.tick, 70)
        elapsed_months = (sim.world.date.year - start.year) * 12 + (
            sim.world.date.month - start.month
        )
        self.assertGreaterEqual(elapsed_months, 2)

    def test_quarter_of_stable_simulation(self) -> None:
        sim = GameSimulation()
        sim.run_days(92)  # 3 个多月，覆盖至少 3 次月度结算
        rulers = list(sim.world.rulers())
        self.assertGreaterEqual(len(rulers), 1)
        for r in rulers:
            self.assertIsNotNone(sim.world.character(r.id))
        # 月度结算后法律与内阁仍覆盖所有统治者
        for r in rulers:
            self.assertIn(r.id, sim.realm_laws)
            self.assertIsNotNone(sim.councils.get(r.id))

    def test_month_start_triggers_monthly_settlement(self) -> None:
        sim = GameSimulation()
        start_month = sim.world.date.month
        for _ in range(40):
            if sim.world.date.day == 1 and sim.world.tick > 0:
                break
            sim.tick_day()
        self.assertEqual(sim.world.date.day, 1)
        self.assertNotEqual(sim.world.date.month, start_month)


if __name__ == "__main__":
    unittest.main()
