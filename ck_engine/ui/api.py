"""游戏状态序列化与玩家操作。

GameAPI 本体只保留生命周期、action 分发与通用辅助；按职责拆分为三个 Mixin：
- SnapshotMixin（ck_engine.ui.snapshot）：snapshot() 快照构建
- SaveGameMixin（ck_engine.ui.savegame）：存档/读档
- DiplomacyActionsMixin（ck_engine.ui.diplomacy_actions）：外交类操作
"""

from __future__ import annotations

import sys
import threading
from pathlib import Path
from typing import Any, Dict, List, Optional

from ck_engine.core import NONE_ID
from ck_engine.game.simulation import GameSimulation
from ck_engine.military.army import ArmyStatus, UnitType
from ck_engine.politics.council import CouncilPosition, CouncilTask
from ck_engine.politics.diplomacy import CasusBelli
from ck_engine.politics.laws import CrownAuthority, GenderLaw, SuccessionLaw
from ck_engine.politics.schemes import SchemeKind
from ck_engine.ui.diplomacy_actions import DiplomacyActionsMixin
from ck_engine.ui.savegame import SaveGameMixin
from ck_engine.ui.snapshot import SnapshotMixin
from ck_engine.ui.tutorial import TutorialSystem
from ck_engine.world.buildings import BuildingKind


