"""存档/读档往返一致性测试。"""

from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

from ck_engine.politics.diplomacy import CasusBelli
from ck_engine.ui.api import GameAPI


class SaveLoadRoundTripTest(unittest.TestCase):
    def setUp(self) -> None:
        self._tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self._tmp.cleanup)
        self.api = GameAPI()
        self.api.save_path = Path(self._tmp.name) / "saves" / "autosave.json"

    def test_roundtrip_preserves_date_gold_and_wars(self) -> None:
        api = self.api
        api.sim.run_days(30)
        other = next(
            c for c in api.sim.world.alive_characters() if c.id != api.player_id
        )
        other.add_gold(321)
        gold_before = other.gold
        date_before = str(api.sim.world.date)
        war_name = "测试战争"
        wid = api.sim.wars.declare_war(
            CasusBelli.CONQUEST,
            api.player_id,
            other.id,
            api.sim.world.date,
            war_name,
        )
        api.sim.diplomacy.set_at_war(api.player_id, other.id, True)

        res = api.action({"action": "save", "name": "roundtrip"})
        self.assertIn("已存档", " ".join(res.get("messages", [])))

        # 破坏状态后再读档
        api.action({"action": "advance", "days": 30})
        other.add_gold(-1000)

        res = api.action({"action": "load", "name": "roundtrip"})
        self.assertIn("已读档", " ".join(res.get("messages", [])))
        self.assertEqual(str(api.sim.world.date), date_before)
        self.assertAlmostEqual(api.sim.world.character(other.id).gold, gold_before)
        war = api.sim.wars.war(wid)
        self.assertIsNotNone(war)
        self.assertEqual(war.name, war_name)
        self.assertTrue(war.active)

    def test_save_listing_and_delete(self) -> None:
        api = self.api
        api.action({"action": "save", "name": "listing_test"})
        names = [s["name"] for s in api.action({"action": "save"})["saves"]]
        self.assertIn("listing_test", names)
        api.action({"action": "delete_save", "name": "listing_test"})
        names = [s["name"] for s in api.snapshot()["saves"]]
        self.assertNotIn("listing_test", names)


if __name__ == "__main__":
    unittest.main()
