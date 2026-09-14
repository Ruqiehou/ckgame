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


    def test_1042_confessor_scenario_has_three_claimants(self) -> None:
        """1042 忏悔者爱德华场景：一顶王冠、三家主张，且国王无地可依。"""
        self.assertIn("1042", {s["id"] for s in list_scenarios()})
        simulation = GameSimulation("1042")
        world = simulation.world
        self.assertEqual(world.date.year, 1042)
        dip = simulation.diplomacy
        england = next(t.id for t in world.titles.values() if t.name == "英格兰王国")

        edward = next(c for c in world.alive_characters() if "爱德华国王" in c.name)
        magnus = next(c for c in world.alive_characters() if "马格努斯" in c.name)
        william = next(c for c in world.alive_characters() if "诺曼底的威廉" in c.name)
        exile = next(c for c in world.alive_characters() if "流亡者爱德华" in c.name)

        # 挪威的马格努斯依哈德克努特之约宣称英格兰，并与爱德华敌对
        self.assertTrue(dip.flags(edward.id, magnus.id).rival, "爱德华与马格努斯应为世仇")
        for claimant, who in ((magnus, "马格努斯"), (william, "诺曼底的威廉"), (exile, "流亡者爱德华")):
            self.assertTrue(
                any(claim.title == england for claim in dip.claims_of(claimant.id)),
                f"{who} 缺少对英格兰王位的宣称",
            )
        # 无地的国王倚仗诺曼底
        self.assertTrue(dip.are_allied(edward.id, william.id), "爱德华应与诺曼底结盟")
        # 戈德温手握四郡，远超王室直辖两郡
        godwin = next(c for c in world.alive_characters() if "戈德温伯爵" in c.name)
        self.assertGreater(
            len(godwin.held_titles), len(edward.held_titles), "戈德温应强于国王"
        )

    def test_1087_conquerors_legacy_has_brother_rivalry(self) -> None:
        """1087 征服者的遗产：三子分家，长幼相争，1088 叛乱集团已成形。"""
        self.assertIn("1087", {s["id"] for s in list_scenarios()})
        simulation = GameSimulation("1087")
        world = simulation.world
        self.assertEqual(world.date.year, 1087)
        dip = simulation.diplomacy
        england = next(t.id for t in world.titles.values() if t.name == "英格兰王国")
        normandy = next(t.id for t in world.titles.values() if t.name == "诺曼底公国")

        robert = next(c for c in world.alive_characters() if "罗伯特·柯索斯" in c.name)
        rufus = next(c for c in world.alive_characters() if "威廉·鲁弗斯" in c.name)
        henry = next(c for c in world.alive_characters() if "亨利·博克莱尔" in c.name)
        odo = next(c for c in world.alive_characters() if "巴约的奥多" in c.name)
        mortain = next(c for c in world.alive_characters() if "莫尔坦" in c.name)

        # 长子权之争：双向宣称 + 世仇
        self.assertTrue(dip.flags(robert.id, rufus.id).rival, "罗伯特与鲁弗斯应为世仇")
        self.assertTrue(
            any(claim.title == england for claim in dip.claims_of(robert.id)),
            "罗伯特缺少对英格兰的宣称",
        )
        self.assertTrue(
            any(claim.title == normandy for claim in dip.claims_of(rufus.id)),
            "鲁弗斯缺少对诺曼底的宣称",
        )
        # 1088 年叛乱集团：幼弟与长兄结盟，奥多与莫尔坦响应
        for ally in (henry, odo, mortain):
            self.assertTrue(
                dip.are_allied(robert.id, ally.id), f"罗伯特应与 {ally.name} 结盟"
            )
        # 亨利只得到贝叶一郡，且是兄长的封臣
        self.assertEqual(len(henry.held_titles), 1, "亨利应只有一块封地")

    def test_1337_hundred_years_war_starts_at_war(self) -> None:
        """1337 百年战争：开局当日即已开战，且老同盟从北方牵制英格兰。"""
        self.assertIn("1337", {s["id"] for s in list_scenarios()})
        simulation = GameSimulation("1337")
        world = simulation.world
        self.assertEqual(world.date.year, 1337)
        dip = simulation.diplomacy
        france = next(t.id for t in world.titles.values() if t.name == "法兰西王国")
        aquitaine = next(t.id for t in world.titles.values() if t.name == "阿基坦公国")

        edward = next(c for c in world.alive_characters() if "爱德华三世" in c.name)
        philip = next(c for c in world.alive_characters() if "腓力六世" in c.name)
        david = next(c for c in world.alive_characters() if "大卫二世" in c.name)

        self.assertTrue(dip.flags(edward.id, philip.id).rival, "爱德华三世与腓力六世应为世仇")
        self.assertTrue(
            any(claim.title == france for claim in dip.claims_of(edward.id)),
            "爱德华三世缺少对法兰西王位的宣称",
        )
        self.assertTrue(
            any(claim.title == aquitaine for claim in dip.claims_of(philip.id)),
            "腓力六世缺少对阿基坦的宣称",
        )
        # 老同盟：苏格兰与法兰西
        self.assertTrue(dip.are_allied(david.id, philip.id), "苏格兰与法兰西应结盟")
        # 开局即处于战争状态
        wars = [
            w
            for w in simulation.wars.active_wars()
            if {w.attacker_primary, w.defender_primary} == {edward.id, philip.id}
        ]
        self.assertTrue(wars, "1337 年 11 月百年战争应已爆发")
        self.assertEqual(wars[0].name, "百年战争")
        self.assertTrue(dip.flags(edward.id, philip.id).at_war, "双方应处于战争状态")
        # 英格兰在欧陆的立足点：阿基坦由英王持有，却是法王的封臣
        self.assertIn(aquitaine, edward.held_titles, "爱德华三世应持有阿基坦")


if __name__ == "__main__":
    unittest.main()
