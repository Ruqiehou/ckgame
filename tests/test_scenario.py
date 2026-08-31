"""1066 场景构建不变量测试。"""

from __future__ import annotations

import random
import unittest

from ck_engine.game.scenario_loader import load_scenario, list_scenarios
from ck_engine.game.simulation import GameSimulation


class ScenarioInvariantsTest(unittest.TestCase):
    def setUp(self) -> None:
        random.seed(11)
        self.sim = GameSimulation()
        self.w = self.sim.world

    def test_scenario_counts(self) -> None:
        self.assertEqual(len(self.w.characters), 25)
        self.assertEqual(len(self.w.map.counties), 14)
        self.assertEqual(len(self.w.titles), 19)
        self.assertEqual(len(self.w.dynasties), 7)

    def test_every_county_has_holder_and_title(self) -> None:
        for county in self.w.map.iter():
            self.assertNotEqual(county.holder, 0, f"{county.name} 无领主")
            self.assertNotEqual(county.owner_title, 0, f"{county.name} 无所属头衔")
            self.assertIsNotNone(self.w.title(county.owner_title))

    def test_rulers_have_councils_and_laws(self) -> None:
        rulers = list(self.w.rulers())
        self.assertGreaterEqual(len(rulers), 5)
        for r in rulers:
            self.assertIn(r.id, self.sim.realm_laws, f"{r.name} 缺少王国法律")
            self.assertIsNotNone(self.sim.councils.get(r.id), f"{r.name} 缺少内阁")

    def test_1453_rose_war_scenario_is_playable(self) -> None:
        world = load_scenario("1453")
        self.assertEqual(world.date.year, 1453)
        self.assertEqual(len(world.map.counties), 8)
        self.assertEqual(len(world.characters), 9)
        self.assertIn("1453", {scenario["id"] for scenario in list_scenarios()})
        simulation = GameSimulation("1453")
        self.assertEqual(len(simulation.councils.by_ruler), 5)
        self.assertTrue(any("玫瑰战争" in scenario["name"] for scenario in list_scenarios()))

    def test_867_viking_scenario_has_core_conflict(self) -> None:
        simulation = GameSimulation("867")
        world = simulation.world
        self.assertEqual(world.date.year, 867)
        self.assertEqual(len(world.map.counties), 8)
        alfred = next(c for c in world.alive_characters() if "阿尔弗雷德" in c.name)
        ivar = next(c for c in world.alive_characters() if "无骨者伊瓦尔" in c.name)
        burgred = next(c for c in world.alive_characters() if "伯格雷德" in c.name)
        self.assertTrue(simulation.diplomacy.flags(alfred.id, ivar.id).rival)
        self.assertTrue(simulation.diplomacy.claims_of(ivar.id))
        self.assertTrue(
            any(
                {treaty.a, treaty.b} == {alfred.id, burgred.id}
                for treaty in simulation.diplomacy.treaties
            )
        )

    def test_william_harold_rivalry_and_claim(self) -> None:
        william = next(c for c in self.w.alive_characters() if "威廉·征服者" in c.name)
        harold = next(c for c in self.w.alive_characters() if "哈罗德" in c.name)
        dip = self.sim.diplomacy
        self.assertTrue(
            dip.flags(william.id, harold.id).rival
            or dip.flags(harold.id, william.id).rival
        )
        self.assertTrue(dip.claims_of(william.id), "威廉缺少对英格兰的宣称")


if __name__ == "__main__":
    unittest.main()
