"""GameAPI 公共操作边界测试。"""

from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

from ck_engine.ui.api import GameAPI


class GameApiActionContractTest(unittest.TestCase):
    def setUp(self) -> None:
        self._tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self._tmp.cleanup)
        self.api = GameAPI()
        self.api.save_path = Path(self._tmp.name) / "saves" / "autosave.json"

    def test_advance_clamps_days_to_one(self) -> None:
        self.api.action({"action": "advance", "days": 0})
        self.assertEqual(self.api.sim.world.tick, 1)

        self.api.action({"action": "advance", "days": -20})
        self.assertEqual(self.api.sim.world.tick, 2)

    def test_advance_clamps_days_to_one_year(self) -> None:
        self.api.action({"action": "advance", "days": 9999})
        self.assertEqual(self.api.sim.world.tick, 365)

    def test_new_game_switches_scenario_and_resets_selection(self) -> None:
        self.api.selected_county = 1
        self.api.selected_army = 2

        result = self.api.action({"action": "new_game", "scenario": "867"})

        self.assertEqual(self.api.sim.scenario_id, "867")
        self.assertEqual(self.api.sim.world.date.year, 867)
        self.assertIsNone(self.api.selected_county)
        self.assertIsNone(self.api.selected_army)
        self.assertIn("867", " ".join(result["messages"]))


if __name__ == "__main__":
    unittest.main()