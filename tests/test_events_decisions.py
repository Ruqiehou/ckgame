"""事件目录与重大决策的完整性/执行测试。"""

from __future__ import annotations

import unittest

from ck_engine.events.engine import builtin_events
from ck_engine.game.simulation import GameSimulation
from ck_engine.politics.decisions import BUILTIN_DECISIONS


class EventsCatalogTest(unittest.TestCase):
    def test_event_ids_are_unique(self) -> None:
        ids = [e.id for e in builtin_events()]
        self.assertEqual(len(ids), len(set(ids)), "事件 id 必须唯一")

    def test_new_events_present(self) -> None:
        ids = {e.id for e in builtin_events()}
        for i in (33, 34, 35, 36):
            self.assertIn(i, ids)


class DecisionsTest(unittest.TestCase):
    def test_new_decisions_present(self) -> None:
        ids = {d.id for d in BUILTIN_DECISIONS}
        self.assertIn("royal_progress", ids)
        self.assertIn("grant_charter", ids)

    def test_royal_progress_executes(self) -> None:
        sim = GameSimulation()
        rulers = list(sim.world.rulers())
        self.assertTrue(rulers)
        r = rulers[0]
        c = sim.world.character(r.id)
        self.assertIsNotNone(c)
        c.add_gold(500)
        before = c.prestige
        ok = sim.decisions.execute("royal_progress", sim.world, r.id)
        self.assertTrue(ok)
        self.assertGreater(c.prestige, before)

    def test_grant_charter_executes(self) -> None:
        sim = GameSimulation()
        rulers = list(sim.world.rulers())
        self.assertTrue(rulers)
        r = rulers[0]
        c = sim.world.character(r.id)
        self.assertIsNotNone(c)
        c.add_gold(500)
        ok = sim.decisions.execute("grant_charter", sim.world, r.id)
        self.assertTrue(ok)


if __name__ == "__main__":
    unittest.main()