class GameAPI(SnapshotMixin, SaveGameMixin, DiplomacyActionsMixin):
    CHEAT_GOLD = 99999.0
    CHEAT_PRESTIGE = 99999.0
    CHEAT_PIETY = 99999.0

    def __init__(self, scenario: str | None = None) -> None:
        self.sim = GameSimulation(scenario)
        self.player_id = self._default_player()
        self.sim.player_ids = {self.player_id}
        self.selected_county: Optional[int] = None
        self.selected_army: Optional[int] = None
        self.messages: List[str] = ["欢迎。点击地图省份查看详情，使用侧栏下达指令。"]
        base_dir = Path(getattr(sys, "_MEIPASS", Path(__file__).resolve().parents[2]))
        self.save_path = base_dir / "saves" / "autosave.json"
        self._lock = threading.Lock()
        self.cheat_mode = False
        self.infinite_gold_mode = False
        self.tutorial = TutorialSystem()

    def _default_player(self) -> int:
        for c in self.sim.world.alive_characters():
            if "哈罗德" in c.name:
                return c.id
        rulers = list(self.sim.world.rulers())
        return rulers[0].id if rulers else 1

    def _sync_player(self) -> None:
        self.sim.player_ids = {self.player_id}

    def notify(self, msg: str) -> None:
        self.messages.append(msg)
        if len(self.messages) > 80:
            self.messages = self.messages[-60:]

    def snapshot(self) -> Dict[str, Any]:
        with self._lock:
            return self._snapshot_unlocked()

    # ---------- 操作 ----------
    def action(self, payload: Dict[str, Any]) -> Dict[str, Any]:
        with self._lock:
            return self._action_unlocked(payload)

    def _action_unlocked(self, payload: Dict[str, Any]) -> Dict[str, Any]:
        kind = payload.get("action")
        try:
            if kind == "select_county":
                self.selected_county = int(payload["county_id"])
                self.selected_army = None
            elif kind == "select_army":
                self.selected_army = int(payload["army_id"])
                self.selected_county = None
            elif kind == "set_player":
                self.player_id = int(payload["character_id"])
                self._sync_player()
                self.selected_army = None
                self.notify(f"切换玩家为 {self._name(self.player_id)}")
            elif kind == "advance":
                days = int(payload.get("days", 1))
                days = max(1, min(365, days))
                prev_chunk = self.sim.world.tick // 30
                self._sync_player()
                old_year = self.sim.world.date.year
                self.sim.run_days(days)
                for _ in range(max(0, self.sim.world.date.year - old_year)):
                    self._educate_children_yearly()
                new_chunk = self.sim.world.tick // 30
                self.notify(f"时间推进 {days} 天 → {self.sim.world.date}")
                # 自动存档（每 30 天存一次）
                if prev_chunk != new_chunk:
                    try:
                        self._save(None)
                    except Exception:
                        pass
            elif kind == "raise_army":
                self._raise_army(int(payload["county_id"]))
            elif kind == "move_army":
                self._move_army(int(payload["army_id"]), int(payload["county_id"]))
            elif kind == "disband_army":
                self._disband_army(int(payload["army_id"]))
            elif kind == "declare_war":
                self._declare_war(int(payload["target_id"]))
            elif kind == "improve_relations":
                self._improve(int(payload["target_id"]))
            elif kind == "hold_feast":
                self._feast()
            elif kind == "appease_faction":
                self._appease_faction(int(payload["faction_id"]))
            elif kind == "respond_ultimatum":
                self._respond_ultimatum(
                    int(payload["faction_id"]), bool(payload.get("accept", False))
                )
            elif kind == "white_peace":
                self._white_peace(int(payload["war_id"]))
            elif kind == "resolve_event":
                self._resolve_event(int(payload["event_id"]), int(payload["choice_id"]))
            elif kind == "save":
                self._save(payload.get("name"))
            elif kind == "load":
                self._load(payload.get("name"))
            elif kind == "delete_save":
                self._delete_save(str(payload["name"]))
            elif kind == "new_game":
                self.sim = GameSimulation(payload.get("scenario") or self.sim.scenario_id)
                self.player_id = self._default_player()
                self._sync_player()
                self.selected_county = None
                self.selected_army = None
                self.messages = [f"新局开始（场景：{self.sim.scenario_id}）。"]
            elif kind == "set_succession_law":
                self._set_succession_law(payload.get("law"))
            elif kind == "set_crown_authority":
                self._set_crown_authority(int(payload.get("level", 0)))
            elif kind == "set_gender_law":
                self._set_gender_law(payload.get("law"))
            elif kind == "start_scheme":
                self._start_scheme(payload.get("scheme_kind"), int(payload["target_id"]))
            elif kind == "form_alliance":
                self._form_alliance(int(payload["target_id"]))
            elif kind == "form_non_aggression":
                self._form_non_aggression(int(payload["target_id"]))
            elif kind == "form_vassalage":
                self._form_vassalage(int(payload["target_id"]))
            elif kind == "form_trade_agreement":
                self._form_trade_agreement(int(payload["target_id"]))
            elif kind == "form_intelligence_sharing":
                self._form_intelligence_sharing(int(payload["target_id"]))
            elif kind == "arrange_marriage":
                self._arrange_marriage(int(payload["target_id"]))
            elif kind == "send_gift":
                self._send_gift(int(payload["target_id"]), float(payload.get("amount", 50)))
            elif kind == "set_rival":
                self._set_rival(int(payload["target_id"]))
            elif kind == "invite_to_court":
                self._invite_to_court(int(payload["target_id"]))
            elif kind == "host_feast_for":
                self._host_feast_for(int(payload["target_id"]))
            elif kind == "duel":
                self._duel(int(payload["target_id"]))
            elif kind == "arrange_child_marriage":
                self._arrange_child_marriage(
                    int(payload["child_id"]), int(payload["target_id"])
                )
            elif kind == "break_engagement":
                self._break_engagement(int(payload["child_id"]))
            elif kind == "set_child_education":
                self._set_child_education(
                    int(payload["child_id"]), str(payload["focus"])
                )
            elif kind == "execute_decision":
                self._execute_decision(payload.get("decision_id"))
            elif kind == "appoint_council":
                self._appoint_council(payload.get("position"), int(payload.get("character_id", NONE_ID)))
            elif kind == "assign_council_task":
                self._assign_council_task(payload.get("position"), payload.get("task"))
            elif kind == "grant_title":
                self._grant_title(int(payload["title_id"]), int(payload["target_id"]))
            elif kind == "develop_county":
                self._develop_county(int(payload["county_id"]))
            elif kind == "recruit_knights":
                self._recruit_knights()
            elif kind == "set_commander":
                self._set_commander(int(payload["army_id"]), int(payload["character_id"]))
            elif kind == "fabricate_claim":
                self._fabricate_claim(int(payload["county_id"]))
            elif kind == "upgrade_building":
                self._upgrade_building(int(payload["county_id"]), payload.get("building_kind"))
            elif kind == "upgrade_port":
                self._upgrade_port(int(payload["county_id"]))
            elif kind == "upgrade_trade_route_maintenance":
                self._upgrade_trade_route_maintenance(int(payload["county_id"]))
            elif kind == "toggle_cheat":
                self.cheat_mode = not self.cheat_mode
                if self.cheat_mode:
                    self._apply_cheat()
                    self.notify("★ 作弊模式已开启：无限金钱/威望/虔诚")
                else:
                    self.notify("作弊模式已关闭")
            elif kind == "toggle_infinite_gold":
                self.infinite_gold_mode = not self.infinite_gold_mode
                if self.infinite_gold_mode:
                    self._apply_infinite_gold()
                    self.notify("★ 无限金钱模式已开启")
                else:
                    self.notify("无限金钱模式已关闭")
            elif kind == "tutorial_next":
                self._tutorial_next()
            elif kind == "tutorial_skip":
                self.tutorial.enabled = False
                self.notify("教程已跳过")
            elif kind == "cheat_add_gold":
                amount = float(payload.get("amount", 1000))
                player = self.sim.world.character(self.player_id)
                if player:
                    player.add_gold(amount)
                    self.notify(f"★ 作弊：+{amount:.0f} 金")
            elif kind == "cheat_complete_scheme":
                self._cheat_complete_scheme()
            else:
                self.notify(f"未知操作: {kind}")
        except Exception as e:  # noqa: BLE001 — 返回给前端
            self.notify(f"操作失败: {e}")
        self._apply_cheat()
        self._apply_infinite_gold()
        return self._snapshot_unlocked()

    def _name(self, cid: int) -> str:
        c = self.sim.world.character(cid)
        return c.name if c else "?"

    def _apply_cheat(self) -> None:
        """作弊模式开启时，每次操作后补满金币/威望/虔诚。"""
        if not self.cheat_mode:
            return
        player = self.sim.world.character(self.player_id)
        if not player:
            return
        player.gold = self.CHEAT_GOLD
        player.prestige = self.CHEAT_PRESTIGE
        player.piety = self.CHEAT_PIETY
        player.stress = 0

    def _apply_infinite_gold(self) -> None:
        """无限金钱模式开启时，每次操作后补满金币。"""
        if not self.infinite_gold_mode:
            return
        player = self.sim.world.character(self.player_id)
        if not player:
            return
        player.gold = self.CHEAT_GOLD

    def _grant_action_xp(self, amount: int) -> None:
        """给玩家角色增加经验值。"""
        player = self.sim.world.character(self.player_id)
        if not player:
            return
        leveled = player.gain_xp(amount)
        if leveled:
            self.notify(f"🎉 角色升级！当前等级：{player.level}")

    def _cheat_complete_scheme(self) -> None:
        """作弊：立即完成所有玩家的进行中阴谋。"""
        completed = 0
        for s in list(self.sim.schemes.schemes.values()):
            if s.owner == self.player_id and not s.exposed and not s.is_complete():
                s.progress = 100.0
                completed += 1
        if completed:
            self.notify(f"★ 作弊：{completed} 个阴谋已立即完成")
        else:
            self.notify("无进行中阴谋可完成")

    def _execute_decision(self, decision_id: str) -> None:
        """执行重大决策。"""
        ok = self.sim.decisions.execute(decision_id, self.sim.world, self.player_id)
        if ok:
            self.notify(f"已执行决策「{decision_id}」")
        else:
            raise ValueError(f"决策「{decision_id}」不可执行")

    def _player_laws(self) -> Dict[str, Any]:
        w = self.sim.world
        player = w.character(self.player_id)
        if not player or player.primary_title == NONE_ID:
            return {
                "succession": None,
                "crown_authority": None,
                "gender_law": None,
            }
        title = w.title(player.primary_title)
        if not title:
            return {
                "succession": None,
                "crown_authority": None,
                "gender_law": None,
            }
        law = title.realm_law
        return {
            "succession": law.succession.name,
            "crown_authority": law.crown_authority.value,
            "gender_law": law.gender_law.name,
        }

    def _set_succession_law(self, law_name: Any) -> None:
        if law_name is None:
            raise ValueError("缺少 law")
        try:
            new_law = SuccessionLaw[law_name]
        except KeyError:
            raise ValueError(f"未知继承法: {law_name}")
        title = self._player_title()
        title.realm_law.succession = new_law
        self.notify(f"继承法已改为：{new_law.name_zh()}")

    def _set_crown_authority(self, level: int) -> None:
        try:
            ca = CrownAuthority(level)
        except ValueError:
            raise ValueError(f"未知王权等级: {level}")
        title = self._player_title()
        title.realm_law.crown_authority = ca
        self.notify(f"王权已改为：{ca.name_zh()}")

    def _set_gender_law(self, law_name: Any) -> None:
        if law_name is None:
            raise ValueError("缺少 law")
        try:
            new_law = GenderLaw[law_name]
        except KeyError:
            raise ValueError(f"未知性别法: {law_name}")
        title = self._player_title()
        title.realm_law.gender_law = new_law
        self.notify(f"性别法已改为：{new_law.name_zh()}")

    def _player_title(self) -> Any:
        w = self.sim.world
        player = w.character(self.player_id)
        if not player or player.primary_title == NONE_ID:
            raise ValueError("玩家无主头衔")
        title = w.title(player.primary_title)
        if not title:
            raise ValueError("主头衔不存在")
        return title

    # ---------- 阴谋 ----------
    def _start_scheme(self, scheme_kind_name: Any, target_id: int) -> None:
        if scheme_kind_name is None:
            raise ValueError("缺少 scheme_kind")
        try:
            kind = SchemeKind[scheme_kind_name]
        except KeyError:
            raise ValueError(f"未知阴谋类型: {scheme_kind_name}")
        if target_id == self.player_id:
            raise ValueError("不能对自己发起阴谋")
        target = self.sim.world.character(target_id)
        if not target or not target.is_alive():
            raise ValueError("目标无效")
        # 已有针对同一目标的同类阴谋则拒绝
        for s in self.sim.schemes.schemes.values():
            if s.owner == self.player_id and s.target == target_id and not s.exposed and not s.is_complete():
                raise ValueError("已有针对该目标的进行中阴谋")
        sid = self.sim.schemes.start(kind, self.player_id, target_id, self.sim.world.date)
        self.sim.world.push_log(f"{self._name(self.player_id)} 开始策划{kind.name_zh()}（目标：{target.name}）")
        self.notify(f"已发起{kind.name_zh()} → {target.name}（#{sid}）")

    # ---------- 内阁 ----------
    def _appoint_council(self, position_name: Any, character_id: int) -> None:
        if position_name is None:
            raise ValueError("缺少 position")
        try:
            pos = CouncilPosition[position_name]
        except KeyError:
            raise ValueError(f"未知职位: {position_name}")
        target = self.sim.world.character(character_id)
        if not target or not target.is_alive():
            raise ValueError("人选无效")
        if not target.is_adult(self.sim.world.date):
            raise ValueError("未成年不能入阁")
        council = self.sim.councils.get_or_create(self.player_id)
        # 如果该角色已在其它职位，先移除
        for p in CouncilPosition.all():
            if council.get(p) == character_id:
                council.set(p, NONE_ID)
        council.set(pos, character_id)
        self.notify(f"任命 {target.name} 为{pos.name_zh()}")

    def _assign_council_task(self, position_name: Any, task_name: Any) -> None:
        if position_name is None or task_name is None:
            raise ValueError("缺少 position 或 task")
        try:
            pos = CouncilPosition[position_name]
            task = CouncilTask[task_name]
        except KeyError as e:
            raise ValueError(f"未知参数: {e}")
        council = self.sim.councils.get_or_create(self.player_id)
        council.tasks[pos] = task
        self.notify(f"{pos.name_zh()} 任务改为：{task.name_zh()}")

    # ---------- 头衔 ----------
    def _grant_title(self, title_id: int, target_id: int) -> None:
        w = self.sim.world
        player = w.character(self.player_id)
        target = w.character(target_id)
        if not player or not target:
            raise ValueError("目标无效")
        title = w.title(title_id)
        if not title:
            raise ValueError("头衔不存在")
        if title.holder != self.player_id:
            raise ValueError("该头衔不属于你")
        if title_id == player.primary_title:
            raise ValueError("不能授予主头衔")
        ok = w.grant_title(title_id, target_id)
        if not ok:
            raise ValueError("授予失败")
        # 授予后目标成为封臣
        if player.primary_title != NONE_ID:
            w.set_vassal(title_id, player.primary_title)
        self.notify(f"将「{title.name}」授予 {target.name}")

    # ---------- 经济与军事 ----------
    def _develop_county(self, county_id: int) -> None:
        county = self.sim.world.map.get(county_id)
        if not county:
            raise ValueError("省份不存在")
        if county.holder != self.player_id:
            raise ValueError("不是己方领地")
        player = self.sim.world.character(self.player_id)
        if not player or player.gold < 10:
            raise ValueError("金币不足（需要 10）")
        cap = county.terrain.development_cap()
        if county.development >= cap:
            raise ValueError(f"已达发展上限（{cap}）")
        player.add_gold(-10)
        county.development = min(cap, county.development + 1)
        self.notify(f"{county.name} 发展度 +1（→ {county.development}）")

    def _upgrade_port(self, county_id: int) -> None:
        county = self.sim.world.map.get(county_id)
        if not county:
            raise ValueError("省份不存在")
        if county.holder != self.player_id:
            raise ValueError("不是己方领地")
        if not county.has_port:
            raise ValueError("该省份没有港口")
        player = self.sim.world.character(self.player_id)
        upgrade_cost = county.upgrade_port()
        if player.gold < upgrade_cost:
            raise ValueError(f"金币不足（需要 {upgrade_cost:.0f}）")
        player.add_gold(-upgrade_cost)
        self.notify(f"{county.name} 港口升级到 {county.port_level} 级（花费 {upgrade_cost:.0f} 金）")

    def _upgrade_trade_route_maintenance(self, county_id: int) -> None:
        county = self.sim.world.map.get(county_id)
        if not county:
            raise ValueError("省份不存在")
        if county.holder != self.player_id:
            raise ValueError("不是己方领地")
        player = self.sim.world.character(self.player_id)
        upgrade_cost = county.upgrade_trade_route_maintenance()
        if player.gold < upgrade_cost:
            raise ValueError(f"金币不足（需要 {upgrade_cost:.0f}）")
        player.add_gold(-upgrade_cost)
        self.notify(f"{county.name} 贸易路线维护升级到 {county.trade_route_maintenance_level} 级（花费 {upgrade_cost:.0f} 金）")

    def _recruit_knights(self) -> None:
        player = self.sim.world.character(self.player_id)
        if not player or player.gold < 25:
            raise ValueError("金币不足（需要 25）")
        armies = self.sim.wars.armies_of(self.player_id)
        if not armies:
            raise ValueError("无野战军，请先征召")
        army = armies[0]
        player.add_gold(-25)
        army.add_men(UnitType.HEAVY_CAVALRY, 40)
        army.add_men(UnitType.HEAVY_INFANTRY, 80)
        self.notify(f"招募精锐：重骑兵+40 重步兵+80（{army.name}）")

    def _set_commander(self, army_id: int, character_id: int) -> None:
        army = self.sim.wars.army(army_id)
        if not army or army.owner != self.player_id:
            raise ValueError("无法指挥该军团")
        target = self.sim.world.character(character_id)
        if not target or not target.is_alive():
            raise ValueError("人选无效")
        if not target.is_adult(self.sim.world.date):
            raise ValueError("未成年不能指挥")
        army.commander = character_id
        self.notify(f"任命 {target.name} 为 {army.name} 指挥官")

    def _raise_army(self, county_id: int) -> None:
        county = self.sim.world.map.get(county_id)
        if not county:
            raise ValueError("省份不存在")
        if county.holder != self.player_id:
            # 允许在自己任意领地征召：若点到别人地，用首都
            owned = [
                c.id
                for c in self.sim.world.map.iter()
                if c.holder == self.player_id
            ]
            if not owned:
                raise ValueError("没有可征召的领地")
            if county_id not in owned:
                county_id = owned[0]
                county = self.sim.world.map.get(county_id)
        if self.sim.wars.armies_of(self.player_id):
            raise ValueError("已有野战军，请先解散或用现有军团")
        levies = max(100, county.monthly_levies() * 3)
        # 汇总玩家全部征召
        total = 0
        for c in self.sim.world.map.iter():
            if c.holder == self.player_id:
                total += c.monthly_levies()
        levies = max(200, total)
        aid = self.sim.wars.raise_army(
            self.player_id, county_id, levies, f"{self._name(self.player_id)}的军团"
        )
        army = self.sim.wars.army(aid)
        if army:
            army.add_men(UnitType.HEAVY_INFANTRY, levies // 10)
            army.add_men(UnitType.ARCHERS, levies // 12)
            army.add_men(UnitType.LIGHT_CAVALRY, levies // 20)
        self.selected_army = aid
        self.sim.world.push_log(f"{self._name(self.player_id)} 在 {county.name} 征召 {levies} 人")
        self.notify(f"征召成功：{levies} 人 @ {county.name}")

    def _move_army(self, army_id: int, county_id: int) -> None:
        army = self.sim.wars.army(army_id)
        if not army or not army.is_active():
            raise ValueError("军团不存在")
        if army.owner != self.player_id:
            raise ValueError("只能调动自己的军团")
        path = self.sim.world.map.path(army.location, county_id)
        if not path:
            raise ValueError("无法到达该省份")
        army.set_path(path)
        dest = self.sim.world.map.get(county_id)
        dname = dest.name if dest else "?"
        self.sim.world.push_log(f"{army.name} 向 {dname} 进军")
        self.notify(f"下令进军：{army.name} → {dname}（{len(path)-1} 步）")
        self.selected_army = army_id

    def _disband_army(self, army_id: int) -> None:
        army = self.sim.wars.army(army_id)
        if not army or army.owner != self.player_id:
            raise ValueError("无法解散该军团")
        army.status = ArmyStatus.DISBANDED
        army.stacks.clear()
        self.selected_army = None
        self.notify(f"已解散 {army.name}")

    def _declare_war(self, target_id: int) -> None:
        if target_id == self.player_id:
            raise ValueError("不能对自己宣战")
        dip = self.sim.diplomacy
        if dip.are_allied(self.player_id, target_id):
            raise ValueError("同盟无法宣战")
        if not dip.can_declare_war(self.player_id, target_id, self.sim.world.date.year):
            raise ValueError("外交上无法宣战（同盟/停战/已交战）")
        if any(
            w.involves(self.player_id) or w.involves(target_id)
            for w in self.sim.wars.active_wars()
        ):
            raise ValueError("一方已在战争中")
        player = self.sim.world.character(self.player_id)
        target = self.sim.world.character(target_id)
        if not player or not target:
            raise ValueError("目标无效")
        cb = CasusBelli.RIVALRY if dip.flags(self.player_id, target_id).rival else CasusBelli.CONQUEST
        cost = cb.prestige_cost()
        if player.prestige < cost:
            raise ValueError(f"威望不足（需要 {cost}）")
        player.add_prestige(-cost)
        name = f"{player.name} 对 {target.name} 的{cb.name_zh()}"
        wid = self.sim.wars.declare_war(cb, self.player_id, target_id, self.sim.world.date, name)
        dip.set_at_war(self.player_id, target_id, True)
        war = self.sim.wars.war(wid)
        if war:
            from ck_engine.military.war import WarParticipant

            for ally in dip.allies_of(self.player_id):
                if ally != target_id and not war.involves(ally):
                    war.participants.append(
                        WarParticipant(character=ally, is_attacker=True, joined=self.sim.world.date)
                    )
                    dip.set_at_war(ally, target_id, True)
        self.sim.world.push_log(f"宣战！{name} (#{wid})")
        dip.add_war_exhaustion(self.player_id)
        dip.add_war_exhaustion(target_id)
        self.notify(f"已对 {target.name} 宣战")
        self._grant_action_xp(50)

    def _improve(self, target_id: int) -> None:
        player = self.sim.world.character(self.player_id)
        if not player or player.gold < 10:
            raise ValueError("金币不足（需要 10）")
        player.add_gold(-10)
        self.sim.world.modify_opinion(target_id, self.player_id, 15)
        self.sim.world.modify_opinion(self.player_id, target_id, 5)
        self.notify(f"改善与 {self._name(target_id)} 的关系")
        self._grant_action_xp(20)

    def _feast(self) -> None:
        player = self.sim.world.character(self.player_id)
        if not player or player.gold < 20:
            raise ValueError("金币不足（需要 20）")
        player.add_gold(-20)
        player.add_prestige(15)
        player.add_stress(-10)
        # 宴会也安抚针对玩家的派系
        for f in list(self.sim.factions.factions.values()):
            if f.target_liege == self.player_id:
                for mid in list(f.members):
                    self.sim.world.modify_opinion(mid, self.player_id, 8)
                self.sim.factions.appease(f.id, 10.0)
        self.sim.world.push_log(f"{player.name} 举办了宴会")
        self.notify("举办宴会：威望+15，压力-10，派系不满下降")

    def _appease_faction(self, faction_id: int) -> None:
        f = self.sim.factions.factions.get(faction_id)
        if not f:
            raise ValueError("派系不存在")
        if f.target_liege != self.player_id:
            raise ValueError("只能安抚针对自己的派系")
        player = self.sim.world.character(self.player_id)
        cost = 25
        if not player or player.gold < cost:
            raise ValueError(f"金币不足（需要 {cost}）")
        player.add_gold(-cost)
        for mid in f.members:
            self.sim.world.modify_opinion(mid, self.player_id, 12)
        ok = self.sim.factions.appease(faction_id, 30.0)
        if not ok or faction_id not in self.sim.factions.factions:
            self.notify("派系已解散")
            self.sim.world.push_log(f"{player.name} 成功安抚并解散了一个派系")
        else:
            left = self.sim.factions.factions[faction_id]
            self.notify(f"派系不满降至 {left.discontent:.0f}")
            self.sim.world.push_log(f"{player.name} 安抚了派系，不满下降")

    def _respond_ultimatum(self, faction_id: int, accept: bool) -> None:
        kind_name = "?"
        f = self.sim.factions.factions.get(faction_id)
        if f:
            kind_name = f.kind.name_zh()
        self.sim.resolve_ultimatum(faction_id, accept)
        if accept:
            self.notify(f"已接受{kind_name}的最后通牒")
        else:
            self.notify(f"已拒绝{kind_name}的最后通牒，叛乱爆发！")
        self._grant_action_xp(30)

    def _white_peace(self, war_id: int) -> None:
        from ck_engine.military.war import WarResult

        w = self.sim.wars.war(war_id)
        if not w or not w.active:
            raise ValueError("战争不存在或已结束")
        if not w.involves(self.player_id):
            raise ValueError("只能提议自己参与的战争白和")
        atk_exh = self.sim.diplomacy.war_exhaustion.get(w.attacker_primary, 0.0)
        def_exh = self.sim.diplomacy.war_exhaustion.get(w.defender_primary, 0.0)
        # 玩家可主动提议：条件略宽于自动白和
        months = w.months_elapsed(self.sim.world.date)
        if months < 6 and abs(w.warscore) > 40:
            raise ValueError("战况未僵持，无法白和")
        if not w.can_white_peace(self.sim.world.date, atk_exh, def_exh) and months < 12:
            raise ValueError("战争时间太短或条件不足")
        self.sim.wars.end_war(war_id, WarResult.WHITE_PEACE)
        self.sim.diplomacy.set_at_war(w.attacker_primary, w.defender_primary, False)
        self.sim.diplomacy.set_truce(
            w.attacker_primary, w.defender_primary, self.sim.world.date.year + 3
        )
        an = self.sim.world.character(w.attacker_primary)
        dn = self.sim.world.character(w.defender_primary)
        self.sim.world.push_log(
            f"白和：{(an.name if an else '?')} 与 {(dn.name if dn else '?')} 停战"
        )
        self.notify("已达成白和")

    def _resolve_event(self, event_id: int, choice_id: int) -> None:
        inst = next(
            (e for e in self.sim.events.pending if e.event_id == event_id and e.character == self.player_id),
            None,
        )
        if not inst:
            return
        self.sim.events.resolve_choice(self.sim.world, inst, choice_id)
        self.sim.events.pending = [e for e in self.sim.events.pending if e is not inst]
        self.notify(f"已选择事件选项：{inst.title}")
        self._grant_action_xp(30)

    def _tutorial_next(self) -> None:
        current = self.tutorial.current_step
        self.tutorial.advance(current)
        step = self.tutorial.get_current_step()
        if step:
            self.notify(f"教程：{step.title} - {step.description}")
        else:
            self.notify("教程已完成！")

    def _fabricate_claim(self, county_id: int) -> None:
        county = self.sim.world.map.get(county_id)
        if not county:
            raise ValueError("省份不存在")
        if county.holder == self.player_id:
            raise ValueError("已是己方领地")
        player = self.sim.world.character(self.player_id)
        if not player or player.gold < 50:
            raise ValueError("金币不足（需要 50）")
        player.add_gold(-50)
        # 找到该省的头衔
        title_id = county.owner_title
        if title_id == NONE_ID:
            # 无头衔则用省份序号造一个虚拟宣称
            self.sim.diplomacy.add_claim(self.player_id, NONE_ID, county_id, 60)
        else:
            self.sim.diplomacy.add_claim(self.player_id, title_id, county_id, 60)
        self.sim.world.push_log(f"{player.name} 伪造了对 {county.name} 的宣称")
        self.notify(f"已伪造对 {county.name} 的宣称（花费 50 金）")
        self._grant_action_xp(40)

    def _upgrade_building(self, county_id: int, building_kind_name: Any) -> None:
        county = self.sim.world.map.get(county_id)
        if not county:
            raise ValueError("省份不存在")
        if county.holder != self.player_id:
            raise ValueError("不是己方领地")
        if not building_kind_name:
            raise ValueError("缺少建筑类型")
        try:
            kind = BuildingKind[building_kind_name]
        except KeyError:
            raise ValueError(f"未知建筑类型: {building_kind_name}")
        player = self.sim.world.character(self.player_id)
        if not player:
            raise ValueError("玩家无效")
        b = self.sim.buildings.get_building(county_id, kind)
        if not b:
            # 新建建筑
            cost = kind.upgrade_cost(0)
            if player.gold < cost:
                raise ValueError(f"金币不足（需要 {cost}）")
            player.add_gold(-cost)
            self.sim.buildings.add_building(county_id, kind)
            self.sim.world.push_log(f"{player.name} 在 {county.name} 建造了 {kind.name_zh()}")
            self.notify(f"建造 {kind.name_zh()}（花费 {cost} 金）")
            self._grant_action_xp(20)
        else:
            # 升级建筑
            if not b.can_upgrade():
                raise ValueError(f"{kind.name_zh()} 已达最高级")
            cost = b.upgrade_cost()
            if player.gold < cost:
                raise ValueError(f"金币不足（需要 {cost}）")
            player.add_gold(-cost)
            b.level += 1
            self.sim.world.push_log(f"{player.name} 将 {county.name} 的 {kind.name_zh()} 升级到 {b.level} 级")
            self.notify(f"{kind.name_zh()} 升级到 {b.level} 级（花费 {cost} 金）")
            self._grant_action_xp(20)
