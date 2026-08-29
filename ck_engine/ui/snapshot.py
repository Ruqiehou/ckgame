"""GameAPI 的快照构建（snapshot）部分。

以 Mixin 形式并入 GameAPI，见 ck_engine/ui/api.py。
"""

from __future__ import annotations

from typing import Any, Dict, List, Optional

from ck_engine.ai.personality import AiPersonality
from ck_engine.core import NONE_ID
from ck_engine.core.balance import SUPPLY_LOW_THRESHOLD, SUPPLY_MOVE_SLOW_THRESHOLD
from ck_engine.politics.council import CouncilPosition, CouncilTask
from ck_engine.ui.map_layout import layout_for, points_to_svg, sea_band, viewbox


class SnapshotMixin:
    """snapshot() 及其辅助方法，依赖宿主提供 sim/player_id/messages 等属性。"""

    def _snapshot_unlocked(self) -> Dict[str, Any]:
        w = self.sim.world
        player = w.character(self.player_id)
        counties = []
        for county in w.map.iter():
            layout = layout_for(county.name)
            holder = w.character(county.holder)
            color = self._holder_color(county.holder)
            armies_here = [
                a.id
                for a in self.sim.wars.armies.values()
                if a.is_active() and a.location == county.id
            ]
            siege = self.sim.sieges.active_at(county.id)
            counties.append(
                {
                    "id": county.id,
                    "name": county.name,
                    "terrain": county.terrain.name,
                    "development": county.development,
                    "dev_cap": county.terrain.development_cap(),
                    "control": round(county.control, 1),
                    "levies": county.monthly_levies(),
                    "tax": round(county.monthly_tax(), 2),
                    "fort": county.fort_level,
                    "buildings": self._county_buildings(county.id),
                    "holder_id": county.holder if county.holder != NONE_ID else None,
                    "holder_name": holder.name if holder else "无主",
                    "color": color,
                    "cx": layout["cx"],
                    "cy": layout["cy"],
                    "points": points_to_svg(layout["points"]),
                    "neighbors": list(county.neighbors),
                    "armies": armies_here,
                    "siege": (
                        {
                            "progress": round(siege.progress, 1),
                            "required": round(siege.required_progress(), 1),
                            "attacker": siege.attacker,
                        }
                        if siege
                        else None
                    ),
                    "is_player": bool(holder and holder.id == self.player_id),
                    "has_port": county.has_port,
                    "port_level": county.port_level,
                    "port_income": round(county.port_income, 1),
                    "trade_route_protected": county.trade_route_protected,
                    "trade_route_protection_level": county.trade_route_protection_level,
                    "trade_route_maintenance_level": county.trade_route_maintenance_level,
                    "trade_route_upgrade_cost": round(county.trade_route_upgrade_cost, 1),
                }
            )

        edges = set()
        for county in w.map.iter():
            for n in county.neighbors:
                a, b = sorted((county.id, n))
                edges.add((a, b))
        edge_list = []
        by_id = {c["id"]: c for c in counties}
        for a, b in edges:
            ca, cb = by_id.get(a), by_id.get(b)
            if ca and cb:
                edge_list.append(
                    {"x1": ca["cx"], "y1": ca["cy"], "x2": cb["cx"], "y2": cb["cy"]}
                )

        armies = []
        for a in self.sim.wars.armies.values():
            if not a.is_active():
                continue
            loc = w.map.get(a.location)
            owner = w.character(a.owner)
            layout = layout_for(loc.name) if loc else {"cx": 0, "cy": 0}
            supply = round(a.supply, 1)
            in_enemy = False
            if loc:
                for war in self.sim.wars.active_wars():
                    if not war.involves(a.owner):
                        continue
                    enemies = {
                        p.character
                        for p in war.participants
                        if war.is_attacker(p.character) != war.is_attacker(a.owner)
                    }
                    if loc.holder in enemies:
                        in_enemy = True
                        break
            armies.append(
                {
                    "id": a.id,
                    "name": a.name,
                    "owner_id": a.owner,
                    "owner_name": owner.name if owner else "?",
                    "location": a.location,
                    "location_name": loc.name if loc else "?",
                    "men": a.total_men(),
                    "status": a.status.name,
                    "morale": round(a.morale, 1),
                    "supply": supply,
                    "supply_low": supply < SUPPLY_LOW_THRESHOLD,
                    "supply_slow": supply < SUPPLY_MOVE_SLOW_THRESHOLD,
                    "in_enemy": in_enemy,
                    "cx": layout["cx"],
                    "cy": layout["cy"] - 18,
                    "is_player": a.owner == self.player_id,
                    "path": list(a.path),
                }
            )

        wars = []
        for war in self.sim.wars.wars.values():
            atk = w.character(war.attacker_primary)
            dfd = w.character(war.defender_primary)
            months = war.months_elapsed(w.date) if war.active else 0
            atk_exh = self.sim.diplomacy.war_exhaustion.get(war.attacker_primary, 0.0)
            def_exh = self.sim.diplomacy.war_exhaustion.get(war.defender_primary, 0.0)
            can_wp = False
            if war.active and war.involves(self.player_id):
                can_wp = war.can_white_peace(w.date, atk_exh, def_exh) or (
                    months >= 12 or (months >= 6 and abs(war.warscore) <= 40)
                )
            wars.append(
                {
                    "id": war.id,
                    "name": war.name,
                    "active": war.active,
                    "warscore": war.warscore,
                    "cb": war.cb.name_zh(),
                    "attacker": atk.name if atk else "?",
                    "defender": dfd.name if dfd else "?",
                    "involves_player": war.involves(self.player_id),
                    "months": months,
                    "can_white_peace": can_wp,
                }
            )

        rulers = []
        for r in w.rulers():
            attrs = w.effective_attrs(r.id)
            title = w.title(r.primary_title)
            profile = AiPersonality.profile_of(w, r.id)
            rulers.append(
                {
                    "id": r.id,
                    "name": r.name,
                    "title": title.name if title else "无",
                    "gold": round(r.gold, 1),
                    "prestige": round(r.prestige, 1),
                    "martial": attrs.martial if attrs else 0,
                    "income": round(w.monthly_income_of(r.id), 1),
                    "men": self.sim.wars.total_men_of(r.id),
                    "persona": AiPersonality.describe(profile),
                    "is_player": r.id == self.player_id,
                }
            )

        log = w.log[-30:]
        player_info = None
        if player:
            title = w.title(player.primary_title)
            attrs = w.effective_attrs(player.id)
            player_info = {
                "id": player.id,
                "name": player.name,
                "title": title.name if title else "无",
                "gold": round(player.gold, 1),
                "prestige": round(player.prestige, 1),
                "piety": round(player.piety, 1),
                "stress": player.stress,
                "health": round(player.health, 2),
                "attrs": {
                    "diplomacy": attrs.diplomacy if attrs else 0,
                    "martial": attrs.martial if attrs else 0,
                    "stewardship": attrs.stewardship if attrs else 0,
                    "intrigue": attrs.intrigue if attrs else 0,
                    "learning": attrs.learning if attrs else 0,
                    "prowess": attrs.prowess if attrs else 0,
                },
                "income": round(w.monthly_income_of(player.id), 1),
                "men": self.sim.wars.total_men_of(player.id),
                "laws": self._player_laws(),
                "level": player.level,
                "xp": player.xp,
                "xp_to_next": player.xp_to_next_level(),
            }

        playable = [
            {"id": r.id, "name": r.name, "title": (w.title(r.primary_title).name if w.title(r.primary_title) else "")}
            for r in w.rulers()
        ]

        factions = []
        for f in self.sim.factions.factions.values():
            if f.target_liege != self.player_id:
                continue
            factions.append(
                {
                    "id": f.id,
                    "kind": f.kind.name_zh() if hasattr(f.kind, "name_zh") else f.kind.name,
                    "members": len(f.members),
                    "power": round(f.power, 1),
                    "discontent": round(f.discontent, 1),
                    "ultimatum": f.ultimatum_sent,
                }
            )

        player_exh = round(self.sim.diplomacy.war_exhaustion.get(self.player_id, 0.0), 1)

        return {
            "date": str(w.date),
            "season": w.date.season().name,
            "tick": w.tick,
            "player": player_info,
            "player_war_exhaustion": player_exh,
            "playable": playable,
            "counties": counties,
            "edges": edge_list,
            "armies": armies,
            "wars": wars,
            "factions": factions,
            "pending_ultimatums": self._pending_ultimatums(),
            "rulers": rulers,
            "log": log,
            "messages": self.messages[-12:],
            "selected_county": self.selected_county,
            "selected_army": self.selected_army,
            "sea": sea_band(),
            "viewbox": viewbox(),
            "supply_low_threshold": SUPPLY_LOW_THRESHOLD,
            "saves": self._list_saves(),
            "cheat_mode": self.cheat_mode,
            "infinite_gold_mode": self.infinite_gold_mode,
            "pending_events": [
                {
                    "event_id": inst.event_id,
                    "title": inst.title,
                    "description": inst.description,
                    "choices": [
                        {"id": c.id, "text": c.text, "ai_weight": c.ai_weight}
                        for c in inst.choices
                    ],
                }
                for inst in self.sim.events.pending
                if inst.character == self.player_id
            ],
            "player_schemes": self._player_schemes(),
            "player_council": self._player_council(),
            "player_claims": self._player_claims(),
            "treaties": self._player_treaties(),
            "characters": self._all_characters(),
            "scheme_types": [(k, v) for k, v in [
                ("MURDER", "谋杀"), ("ABDUCT", "绑架"), ("FABRICATE_HOOK", "伪造把柄"),
                ("SWAY", "拉拢"), ("SEDUCE", "引诱"), ("CLAIM_FABRICATION", "伪造宣称"),
            ]],
            "council_positions": [(p.name, p.name_zh()) for p in CouncilPosition.all()],
            "council_tasks": [(t.name, t.name_zh()) for t in CouncilTask],
            "storylines": self._player_storylines(),
            "tutorial": self._tutorial_snapshot(),
            "trade_routes": w.trade_routes,
            "exchange_rates": w.exchange_rates,
            "trade_events": w.trade_events,
            "decisions": self._player_decisions(),
            "chains": self._player_chains(),
            "family": self._player_family(),
        }

    def _player_decisions(self) -> List[Dict[str, Any]]:
        """返回玩家可执行的决策及原因。"""
        out = []
        for d, reason in self.sim.decisions.available(self.sim.world, self.player_id):
            out.append({
                "id": d.id,
                "title": d.title,
                "category": d.category.name,
                "description": d.description,
                "available": reason == "",
                "reason": reason if reason else None,
                "cost_gold": d.cost_gold,
                "cost_prestige": d.cost_prestige,
            })
        return out

    def _player_chains(self) -> List[Dict[str, Any]]:
        """返回活跃事件链。"""
        out = []
        for ch in self.sim.chains.active_chains:
            stages = []
            for i, s in enumerate(ch.stages):
                stages.append({
                    "id": s.id,
                    "title": s.title,
                    "description": s.description,
                })
            out.append({
                "id": ch.id,
                "title": ch.title,
                "description": ch.description,
                "current_stage": ch.current_stage,
                "stages": stages,
            })
        return out

    def _player_family(self) -> Dict[str, Any]:
        """返回玩家家族信息。"""
        player = self.sim.world.character(self.player_id)
        if not player:
            return {"spouses": [], "children": []}
        spouses = []
        for sid in player.spouses:
            s = self.sim.world.character(sid)
            if s:
                spouses.append({
                    "id": s.id,
                    "name": s.name,
                    "age": s.age_at(self.sim.world.date),
                })
        children = []
        for cid in player.children:
            c = self.sim.world.character(cid)
            if not c or not c.is_alive() or c.id == self.player_id:
                continue
            status = "已配偶"
            if c.is_married():
                spouse = self.sim.world.character(c.spouses[0])
                status = f"已配偶{spouse.name if spouse else '?'}"
            elif c.betrothed_to != NONE_ID:
                bt = self.sim.world.character(c.betrothed_to)
                status = f"订婚{bt.name if bt else '?'}"
            children.append({
                "id": c.id,
                "name": c.name,
                "age": c.age_at(self.sim.world.date),
                "gender": c.gender.name,
                "status": status,
            })
        return {"spouses": spouses, "children": children}

    def _holder_color(self, holder_id: int) -> str:
        if holder_id == NONE_ID:
            return "#4a5568"
        w = self.sim.world
        c = w.character(holder_id)
        if not c:
            return "#4a5568"
        d = w.dynasties.get(c.dynasty)
        if d and d.color:
            r, g, b = d.color
            return f"rgb({r},{g},{b})"
        # 按 id 生成稳定色
        hue = (holder_id * 47) % 360
        return f"hsl({hue} 55% 42%)"

    def _county_buildings(self, county_id: int) -> List[Dict]:
        buildings = []
        for b in self.sim.buildings.get_buildings(county_id):
            buildings.append({
                "kind": b.kind.name,
                "name": b.kind.name_zh(),
                "level": b.level,
                "max_level": b.kind.max_level(),
                "can_upgrade": b.can_upgrade(),
                "upgrade_cost": b.upgrade_cost(),
                "description": b.kind.description(),
            })
        return buildings

    # ---------- snapshot 辅助 ----------
    def _pending_ultimatums(self) -> List[Dict[str, Any]]:
        out = []
        for u in self.sim.pending_ultimatums.values():
            if u.liege != self.player_id:
                continue
            out.append(
                {
                    "faction_id": u.faction_id,
                    "kind": u.kind.name,
                    "kind_zh": u.kind.name_zh(),
                    "text": u.kind.ultimatum_text(),
                    "members": len(u.members),
                }
            )
        return out

    def _player_schemes(self) -> List[Dict[str, Any]]:
        out = []
        for s in self.sim.schemes.schemes.values():
            if s.owner != self.player_id or s.exposed or s.is_complete():
                continue
            target = self.sim.world.character(s.target)
            out.append({
                "id": s.id,
                "kind": s.kind.name,
                "kind_zh": s.kind.name_zh(),
                "target_id": s.target,
                "target_name": target.name if target else "?",
                "progress": round(s.progress, 1),
                "secrecy": round(s.secrecy, 1),
            })
        return out

    def _player_council(self) -> Optional[Dict[str, Any]]:
        council = self.sim.councils.get(self.player_id)
        if not council:
            return None
        w = self.sim.world
        members = []
        for pos in CouncilPosition.all():
            who = council.get(pos)
            task = council.task_of(pos)
            ch = w.character(who) if who != NONE_ID else None
            members.append({
                "position": pos.name,
                "position_zh": pos.name_zh(),
                "holder_id": who if who != NONE_ID else None,
                "holder_name": ch.name if ch else "（空缺）",
                "task": task.name,
                "task_zh": task.name_zh(),
            })
        return {"members": members}

    def _player_storylines(self) -> List[Dict[str, Any]]:
        out: List[Dict[str, Any]] = []
        for s in self.sim.storylines.storylines:
            if s.character_id == 0 or s.character_id == self.player_id:
                out.append({
                    "id": s.id,
                    "title": s.title,
                    "description": s.description,
                    "status": s.status.name,
                    "current_stage": s.current_stage,
                    "stage_title": next((st.title for st in s.stages if st.stage_id == s.current_stage), ""),
                    "stage_description": next((st.description for st in s.stages if st.stage_id == s.current_stage), ""),
                    "tags": s.tags,
                })
        return out

    def _tutorial_snapshot(self) -> Dict[str, Any]:
        step = self.tutorial.get_current_step()
        return {
            "enabled": self.tutorial.enabled,
            "current_step": self.tutorial.current_step.name,
            "current_step_zh": self.tutorial.current_step.name_zh(),
            "title": step.title if step else "",
            "description": step.description if step else "",
            "hint": step.hint if step else "",
            "action_hint": step.action_hint if step else "",
            "completed": self.tutorial.is_completed(),
            "completed_steps": [s.name for s in self.tutorial.completed_steps],
        }

    def _player_claims(self) -> List[Dict[str, Any]]:
        out = []
        w = self.sim.world
        for claim in self.sim.diplomacy.claims_of(self.player_id):
            title = w.title(claim.title) if claim.title != NONE_ID else None
            county = w.map.get(claim.county) if claim.county else None
            out.append({
                "title_id": claim.title if claim.title != NONE_ID else None,
                "title_name": title.name if title else None,
                "county_id": claim.county,
                "county_name": county.name if county else None,
                "strength": claim.strength,
                "pressed": claim.pressed,
            })
        return out

    def _player_treaties(self) -> List[Dict[str, Any]]:
        out = []
        w = self.sim.world
        for t in self.sim.diplomacy.treaties:
            if t.a != self.player_id and t.b != self.player_id:
                continue
            other_id = t.b if t.a == self.player_id else t.a
            other = w.character(other_id)
            out.append({
                "kind": t.kind.name,
                "kind_zh": t.kind.name_zh(),
                "other_id": other_id,
                "other_name": other.name if other else "?",
                "expires_year": t.expires_year,
            })
        # 停战
        for (a, b), until in self.sim.diplomacy.truce_until.items():
            if self.player_id not in (a, b):
                continue
            other_id = b if a == self.player_id else a
            other = w.character(other_id)
            if until > w.date.year:
                out.append({
                    "kind": "TRUCE",
                    "kind_zh": "停战",
                    "other_id": other_id,
                    "other_name": other.name if other else "?",
                    "expires_year": until,
                })
        return out

    def _all_characters(self) -> List[Dict[str, Any]]:
        """输出所有存活角色的简要信息，供前端人物详情面板使用。"""
        w = self.sim.world
        out = []
        for c in w.alive_characters():
            attrs = w.effective_attrs(c.id)
            dynasty = w.dynasties.get(c.dynasty)
            title = w.title(c.primary_title) if c.primary_title != NONE_ID else None
            out.append({
                "id": c.id,
                "name": c.name,
                "dynasty_name": dynasty.name if dynasty else "",
                "gender": c.gender.name,
                "age": c.age_at(w.date),
                "is_ruler": c.is_ruler,
                "title": title.name if title else "",
                "gold": round(c.gold, 0),
                "prestige": round(c.prestige, 0),
                "is_married": c.is_married(),
                "spouse_ids": list(c.spouses),
                "attrs": {
                    "diplomacy": attrs.diplomacy if attrs else 0,
                    "martial": attrs.martial if attrs else 0,
                    "stewardship": attrs.stewardship if attrs else 0,
                    "intrigue": attrs.intrigue if attrs else 0,
                    "learning": attrs.learning if attrs else 0,
                    "prowess": attrs.prowess if attrs else 0,
                } if attrs else None,
                "opinion_of_player": w.opinion(c.id, self.player_id),
                "player_opinion": w.opinion(self.player_id, c.id),
                "relation_allied": self.sim.diplomacy.are_allied(self.player_id, c.id),
                "relation_rival": self.sim.diplomacy.flags(self.player_id, c.id).rival,
                "relation_at_war": self.sim.diplomacy.flags(self.player_id, c.id).at_war,
                "relation_marriage": self.sim.diplomacy.flags(self.player_id, c.id).marriage_pact,
                "relation_vassalage": self.sim.diplomacy.flags(self.player_id, c.id).vassalage,
                "relation_trade_agreement": self.sim.diplomacy.flags(self.player_id, c.id).trade_agreement,
                "relation_intelligence_sharing": self.sim.diplomacy.flags(self.player_id, c.id).intelligence_sharing,
                "held_title_ids": list(c.held_titles),
                "level": c.level,
                "xp": c.xp,
                "xp_to_next": c.xp_to_next_level(),
            })
        return out
