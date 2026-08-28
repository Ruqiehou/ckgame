"""派系最后通牒接受/拒绝闭环测试。"""

from __future__ import annotations

import random
import tempfile
import unittest
from pathlib import Path

from ck_engine.core import NONE_ID
from ck_engine.game.simulation import GameSimulation, PendingUltimatum
from ck_engine.politics import FactionKind
from ck_engine.politics.factions import FactionEvent
from ck_engine.ui.api import GameAPI


def build_faction(sim: GameSimulation, kind: FactionKind, liege_id: int, members):
    fid = sim.factions.create(kind, liege_id)
    for m in members:
        sim.factions.join(fid, m)
    return fid


class UltimatumResolveTest(unittest.TestCase):
    def setUp(self) -> None:
        random.seed(3)
        self.sim = GameSimulation()
        self.w = self.sim.world
        self.harold = next(c for c in self.w.alive_characters() if "哈罗德" in c.name)
        self.vassal = self.w.character(17)  # 埃德温，麦西亚公爵
        self.assertIn(17, [t.holder for t in self.w.titles.values()])

    def test_accept_lower_crown_authority(self) -> None:
        fid = build_faction(
            self.sim, FactionKind.LOWER_CROWN_AUTHORITY, self.harold.id, [17]
        )
        title = self.w.title(self.harold.primary_title)
        before = title.realm_law.crown_authority
        self.sim.resolve_ultimatum(fid, accept=True)
        self.assertLess(title.realm_law.crown_authority, before)
        self.assertNotIn(fid, self.sim.factions.factions)
        self.assertNotIn(fid, self.sim.pending_ultimatums)
        self.assertTrue(any("接受了限制王权" in line for line in self.w.log))

    def test_accept_independence_frees_vassal(self) -> None:
        fid = build_faction(self.sim, FactionKind.INDEPENDENCE, self.harold.id, [17])
        held = [
            tid
            for tid in self.vassal.held_titles
            if self.w.title(tid) and self.w.title(tid).de_facto_liege != NONE_ID
        ]
        self.assertTrue(held, "前置条件：封臣应有宗主头衔链")
        self.sim.resolve_ultimatum(fid, accept=True)
        for tid in held:
            self.assertEqual(self.w.title(tid).de_facto_liege, NONE_ID)
        self.assertTrue(any("接受了解离" in line or "独立" in line for line in self.w.log))

    def test_reject_starts_revolt_war(self) -> None:
        fid = build_faction(self.sim, FactionKind.LIBERTY, self.harold.id, [17])
        self.sim.resolve_ultimatum(fid, accept=False)
        wars = [
            w
            for w in self.sim.wars.active_wars()
            if w.involves(17) and w.involves(self.harold.id)
        ]
        self.assertEqual(len(wars), 1)
        self.assertNotIn(fid, self.sim.factions.factions)
        self.assertTrue(self.sim.diplomacy.flags(17, self.harold.id).at_war)
        self.assertTrue(any("拒绝了最后通牒" in line for line in self.w.log))

    def test_resolve_missing_faction_raises(self) -> None:
        with self.assertRaises(ValueError):
            self.sim.resolve_ultimatum(99999, accept=True)

    def test_ai_liege_resolves_immediately_by_power(self) -> None:
        william = next(c for c in self.w.alive_characters() if "威廉·征服者" in c.name)
        # 实力占优 → AI 让步
        fid = build_faction(
            self.sim, FactionKind.LOWER_CROWN_AUTHORITY, william.id, [17]
        )
        self.sim.factions.factions[fid].power = 120.0
        ev = FactionEvent(
            kind="ultimatum",
            faction_id=fid,
            liege=william.id,
            faction_kind=FactionKind.LOWER_CROWN_AUTHORITY,
            members=[17],
        )
        self.sim._process_ultimatum_event(ev)
        self.assertNotIn(fid, self.sim.factions.factions)
        self.assertNotIn(fid, self.sim.pending_ultimatums)
        self.assertTrue(any("接受了限制王权" in line for line in self.w.log))

        # 实力不足 → AI 拒绝开战
        fid2 = build_faction(self.sim, FactionKind.INDEPENDENCE, william.id, [17])
        self.sim.factions.factions[fid2].power = 80.0
        ev2 = FactionEvent(
            kind="ultimatum",
            faction_id=fid2,
            liege=william.id,
            faction_kind=FactionKind.INDEPENDENCE,
            members=[17],
        )
        self.sim._process_ultimatum_event(ev2)
        self.assertNotIn(fid2, self.sim.factions.factions)
        self.assertTrue(any("拒绝了最后通牒" in line for line in self.w.log))

    def test_player_liege_gets_pending_ultimatum(self) -> None:
        self.sim.player_ids = {self.harold.id}
        fid = build_faction(self.sim, FactionKind.LIBERTY, self.harold.id, [17])
        ev = FactionEvent(
            kind="ultimatum",
            faction_id=fid,
            liege=self.harold.id,
            faction_kind=FactionKind.LIBERTY,
            members=[17],
        )
        self.sim._process_ultimatum_event(ev)
        self.assertIn(fid, self.sim.pending_ultimatums)
        self.assertIn(fid, self.sim.factions.factions)
        u = self.sim.pending_ultimatums[fid]
        self.assertEqual(u.kind, FactionKind.LIBERTY)
        self.assertEqual(u.liege, self.harold.id)


class UltimatumApiTest(unittest.TestCase):
    def setUp(self) -> None:
        random.seed(3)
        self._tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self._tmp.cleanup)
        self.api = GameAPI()
        self.api.save_path = Path(self._tmp.name) / "saves" / "autosave.json"

    def test_snapshot_and_action_roundtrip(self) -> None:
        api = self.api
        fid = build_faction(
            api.sim, FactionKind.LOWER_CROWN_AUTHORITY, api.player_id, [17]
        )
        api.sim.pending_ultimatums[fid] = PendingUltimatum(
            faction_id=fid,
            kind=FactionKind.LOWER_CROWN_AUTHORITY,
            liege=api.player_id,
            members=[17],
        )
        snap = api.snapshot()
        self.assertEqual(len(snap["pending_ultimatums"]), 1)
        self.assertEqual(
            snap["pending_ultimatums"][0]["faction_id"], fid
        )
        title = api.sim.world.title(api.player_id)
        before = title.realm_law.crown_authority
        res = api.action({"action": "respond_ultimatum", "faction_id": fid, "accept": True})
        self.assertIn("已接受", " ".join(res.get("messages", [])))
        self.assertNotIn(fid, api.sim.pending_ultimatums)
        self.assertNotIn(fid, api.sim.factions.factions)
        self.assertLess(title.realm_law.crown_authority, before)

    def test_pending_ultimatum_persists_through_save(self) -> None:
        api = self.api
        fid = build_faction(api.sim, FactionKind.LIBERTY, api.player_id, [17])
        api.sim.pending_ultimatums[fid] = PendingUltimatum(
            faction_id=fid,
            kind=FactionKind.LIBERTY,
            liege=api.player_id,
            members=[17],
        )
        api.action({"action": "save", "name": "ult"})
        api.action({"action": "load", "name": "ult"})
        self.assertIn(fid, api.sim.pending_ultimatums)
        api.action({"action": "respond_ultimatum", "faction_id": fid, "accept": False})
        self.assertNotIn(fid, api.sim.pending_ultimatums)
        wars = [
            w
            for w in api.sim.wars.active_wars()
            if w.involves(17) and w.involves(api.player_id)
        ]
        self.assertEqual(len(wars), 1)


if __name__ == "__main__":
    unittest.main()
