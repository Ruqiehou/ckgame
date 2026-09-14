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

    def test_all_scenarios_load_and_bootstrap(self) -> None:
        """data/scenarios 下的每个场景都必须能加载并完整构建模拟。

        这条用例是 1200 场景崩溃事故的回归防线：任何新增场景若数据不自洽
        （如引用不存在的角色 key），都会在这里被立刻拦下。
        """
        scenarios = list_scenarios()
        self.assertGreaterEqual(len(scenarios), 4, "场景数量异常")
        for scenario in scenarios:
            sid = scenario["id"]
            with self.subTest(scenario=sid):
                # 1) 纯数据加载
                world = load_scenario(sid)
                self.assertGreater(len(world.characters), 0, f"{sid} 无角色")
                self.assertGreater(len(world.map.counties), 0, f"{sid} 无省份")
                self.assertGreater(len(world.titles), 0, f"{sid} 无头衔")
                self.assertGreater(len(world.dynasties), 0, f"{sid} 无王朝")
                # 2) 每个省份都必须有领主与所属头衔
                for county in world.map.iter():
                    self.assertNotEqual(county.holder, 0, f"{sid}: {county.name} 无领主")
                    self.assertNotEqual(
                        county.owner_title, 0, f"{sid}: {county.name} 无所属头衔"
                    )
                    self.assertIsNotNone(
                        world.title(county.owner_title), f"{sid}: {county.name} 头衔悬空"
                    )
                # 3) 完整构建模拟（bootstrap 会跑外交 / 内阁 / 王国法律）
                sim = GameSimulation(sid)
                rulers = list(sim.world.rulers())
                self.assertGreaterEqual(len(rulers), 1, f"{sid} 无统治者")
                for ruler in rulers:
                    self.assertIn(ruler.id, sim.realm_laws, f"{sid}: {ruler.name} 缺王国法律")
                    self.assertIsNotNone(
                        sim.councils.get(ruler.id), f"{sid}: {ruler.name} 缺内阁"
                    )

    def test_1200_angevin_scenario_has_core_conflict(self) -> None:
        """1200 安茹帝国场景：约翰王、阿蒂尔与腓力二世的三方冲突必须成立。"""
        self.assertIn("1200", {s["id"] for s in list_scenarios()})
        simulation = GameSimulation("1200")
        world = simulation.world
        self.assertEqual(world.date.year, 1200)
        dip = simulation.diplomacy

        john = next(c for c in world.alive_characters() if "约翰" in c.name)
        arthur = next(c for c in world.alive_characters() if "阿蒂尔" in c.name)
        philip = next(c for c in world.alive_characters() if "腓力二世" in c.name)
        england = next(t.id for t in world.titles.values() if "英格兰" in t.name)
        normandy = next(t.id for t in world.titles.values() if "诺曼底" in t.name)

        # 约翰 vs 侄儿阿蒂尔：王位争夺
        self.assertTrue(dip.flags(john.id, arthur.id).rival, "约翰与阿蒂尔应为世仇")
        self.assertTrue(
            any(claim.title == england for claim in dip.claims_of(arthur.id)),
            "阿蒂尔缺少对英格兰王位的宣称",
        )
        # 约翰 vs 腓力二世：封建宗主之争
        self.assertTrue(dip.flags(john.id, philip.id).rival, "约翰与腓力二世应为世仇")
        self.assertTrue(
            any(claim.title == normandy for claim in dip.claims_of(philip.id)),
            "腓力二世缺少对诺曼底的宣称",
        )
        # 布列塔尼与法兰西结盟对抗约翰
        self.assertTrue(dip.are_allied(arthur.id, philip.id), "阿蒂尔应与腓力二世结盟")
        # 阿蒂尔在开局仍未成年，布列塔尼处于摄政状态
        self.assertFalse(arthur.is_adult(world.date), "1200 年阿蒂尔应未成年")
        # 阿基坦的埃莉诺仍持有阿基坦
        eleanor = next(c for c in world.alive_characters() if "埃莉诺" in c.name)
        self.assertTrue(
            any(world.title(t).name.startswith("阿基坦") for t in eleanor.held_titles),
            "埃莉诺应持有阿基坦",
        )


if __name__ == "__main__":
    unittest.main()
