"""事件目录、重大决策与事件链的完整性/执行测试。"""

from __future__ import annotations

import random
import unittest

from ck_engine.events.engine import builtin_events
from ck_engine.events.event_chains import BUILTIN_CHAINS, ChainEngine, builtin_chains
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


class EventChainsTest(unittest.TestCase):
    def test_chain_ids_unique(self) -> None:
        ids = [c.id for c in builtin_chains()]
        self.assertEqual(len(ids), len(set(ids)), "事件链 id 必须唯一")

    def test_new_chains_present(self) -> None:
        ids = {c.id for c in builtin_chains()}
        self.assertIn("pretender_rising", ids)
        self.assertIn("great_famine", ids)

    def test_pretender_chain_triggers_for_weak_ruler(self) -> None:
        random.seed(42)
        sim = GameSimulation()
        rulers = list(sim.world.rulers())
        r = rulers[0]
        c = sim.world.character(r.id)
        # 削弱君主使其满足触发条件
        c.prestige = 50
        engine = ChainEngine(builtin_chains())
        triggered = engine.check_triggers(sim.world, r.id)
        self.assertTrue(any(ch.id == "pretender_rising" for ch in triggered))
        chain = next(ch for ch in engine.chains if ch.id == "pretender_rising")
        self.assertTrue(chain.active)
        self.assertIsNotNone(chain.stage_started)

    def test_chain_stage_advances_after_days(self) -> None:
        random.seed(42)
        sim = GameSimulation()
        rulers = list(sim.world.rulers())
        r = rulers[0]
        c = sim.world.character(r.id)
        c.prestige = 50
        engine = ChainEngine(builtin_chains())
        engine.check_triggers(sim.world, r.id)
        chain = next(ch for ch in engine.chains if ch.id == "pretender_rising")
        self.assertEqual(chain.current_stage, 0)
        # 推进 200 天（超过第一阶段 180 天），阶段应推进
        sim.run_days(200)
        engine.tick(sim.world, r.id)
        self.assertEqual(chain.current_stage, 1)


if __name__ == "__main__":
    unittest.main()
