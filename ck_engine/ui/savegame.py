"""GameAPI 的存档/读档（save/load）部分。

以 Mixin 形式并入 GameAPI，见 ck_engine/ui/api.py。
"""

from __future__ import annotations

import json
import re
from pathlib import Path
from typing import Any, List

from ck_engine.core import GameDate
from ck_engine.game.simulation import GameSimulation, PendingUltimatum
from ck_engine.military.army import Army, ArmyStatus, UnitType
from ck_engine.military.siege import Siege
from ck_engine.military.war import War, WarParticipant, WarResult
from ck_engine.politics.council import Council, CouncilPosition, CouncilTask
from ck_engine.politics.diplomacy import (
    CasusBelli,
    Claim,
    RelationFlags,
    Treaty,
    TreatyKind,
)
from ck_engine.politics.factions import Faction, FactionKind
from ck_engine.politics.schemes import Scheme, SchemeKind
from ck_engine.world.buildings import BuildingKind
from ck_engine.world.character import LifeState


class SaveGameMixin:
    """存档管理方法，依赖宿主提供 sim/player_id/messages/save_path 等属性。"""

    def _save_file(self, name: Any = None) -> Path:
        if name is None or str(name).strip() == "":
            return self.save_path
        raw = str(name).strip()
        raw = re.sub(r"[\\/:*?\"<>|]+", "_", raw)
        raw = re.sub(r"\s+", "_", raw)
        safe = Path(raw).name[:48] or "autosave"
        if safe.lower().endswith(".json"):
            safe = safe[:-5]
        return self.save_path.parent / f"{safe}.json"

    def _list_saves(self) -> List[dict]:
        folder = self.save_path.parent
        if not folder.exists():
            return []
        rows: List[dict] = []
        for path in sorted(folder.glob("*.json"), key=lambda p: p.stat().st_mtime, reverse=True):
            row: dict = {
                "name": path.stem,
                "file": path.name,
                "mtime": int(path.stat().st_mtime),
                "size": path.stat().st_size,
            }
            try:
                data = json.loads(path.read_text(encoding="utf-8"))
                date = data.get("date")
                if isinstance(date, list) and len(date) == 3:
                    row["date"] = f"{date[0]:04d}-{date[1]:02d}-{date[2]:02d}"
                row["player_id"] = data.get("player_id")
            except Exception:
                row["broken"] = True
            rows.append(row)
        return rows

    def _delete_save(self, name: str) -> None:
        path = self._save_file(name)
        if path.stem == "autosave":
            raise ValueError("不能删除自动存档")
        if not path.exists():
            raise ValueError("存档不存在")
        path.unlink()
        self.notify(f"已删除存档 → {path.name}")

    def _save(self, name: Any = None) -> None:
        """完整快照：日期、玩家、人物、省份、头衔、战争、外交、派系、阴谋、军团、围城。"""
        w = self.sim.world
        sim = self.sim
        data = {
            "date": [w.date.year, w.date.month, w.date.day],
            "tick": w.tick,
            "player_id": self.player_id,
            "scenario": sim.scenario_id,
            "characters": {
                str(c.id): {
                    "gold": c.gold,
                    "prestige": c.prestige,
                    "piety": c.piety,
                    "stress": c.stress,
                    "health": c.health,
                    "life": c.life.name,
                    "held_titles": list(c.held_titles),
                    "primary_title": c.primary_title,
                    "is_ruler": c.is_ruler,
                    "father": c.father,
                    "mother": c.mother,
                    "spouses": list(c.spouses),
                    "children": list(c.children),
                    "betrothed_to": c.betrothed_to,
                    "education_focus": c.education_focus,
                    "traits": list(c.traits),
                    "opinion_cache": dict(c.opinion_cache),
                }
                for c in w.characters.values()
            },
            "counties": {
                str(c.id): {
                    "holder": c.holder, "control": c.control, "development": c.development,
                    "buildings": [
                        {"kind": b.kind.name, "level": b.level}
                        for b in self.sim.buildings.get_buildings(c.id)
                    ],
                }
                for c in w.map.iter()
            },
            "titles": {
                str(t.id): {"holder": t.holder, "de_facto_liege": t.de_facto_liege, "de_facto_vassals": list(t.de_facto_vassals)}
                for t in w.titles.values()
            },
            "wars": [
                {
                    "id": war.id, "name": war.name, "cb": war.cb.name,
                    "attacker_primary": war.attacker_primary, "defender_primary": war.defender_primary,
                    "start": [war.start.year, war.start.month, war.start.day],
                    "warscore": war.warscore, "active": war.active,
                    "result": war.result.name,
                    "participants": [{"character": p.character, "is_attacker": p.is_attacker, "joined": [p.joined.year, p.joined.month, p.joined.day]} for p in war.participants],
                }
                for war in sim.wars.wars.values()
            ],
            "armies": [
                {
                    "id": a.id, "owner": a.owner, "name": a.name, "location": a.location,
                    "commander": a.commander, "status": a.status.name, "morale": a.morale,
                    "supply": a.supply,
                    "path": list(a.path), "stacks": [{"unit_type": s.unit_type.name, "men": s.men} for s in a.stacks],
                }
                for a in sim.wars.armies.values() if a.status != ArmyStatus.DISBANDED
            ],
            "sieges": [
                {
                    "id": s.id, "county": s.county, "attacker_army": s.attacker_army,
                    "attacker": s.attacker, "defender": s.defender, "fort_level": s.fort_level,
                    "garrison": s.garrison, "progress": s.progress, "started": [s.started.year, s.started.month, s.started.day] if s.started else None,
                }
                for s in sim.sieges.sieges.values() if s.active
            ],
            "diplomacy": {
                "relations": {f"{k[0]},{k[1]}": {"allied": v.allied, "at_war": v.at_war, "rival": v.rival, "marriage_pact": v.marriage_pact, "non_aggression": v.non_aggression} for k, v in sim.diplomacy.relations.items()},
                "treaties": [{"a": t.a, "b": t.b, "kind": t.kind.name, "start": [t.start.year, t.start.month, t.start.day], "expires_year": t.expires_year} for t in sim.diplomacy.treaties],
                "claims": {str(k): [{"title": c.title, "county": c.county, "strength": c.strength, "pressed": c.pressed} for c in v] for k, v in sim.diplomacy.claims.items()},
                "truce_until": {f"{k[0]},{k[1]}": v for k, v in sim.diplomacy.truce_until.items()},
                "war_exhaustion": dict(sim.diplomacy.war_exhaustion),
            },
            "factions": [
                {
                    "id": f.id, "kind": f.kind.name, "target_liege": f.target_liege,
                    "members": list(f.members), "power": f.power, "discontent": f.discontent,
                }
                for f in sim.factions.factions.values()
            ],
            "pending_ultimatums": [
                {
                    "faction_id": u.faction_id, "kind": u.kind.name,
                    "liege": u.liege, "members": list(u.members),
                }
                for u in sim.pending_ultimatums.values()
            ],
            "schemes": [
                {
                    "id": s.id, "kind": s.kind.name, "owner": s.owner, "target": s.target,
                    "progress": s.progress, "secrecy": s.secrecy,
                    "started": [s.started.year, s.started.month, s.started.day] if s.started else None,
                    "exposed": s.exposed,
                }
                for s in sim.schemes.schemes.values()
                if not s.exposed and not s.is_complete()
            ],
            "decisions": {
                "cooldowns": dict(sim.decisions.cooldowns),
                "history": list(sim.decisions.history),
            },
            "gameplays": sim.gameplays.save_state(),
            "chains": [
                {
                    "id": chain.id,
                    "active": chain.active,
                    "completed": chain.completed,
                    "current_stage": chain.current_stage,
                    "participants": list(chain.participants),
                    "start_date": [chain.start_date.year, chain.start_date.month, chain.start_date.day] if chain.start_date else None,
                    "stage_started": [chain.stage_started.year, chain.stage_started.month, chain.stage_started.day] if chain.stage_started else None,
                }
                for chain in sim.chains.chains
                if chain.active or chain.completed
            ],
            "councils": {
                str(rid): {
                    "chancellor": c.chancellor,
                    "marshal": c.marshal,
                    "steward": c.steward,
                    "spymaster": c.spymaster,
                    "chaplain": c.chaplain,
                    "tasks": {pos.name: task.name for pos, task in c.tasks.items()},
                }
                for rid, c in sim.councils.by_ruler.items()
            },
            "log": w.log[-100:],
            "messages": self.messages[-20:],
        }
        path = self._save_file(name)
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
        if name is None:
            self.save_path = path
        self.notify(f"已存档 → {path.name}")

    def _load(self, name: Any = None) -> None:
        path = self._save_file(name)
        if not path.exists():
            raise ValueError("没有存档")
        data = json.loads(path.read_text(encoding="utf-8"))
        # 新开局再覆盖动态状态，保证 ID 一致
        self.sim = GameSimulation(data.get("scenario") or self.sim.scenario_id)
        w = self.sim.world
        sim = self.sim

        y, m, d = data["date"]
        w.date = GameDate(y, m, d)
        w.tick = int(data.get("tick", 0))
        for cid, row in data.get("characters", {}).items():
            c = w.character(int(cid))
            if not c:
                continue
            c.gold = row["gold"]
            c.prestige = row["prestige"]
            c.piety = row.get("piety", c.piety)
            c.stress = row.get("stress", 0)
            c.health = row.get("health", c.health)
            c.father = row.get("father", c.father)
            c.mother = row.get("mother", c.mother)
            c.spouses = list(row.get("spouses", []))
            c.children = list(row.get("children", []))
            c.betrothed_to = row.get("betrothed_to", c.betrothed_to)
            c.education_focus = row.get("education_focus", c.education_focus)
            c.education_years = int(row.get("education_years", 0))
            c.traits = list(row.get("traits", []))
            c.opinion_cache = {int(k): v for k, v in row.get("opinion_cache", {}).items()}
            if row.get("life") == "DEAD":
                c.life = LifeState.DEAD
                c.is_ruler = False
            else:
                c.held_titles = list(row.get("held_titles", []))
                c.primary_title = row.get("primary_title", c.primary_title)
                c.is_ruler = bool(row.get("is_ruler", c.is_ruler))
        for tid, row in data.get("titles", {}).items():
            t = w.title(int(tid))
            if t:
                t.holder = row["holder"]
                t.de_facto_liege = row.get("de_facto_liege", t.de_facto_liege)
                t.de_facto_vassals = list(row.get("de_facto_vassals", []))
        for cid, row in data.get("counties", {}).items():
            county = w.map.get(int(cid))
            if county:
                county.holder = row["holder"]
                county.control = row.get("control", county.control)
                county.development = row.get("development", county.development)
                for b_row in row.get("buildings", []):
                    try:
                        kind = BuildingKind[b_row["kind"]]
                        b = self.sim.buildings.add_building(county.id, kind)
                        b.level = b_row.get("level", 0)
                    except KeyError:
                        continue

        # 恢复战争
        sim.wars.wars.clear()
        sim.wars.next_war = 1
        for row in data.get("wars", []):
            war = War(
                id=row["id"], name=row["name"], cb=CasusBelli[row["cb"]],
                attacker_primary=row["attacker_primary"], defender_primary=row["defender_primary"],
                start=GameDate(*row["start"]), warscore=row["warscore"], active=row["active"],
                result=WarResult[row["result"]],
            )
            war.participants = [
                WarParticipant(character=p["character"], is_attacker=p["is_attacker"], joined=GameDate(*p["joined"]))
                for p in row.get("participants", [])
            ]
            sim.wars.wars[war.id] = war
            sim.wars.next_war = max(sim.wars.next_war, war.id + 1)

        # 恢复军团
        sim.wars.armies.clear()
        sim.wars.next_army = 1
        for row in data.get("armies", []):
            army = Army(
                id=row["id"], owner=row["owner"], name=row["name"],
                location=row["location"], commander=row["commander"],
            )
            army.status = ArmyStatus[row["status"]]
            army.morale = row.get("morale", 100.0)
            army.supply = row.get("supply", army.supply)
            army.path = list(row.get("path", []))
            for s in row.get("stacks", []):
                army.add_men(UnitType[s["unit_type"]], s["men"])
            sim.wars.armies[army.id] = army
            sim.wars.next_army = max(sim.wars.next_army, army.id + 1)

        # 恢复围城
        sim.sieges.sieges.clear()
        sim.sieges.next_id = 1
        for row in data.get("sieges", []):
            started = GameDate(*row["started"]) if row.get("started") else None
            siege = Siege(
                id=row["id"], county=row["county"], attacker_army=row["attacker_army"],
                attacker=row["attacker"], defender=row["defender"], fort_level=row["fort_level"],
                garrison=row["garrison"], started=started,
            )
            siege.progress = row.get("progress", 0.0)
            sim.sieges.sieges[siege.id] = siege
            sim.sieges.next_id = max(sim.sieges.next_id, siege.id + 1)

        # 恢复外交
        sim.diplomacy.relations.clear()
        for k, v in data.get("diplomacy", {}).get("relations", {}).items():
            a, b = map(int, k.split(","))
            sim.diplomacy.relations[(a, b)] = RelationFlags(
                allied=v.get("allied", False), at_war=v.get("at_war", False),
                rival=v.get("rival", False), marriage_pact=v.get("marriage_pact", False),
                non_aggression=v.get("non_aggression", False),
            )
        sim.diplomacy.treaties.clear()
        for row in data.get("diplomacy", {}).get("treaties", []):
            sim.diplomacy.treaties.append(Treaty(
                a=row["a"], b=row["b"], kind=TreatyKind[row["kind"]],
                start=GameDate(*row["start"]), expires_year=row["expires_year"],
            ))
        sim.diplomacy.claims.clear()
        for k, v in data.get("diplomacy", {}).get("claims", {}).items():
            sim.diplomacy.claims[int(k)] = [
                Claim(claimant=int(k), title=c["title"], county=c.get("county"), strength=c.get("strength", 50), pressed=c.get("pressed", False))
                for c in v
            ]
        sim.diplomacy.truce_until.clear()
        for k, v in data.get("diplomacy", {}).get("truce_until", {}).items():
            a, b = map(int, k.split(","))
            sim.diplomacy.truce_until[(a, b)] = v
        sim.diplomacy.war_exhaustion = {int(k): v for k, v in data.get("diplomacy", {}).get("war_exhaustion", {}).items()}

        # 恢复派系
        sim.factions.factions.clear()
        sim.factions.next_id = 1
        for row in data.get("factions", []):
            faction = Faction(
                id=row["id"], kind=FactionKind[row["kind"]], target_liege=row["target_liege"],
                members=list(row["members"]), power=row["power"], discontent=row["discontent"],
            )
            sim.factions.factions[faction.id] = faction
            sim.factions.next_id = max(sim.factions.next_id, faction.id + 1)

        # 恢复待回应的最后通牒
        sim.pending_ultimatums.clear()
        for row in data.get("pending_ultimatums", []):
            u = PendingUltimatum(
                faction_id=row["faction_id"], kind=FactionKind[row["kind"]],
                liege=row["liege"], members=list(row["members"]),
            )
            sim.pending_ultimatums[u.faction_id] = u

        # 恢复阴谋
        sim.schemes.schemes.clear()
        sim.schemes.next_id = 1
        for row in data.get("schemes", []):
            started = GameDate(*row["started"]) if row.get("started") else None
            scheme = Scheme(
                id=row["id"], kind=SchemeKind[row["kind"]], owner=row["owner"], target=row["target"],
                started=started,
            )
            scheme.progress = row.get("progress", 0.0)
            scheme.secrecy = row.get("secrecy", 100.0)
            scheme.exposed = bool(row.get("exposed", False))
            sim.schemes.schemes[scheme.id] = scheme
            sim.schemes.next_id = max(sim.schemes.next_id, scheme.id + 1)

        # 恢复重大决策与事件链
        decision_data = data.get("decisions", {})
        sim.decisions.cooldowns = {
            str(k): int(v) for k, v in decision_data.get("cooldowns", {}).items()
        }
        sim.decisions.history = list(decision_data.get("history", []))

        # 恢复玩法状态（狩猎冷却、宫廷医师等）
        sim.gameplays.load_state(data.get("gameplays", {}))
        chain_rows = {row.get("id"): row for row in data.get("chains", [])}
        sim.chains.active_chains.clear()
        for chain in sim.chains.chains:
            row = chain_rows.get(chain.id)
            if not row:
                continue
            chain.active = bool(row.get("active", False))
            chain.completed = bool(row.get("completed", False))
            chain.current_stage = min(int(row.get("current_stage", 0)), max(0, len(chain.stages) - 1))
            chain.participants = list(row.get("participants", []))
            chain.start_date = GameDate(*row["start_date"]) if row.get("start_date") else None
            chain.stage_started = GameDate(*row["stage_started"]) if row.get("stage_started") else chain.start_date
            if chain.active and not chain.completed:
                sim.chains.active_chains.append(chain)

        # 恢复内阁
        sim.councils.by_ruler.clear()
        for rid, row in data.get("councils", {}).items():
            council = Council.empty(int(rid))
            council.chancellor = row.get("chancellor", council.chancellor)
            council.marshal = row.get("marshal", council.marshal)
            council.steward = row.get("steward", council.steward)
            council.spymaster = row.get("spymaster", council.spymaster)
            council.chaplain = row.get("chaplain", council.chaplain)
            tasks = {}
            for pos_name, task_name in row.get("tasks", {}).items():
                try:
                    tasks[CouncilPosition[pos_name]] = CouncilTask[task_name]
                except KeyError:
                    continue
            if tasks:
                council.tasks = tasks
            sim.councils.by_ruler[int(rid)] = council

        w.log = list(data.get("log", []))
        self.messages = list(data.get("messages", ["读档完成"]))
        self.player_id = int(data.get("player_id", self._default_player()))
        self._sync_player()
        self.selected_county = None
        self.selected_army = None
        if name is None:
            self.save_path = path
        self.notify(f"已读档 ← {path.name}")
