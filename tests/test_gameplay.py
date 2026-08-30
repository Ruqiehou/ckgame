"""玩法目录测试：目录注册、狩猎与养生玩法、快照与存档。"""

from __future__ import annotations

import random
import tempfile
import unittest
from pathlib import Path

from ck_engine.game.simulation import GameSimulation
from ck_engine.gameplay.registry import catalog, create_active
from ck_engine.ui.api import GameAPI


def _first_ruler(sim: GameSimulation):
    return next(iter(sim.world.rulers()))


class GameplayCatalogTest(unittest.TestCase):
    def test_catalog_contains_unique_ids(self) -> None:
        rows = catalog()
        ids = [r["id"] for r in rows]
        self.assertEqual(len(ids), len(set(ids)))
        self.assertIn("hunting", ids)
        self.assertIn("wellness", ids)

    def test_active_systems_match_catalog(self) -> None:
        systems = create_active()
        self.assertGreaterEqual(len(systems), 2)
        for gp in systems:
            self.assertEqual(gp.entry.status, "active")

    def test_simulation_monthly_settlement_runs_gameplays(self) -> None:
        random.seed(11)
        sim = GameSimulation()
        sim.run_days(35)  # 至少一次月度结算，玩法结算不应报错
        self.assertIsNotNone(sim.gameplays.systems["hunting"])
        self.assertIsNotNone(sim.gameplays.systems["wellness"])


class HuntingTest(unittest.TestCase):
    def setUp(self) -> None:
        random.seed(7)
        self.sim = GameSimulation()
        self.rid = _first_ruler(self.sim).id
        self.hunting = self.sim.gameplays.systems["hunting"]

    def test_organize_hunt_costs_gold_and_sets_cooldown(self) -> None:
        c = self.sim.world.character(self.rid)
        c.gold = 500
        c.stress = 200
        msg = self.sim.gameplays.perform(self.sim, self.rid, "hunting", "organize")
        self.assertIn("狩猎", msg)
        self.assertEqual(c.gold, 440)
        self.assertLess(c.stress, 200)
        self.assertEqual(
            self.hunting.state(self.sim, self.rid)["cooldown_months"], 3
        )

    def test_hunt_blocked_by_cooldown_then_gold(self) -> None:
        c = self.sim.world.character(self.rid)
        c.gold = 500
        self.sim.gameplays.perform(self.sim, self.rid, "hunting", "organize")
        with self.assertRaises(ValueError):
            self.sim.gameplays.perform(self.sim, self.rid, "hunting", "organize")
        self.hunting.cooldowns[self.rid] = 0
        c.gold = 0
        with self.assertRaises(ValueError):
            self.sim.gameplays.perform(self.sim, self.rid, "hunting", "organize")

    def test_cooldown_decays_monthly(self) -> None:
        self.sim.gameplays.perform(self.sim, self.rid, "hunting", "organize")
        self.hunting.cooldowns[self.rid] = 1
        self.hunting.tick_month(self.sim, self.rid)
        self.assertEqual(self.hunting.cooldowns[self.rid], 0)


class WellnessTest(unittest.TestCase):
    def setUp(self) -> None:
        random.seed(7)
        self.sim = GameSimulation()
        self.rid = _first_ruler(self.sim).id
        self.wellness = self.sim.gameplays.systems["wellness"]

    def test_hire_physician_and_monthly_effect(self) -> None:
        c = self.sim.world.character(self.rid)
        c.gold = 200
        c.health = 3.0
        c.stress = 100
        msg = self.sim.gameplays.perform(
            self.sim, self.rid, "wellness", "hire_physician"
        )
        self.assertIn("医师", msg)
        self.assertTrue(self.wellness.state(self.sim, self.rid)["has_physician"])
        self.assertEqual(c.gold, 170)
        self.wellness.tick_month(self.sim, self.rid)
        self.assertEqual(c.gold, 160)
        self.assertGreater(c.health, 3.0)
        self.assertLess(c.stress, 100)

    def test_doctor_leaves_when_unpaid(self) -> None:
        c = self.sim.world.character(self.rid)
        c.gold = 100
        self.sim.gameplays.perform(self.sim, self.rid, "wellness", "hire_physician")
        c.gold = 0
        self.wellness.tick_month(self.sim, self.rid)
        self.assertFalse(self.wellness.state(self.sim, self.rid)["has_physician"])

    def test_dismiss_physician(self) -> None:
        c = self.sim.world.character(self.rid)
        c.gold = 100
        self.sim.gameplays.perform(self.sim, self.rid, "wellness", "hire_physician")
        self.sim.gameplays.perform(
            self.sim, self.rid, "wellness", "dismiss_physician"
        )
        self.assertFalse(self.wellness.state(self.sim, self.rid)["has_physician"])
        with self.assertRaises(ValueError):
            self.sim.gameplays.perform(
                self.sim, self.rid, "wellness", "dismiss_physician"
            )


class GameplayApiTest(unittest.TestCase):
    def setUp(self) -> None:
        self._tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self._tmp.cleanup)
        self.api = GameAPI()
        self.api.save_path = Path(self._tmp.name) / "saves" / "autosave.json"
        self.api.sim.world.character(self.api.player_id).gold = 500

    def test_snapshot_exposes_catalog_and_player_state(self) -> None:
        snap = self.api.snapshot()
        gp = snap["gameplays"]
        ids = [r["id"] for r in gp["catalog"]]
        self.assertIn("hunting", ids)
        self.assertIn("pilgrimage", ids)  # 规划中条目也在目录中
        state_ids = [row["id"] for row in gp["player"]]
        self.assertIn("wellness", state_ids)

    def test_gameplay_action_hunt(self) -> None:
        res = self.api.action(
            {"action": "gameplay_action", "gameplay": "hunting", "op": "organize"}
        )
        self.assertIn("狩猎", " ".join(res.get("messages", [])))
        self.assertEqual(
            self.api.sim.gameplays.systems["hunting"].cooldowns.get(
                self.api.player_id
            ),
            3,
        )

    def test_save_load_roundtrip_preserves_gameplay_state(self) -> None:
        self.api.action(
            {"action": "gameplay_action", "gameplay": "hunting", "op": "organize"}
        )
        self.api.action(
            {"action": "gameplay_action", "gameplay": "wellness", "op": "hire_physician"}
        )
        self.api.action({"action": "save", "name": "gameplay"})

        # 破坏状态后读档
        self.api.sim.gameplays.systems["hunting"].cooldowns.clear()
        self.api.sim.gameplays.systems["wellness"].physicians.clear()
        self.api.action({"action": "load", "name": "gameplay"})

        systems = self.api.sim.gameplays.systems
        self.assertEqual(systems["hunting"].cooldowns.get(self.api.player_id), 3)
        self.assertTrue(systems["wellness"].physicians.get(self.api.player_id))


if __name__ == "__main__":
    unittest.main()
