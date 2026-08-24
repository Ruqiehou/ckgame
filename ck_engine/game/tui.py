"""CK 风格大战略引擎 — 命令行文字对话 TUI。

运行:
  python -m ck_engine.game.tui
"""

from __future__ import annotations

import json
import os
import sys
from pathlib import Path
from typing import Any, Dict, List

ROOT = Path(getattr(sys, "_MEIPASS", Path(__file__).resolve().parents[1]))
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from ck_engine.politics.laws import CrownAuthority, GenderLaw, SuccessionLaw
from ck_engine.ui.api import GameAPI
from ck_engine.world.buildings import BuildingKind


def clear() -> None:
    os.system("cls" if os.name == "nt" else "clear")


def pause(msg: str = "按回车继续...") -> None:
    input(msg)


class GameTUI:
    def __init__(self) -> None:
        self.api = GameAPI()
        self.save_dir = ROOT / "saves"
        self.save_dir.mkdir(exist_ok=True)

    # ---------- 工具 ----------
    def _name(self, cid: int) -> str:
        return self.api._name(cid)

    def _title_name(self, tid: int) -> str:
        w = self.api.sim.world
        t = w.title(tid)
        return t.name if t else "无"

    # ---------- 渲染 ----------
    def render(self, title: str = "") -> None:
        clear()
        snap = self.api.snapshot()
        p = snap["player"] or {}
        print("=" * 60)
        if title:
            print(f"  {title}")
        print(f"  日期: {snap['date']}  统治者: {p.get('name', '?')}")
        print(f"  头衔: {p.get('title', '无')}")
        print(f"  金: {p.get('gold', 0):.0f}  威望: {p.get('prestige', 0):.0f}  虔诚: {p.get('piety', 0):.0f}  压力: {p.get('stress', 0)}")
        attrs = p.get("attrs", {})
        if attrs:
            print(
                f"  外交:{attrs.get('diplomacy',0)} 军略:{attrs.get('martial',0)} 管理:{attrs.get('stewardship',0)} "
                f"阴谋:{attrs.get('intrigue',0)} 学识:{attrs.get('learning',0)} 勇武:{attrs.get('prowess',0)}"
            )
        print("=" * 60)

    # ---------- 主循环 ----------
    def run(self) -> None:
        print("====================================================")
        print("  CK-Style Grand Strategy Engine (TUI)")
        print("  大战略引擎 — 命令行文字对话模式")
        print("====================================================")
        snap = self.api.snapshot()
        p = snap["player"] or {}
        print(f"  当前玩家: {p.get('name', '?')}")
        print("  输入 help 查看指令")
        pause()

        while True:
            # 处理事件
            pending = [e for e in self.api.sim.events.pending if e.character == self.api.player_id]
            if pending:
                for inst in pending:
                    self._handle_event(inst)
                self.api.sim.events.pending = [e for e in self.api.sim.events.pending if e.character in self.api.sim.player_ids]

            self.render("主菜单")
            self._show_main_menu()
            cmd = input("> ").strip().lower()

            if cmd in ("exit", "quit", "q"):
                self.api.action({"action": "save"})
                print("再见！")
                break
            elif cmd == "help":
                self._show_help()
                pause()
            elif cmd == "status":
                self._show_status()
            elif cmd == "counties":
                self._show_counties()
            elif cmd == "armies":
                self._show_armies()
            elif cmd == "wars":
                self._show_wars()
            elif cmd == "council":
                self._menu_council()
            elif cmd == "schemes":
                self._menu_schemes()
            elif cmd == "diplomacy":
                self._menu_diplomacy()
            elif cmd == "raise":
                self._action_raise()
            elif cmd == "move":
                self._action_move()
            elif cmd == "disband":
                self._action_disband()
            elif cmd == "war":
                self._action_war()
            elif cmd == "improve":
                self._action_improve()
            elif cmd == "feast":
                self._action_feast()
            elif cmd == "build":
                self._menu_build()
            elif cmd == "laws":
                self._menu_laws()
            elif cmd == "peace":
                self._action_peace()
            elif cmd == "factions":
                self._menu_factions()
            elif cmd == "grant":
                self._action_grant()
            elif cmd == "claim":
                self._action_claim()
            elif cmd == "develop":
                self._action_develop()
            elif cmd == "knights":
                self._action_knights()
            elif cmd == "commander":
                self._action_commander()
            elif cmd == "claims":
                self._show_claims()
            elif cmd == "treaties":
                self._show_treaties()
            elif cmd == "rulers":
                self._show_rulers()
            elif cmd == "new":
                self._action_new_game()
            elif cmd == "player":
                self._action_switch_player()
            elif cmd == "advance":
                self._action_advance()
            elif cmd == "save":
                self.api.action({"action": "save"})
                print("已存档")
                pause()
            elif cmd == "load":
                self._menu_load()
            elif cmd == "cheat":
                self._toggle_cheat()
            else:
                print("未知指令，输入 help 查看帮助")
                pause()

    # ---------- 主菜单 ----------
    def _show_main_menu(self) -> None:
        print("\n—— 主菜单 ——")
        print("  status    查看角色状态      rulers    查看各国君主")
        print("  counties  查看领地列表      claims    查看我的宣称")
        print("  armies    查看军团          treaties  查看条约/停战")
        print("  wars      查看战争          factions  派系管理")
        print("  raise     征召军团          build     建造/升级建筑")
        print("  move      移动军团          develop   发展领地")
        print("  disband   解散军团          laws      更改法律")
        print("  war       宣战              claim     伪造宣称")
        print("  peace     求和/白和         grant     授予头衔")
        print("  improve   改善关系          knights   招募精锐")
        print("  feast     举办宴会          commander 任命指挥官")
        print("  council   内阁管理          player    切换玩家")
        print("  schemes   阴谋活动          new       新游戏")
        print("  diplomacy 外交行动          advance   推进时间")
        print("  save      存档    load      读档    cheat     作弊模式")
        print("  help      帮助    exit      退出")

    def _show_help(self) -> None:
        clear()
        print("—— 帮助 ——")
        print("  这是一个 CK 风格的大战略模拟引擎。")
        print("  你扮演一位中世纪统治者，管理领地、组建军队、进行外交和阴谋。")
        print("  输入菜单对应的英文指令来执行操作。")
        print("  时间推进时会自动处理月度事件、战斗、围城等。")
        print("  如果有事件弹出，请选择对应数字。")

    # ---------- 状态展示 ----------
    def _show_status(self) -> None:
        self.render("角色状态")
        snap = self.api.snapshot()
        p = snap["player"] or {}
        print(f"\n  金币: {p.get('gold', 0):.0f}")
        print(f"  威望: {p.get('prestige', 0):.0f}")
        print(f"  虔诚: {p.get('piety', 0):.0f}")
        print(f"  压力: {p.get('stress', 0)}")
        print(f"  健康: {p.get('health', 0):.1f}")
        print(f"  等级: {p.get('level', 1)}  XP: {p.get('xp', 0)}/{p.get('xp_to_next', 100)}")
        print(f"  月收入: {p.get('income', 0):.1f}")
        print(f"  野战军: {p.get('men', 0)}")
        print(f"  战争疲劳: {snap.get('player_war_exhaustion', 0):.1f}")

        laws = p.get("laws", {})
        if laws:
            print(f"  继承法: {laws.get('succession', '无')}")
            print(f"  王权: {laws.get('crown_authority', '无')}")
            print(f"  性别法: {laws.get('gender_law', '无')}")

        print("\n—— 近期日志 ——")
        for line in snap.get("log", [])[-8:]:
            print(f"  {line}")
        pause()

    # ---------- 领地 ----------
    def _show_counties(self) -> None:
        self.render("领地列表")
        snap = self.api.snapshot()
        print(f"\n  {'ID':<4} {'名称':<10} {'地形':<8} {'发展':<6} {'控制':<8} {'领主':<12} {'税收':<8} {'征召':<8}")
        print("  " + "-" * 70)
        for c in snap.get("counties", []):
            marker = "*" if c.get("is_player") else " "
            print(
                f"  {marker}{c['id']:<3} {c['name']:<10} {c['terrain']:<8} "
                f"{c['development']:<6} {c['control']:<8.1f} {(c.get('holder_name') or '无主'):<12} "
                f"{c['tax']:<8.1f} {c['levies']:<8}"
            )
        print(f"\n  * 表示己方领地")
        pause()

    # ---------- 军团 ----------
    def _show_armies(self) -> None:
        self.render("军团列表")
        snap = self.api.snapshot()
        print(f"\n  {'ID':<4} {'名称':<16} {'状态':<10} {'位置':<10} {'兵力':<8} {'补给':<8} {'指挥官':<10}")
        print("  " + "-" * 70)
        for a in snap.get("armies", []):
            marker = "*" if a.get("is_player") else " "
            print(
                f"  {marker}{a['id']:<3} {a['name']:<16} {a['status']:<10} "
                f"{a.get('location_name', '?'):<10} {a['men']:<8} {a['supply']:<8.1f} "
                f"{(a.get('owner_name') or '?'):<10}"
            )
        print(f"\n  * 表示己方军团")
        pause()

    # ---------- 战争 ----------
    def _show_wars(self) -> None:
        self.render("战争")
        snap = self.api.snapshot()
        wars = snap.get("wars", [])
        if not wars:
            print("\n  当前无战争")
        else:
            for w in wars:
                status = "进行中" if w.get("active") else "已结束"
                print(
                    f"  [{status}] {w.get('name', '?')} | "
                    f"{w.get('attacker', '?')} vs {w.get('defender', '?')} | "
                    f"分数:{w.get('warscore', 0):.0f} | {w.get('cb', '?')}"
                )
        pause()

    # ---------- 事件处理 ----------
    def _handle_event(self, inst) -> None:
        clear()
        print("=" * 60)
        print(f"  ⚡ 事件：{inst.title}")
        print("=" * 60)
        print(f"\n  {inst.description}\n")
        for i, choice in enumerate(inst.choices, 1):
            print(f"  {i}. {choice['text']}")
        print(f"  0. 返回")

        while True:
            try:
                choice = int(input("\n选择 > ").strip())
            except ValueError:
                print("请输入数字")
                continue
            if choice == 0:
                return
            if 1 <= choice <= len(inst.choices):
                c = inst.choices[choice - 1]
                self.api.action({"action": "resolve_event", "event_id": inst.event_id, "choice_id": c["id"]})
                print(f"\n  你选择了：{c['text']}")
                pause()
                return
            print("无效选择")

    # ---------- 征召 ----------
    def _action_raise(self) -> None:
        self.render("征召军团")
        snap = self.api.snapshot()
        owned = [c for c in snap.get("counties", []) if c.get("is_player")]
        if not owned:
            print("\n  你没有领地，无法征召")
            pause()
            return

        print("\n  选择征召省份：")
        for i, c in enumerate(owned, 1):
            print(f"    {i}. {c['name']} (征召:{c['levies']} 税收:{c['tax']:.1f})")
        print(f"    0. 返回")

        try:
            choice = int(input("\n选择 > ").strip())
        except ValueError:
            return
        if choice == 0:
            return
        if not (1 <= choice <= len(owned)):
            print("无效选择")
            pause()
            return

        county = owned[choice - 1]
        armies = [a for a in snap.get("armies", []) if a.get("is_player") and a.get("status") != "DISBANDED"]
        if armies:
            print(f"\n  已有军团，请先解散")
            pause()
            return

        res = self.api.action({"action": "raise_army", "county_id": county["id"]})
        msg = res.get("messages", [""])[-1] if res.get("messages") else ""
        print(f"\n  {msg or '征召完成'}")
        pause()

    # ---------- 移动 ----------
    def _action_move(self) -> None:
        self.render("移动军团")
        snap = self.api.snapshot()
        armies = [a for a in snap.get("armies", []) if a.get("is_player") and a.get("status") != "DISBANDED"]
        if not armies:
            print("\n  没有可指挥的军团")
            pause()
            return

        print("\n  选择军团：")
        for i, a in enumerate(armies, 1):
            print(f"    {i}. {a['name']} (兵力:{a['men']} 位置:{a.get('location_name', '?')})")
        print(f"    0. 返回")

        try:
            choice = int(input("\n选择 > ").strip())
        except ValueError:
            return
        if choice == 0:
            return
        if not (1 <= choice <= len(armies)):
            print("无效选择")
            pause()
            return

        army = armies[choice - 1]
        print("\n  选择目标省份ID：")
        for c in snap.get("counties", []):
            print(f"    {c['id']}. {c['name']}")
        print(f"    0. 返回")

        try:
            target = int(input("\n目标ID > ").strip())
        except ValueError:
            return
        if target == 0:
            return

        res = self.api.action({"action": "move_army", "army_id": army["id"], "county_id": target})
        msg = res.get("messages", [""])[-1] if res.get("messages") else ""
        print(f"\n  {msg or '移动完成'}")
        pause()

    # ---------- 解散 ----------
    def _action_disband(self) -> None:
        self.render("解散军团")
        snap = self.api.snapshot()
        armies = [a for a in snap.get("armies", []) if a.get("is_player") and a.get("status") != "DISBANDED"]
        if not armies:
            print("\n  没有可解散的军团")
            pause()
            return

        print("\n  选择解散的军团：")
        for i, a in enumerate(armies, 1):
            print(f"    {i}. {a['name']} (兵力:{a['men']})")
        print(f"    0. 返回")

        try:
            choice = int(input("\n选择 > ").strip())
        except ValueError:
            return
        if choice == 0:
            return
        if not (1 <= choice <= len(armies)):
            print("无效选择")
            pause()
            return

        army = armies[choice - 1]
        self.api.action({"action": "disband_army", "army_id": army["id"]})
        print(f"\n  已解散 {army['name']}")
        pause()

    # ---------- 宣战 ----------
    def _action_war(self) -> None:
        self.render("宣战")
        snap = self.api.snapshot()
        targets = [r for r in snap.get("rulers", []) if not r.get("is_player") and r.get("id") != self.api.player_id]
        # 过滤掉已同盟的
        filtered = []
        for t in targets:
            allied = False
            for tr in snap.get("treaties", []):
                if tr.get("kind") == "ALLIANCE" and self.api.player_id in (tr.get("a"), tr.get("b")) and t["id"] in (tr.get("a"), tr.get("b")):
                    allied = True
                    break
            if not allied:
                filtered.append(t)

        if not filtered:
            print("\n  没有可宣战的目标")
            pause()
            return

        print("\n  可选目标：")
        for i, t in enumerate(filtered, 1):
            print(f"    {i}. {t['name']} ({t.get('title', '无')})")
        print(f"    0. 返回")

        try:
            choice = int(input("\n选择 > ").strip())
        except ValueError:
            return
        if choice == 0:
            return
        if not (1 <= choice <= len(filtered)):
            print("无效选择")
            pause()
            return

        target = filtered[choice - 1]
        res = self.api.action({"action": "declare_war", "target_id": target["id"]})
        msg = res.get("messages", [""])[-1] if res.get("messages") else ""
        print(f"\n  {msg or '宣战完成'}")
        pause()

    # ---------- 改善关系 ----------
    def _action_improve(self) -> None:
        self.render("改善关系")
        snap = self.api.snapshot()
        chars = [c for c in snap.get("characters", []) if c.get("id") != self.api.player_id]
        print("\n  选择角色：")
        for i, c in enumerate(chars, 1):
            print(f"    {i}. {c['name']} ({c.get('title', '无')})")
        print(f"    0. 返回")

        try:
            choice = int(input("\n选择 > ").strip())
        except ValueError:
            return
        if choice == 0:
            return
        if not (1 <= choice <= len(chars)):
            print("无效选择")
            pause()
            return

        target = chars[choice - 1]
        res = self.api.action({"action": "improve_relations", "target_id": target["id"]})
        msg = res.get("messages", [""])[-1] if res.get("messages") else ""
        print(f"\n  {msg or '操作完成'}")
        pause()

    # ---------- 宴会 ----------
    def _action_feast(self) -> None:
        self.render("举办宴会")
        res = self.api.action({"action": "hold_feast"})
        msg = res.get("messages", [""])[-1] if res.get("messages") else ""
        print(f"\n  {msg or '宴会完成'}")
        pause()

    # ---------- 建筑 ----------
    def _menu_build(self) -> None:
        self.render("建造/升级建筑")
        snap = self.api.snapshot()
        owned = [c for c in snap.get("counties", []) if c.get("is_player")]
        if not owned:
            print("\n  你没有领地，无法建造")
            pause()
            return

        print("\n  选择省份：")
        for i, c in enumerate(owned, 1):
            print(f"    {i}. {c['name']} (发展:{c['development']} 已有建筑:{len(c.get('buildings', []))})")
        print(f"    0. 返回")

        try:
            choice = int(input("\n选择 > ").strip())
        except ValueError:
            return
        if choice == 0 or not (1 <= choice <= len(owned)):
            return
        county = owned[choice - 1]

        existing = {b["kind"]: b for b in county.get("buildings", [])}
        print(f"\n  {county['name']} 现有建筑：")
        if existing:
            for b in existing.values():
                print(f"    {b['name']} Lv.{b['level']}/{b['max_level']} 下一级费用:{b['upgrade_cost'] if b['can_upgrade'] else '已满级'}")
        else:
            print("    （无）")

        print("\n  选择要建造/升级的建筑：")
        kinds = list(BuildingKind)
        for i, k in enumerate(kinds, 1):
            cur = existing.get(k.name)
            if cur:
                cost = cur["upgrade_cost"] if cur["can_upgrade"] else "-"
                print(f"    {i}. {k.name_zh()} Lv.{cur['level']}/{k.max_level()} 费用:{cost} — {k.description()}")
            else:
                print(f"    {i}. {k.name_zh()} (新建 费用:{k.upgrade_cost(0)}) — {k.description()}")
        print(f"    0. 返回")

        try:
            kchoice = int(input("\n选择 > ").strip())
        except ValueError:
            return
        if kchoice == 0 or not (1 <= kchoice <= len(kinds)):
            return
        kind = kinds[kchoice - 1]

        res = self.api.action({"action": "upgrade_building", "county_id": county["id"], "building_kind": kind.name})
        msgs = [m for m in res.get("messages", []) if m]
        print(f"\n  {msgs[-1] if msgs else '操作完成'}")
        pause()

    # ---------- 法律 ----------
    def _menu_laws(self) -> None:
        while True:
            self.render("法律管理")
            snap = self.api.snapshot()
            laws = (snap.get("player") or {}).get("laws") or {}
            print(f"\n  当前继承法: {laws.get('succession') or '无'}")
            print(f"  当前王权:   {laws.get('crown_authority') if laws.get('crown_authority') is not None else '无'}")
            print(f"  当前性别法: {laws.get('gender_law') or '无'}")
            print("\n  1. 更改继承法")
            print("  2. 更改王权")
            print("  3. 更改性别法")
            print("  0. 返回")
            cmd = input("\n选择 > ").strip()
            if cmd == "0":
                break
            elif cmd == "1":
                self._pick_law("set_succession_law", "继承法", [(l.name, l.name_zh()) for l in SuccessionLaw])
            elif cmd == "2":
                self._pick_law("set_crown_authority", "王权", [(str(l.value), l.name_zh()) for l in CrownAuthority], is_level=True)
            elif cmd == "3":
                self._pick_law("set_gender_law", "性别法", [(l.name, l.name_zh()) for l in GenderLaw])

    def _pick_law(self, action: str, label: str, options: List[tuple], is_level: bool = False) -> None:
        print(f"\n  选择{label}：")
        for i, (ename, cname) in enumerate(options, 1):
            print(f"    {i}. {cname}")
        print(f"    0. 返回")
        try:
            choice = int(input("\n选择 > ").strip())
        except ValueError:
            return
        if choice == 0 or not (1 <= choice <= len(options)):
            return
        value = options[choice - 1][0]
        payload = {"action": action, "level": int(value)} if is_level else {"action": action, "law": value}
        res = self.api.action(payload)
        msgs = [m for m in res.get("messages", []) if m]
        print(f"\n  {msgs[-1] if msgs else '已更改'}")
        pause()

    # ---------- 求和 ----------
    def _action_peace(self) -> None:
        self.render("求和/白和")
        snap = self.api.snapshot()
        active = [w for w in snap.get("wars", []) if w.get("active") and w.get("involves_player")]
        if not active:
            print("\n  你没有进行中的战争")
            pause()
            return

        print("\n  你的战争：")
        for i, w in enumerate(active, 1):
            tag = "可白和" if w.get("can_white_peace") else "条件不足"
            print(f"    {i}. {w['name']} | 分数:{w.get('warscore', 0):.0f} 已持续:{w.get('months', 0)}月 [{tag}]")
        print(f"    0. 返回")

        try:
            choice = int(input("\n选择 > ").strip())
        except ValueError:
            return
        if choice == 0 or not (1 <= choice <= len(active)):
            return
        war = active[choice - 1]
        res = self.api.action({"action": "white_peace", "war_id": war["id"]})
        msgs = [m for m in res.get("messages", []) if m]
        print(f"\n  {msgs[-1] if msgs else '操作完成'}")
        pause()

    # ---------- 派系 ----------
    def _menu_factions(self) -> None:
        self.render("派系管理")
        snap = self.api.snapshot()
        factions = snap.get("factions", [])
        if not factions:
            print("\n  目前没有针对你的派系")
            pause()
            return

        print("\n  针对你的派系：")
        for i, f in enumerate(factions, 1):
            tag = "⚠已发最后通牒" if f.get("ultimatum") else ""
            print(f"    {i}. {f['kind']} 成员:{f['members']} 实力:{f['power']} 不满:{f['discontent']} {tag}")
        print(f"\n  安抚需花费 25 金")
        print(f"    0. 返回")

        try:
            choice = int(input("\n选择安抚对象 > ").strip())
        except ValueError:
            return
        if choice == 0 or not (1 <= choice <= len(factions)):
            return
        f = factions[choice - 1]
        res = self.api.action({"action": "appease_faction", "faction_id": f["id"]})
        msgs = [m for m in res.get("messages", []) if m]
        print(f"\n  {msgs[-1] if msgs else '操作完成'}")
        pause()

    # ---------- 授予头衔 ----------
    def _action_grant(self) -> None:
        self.render("授予头衔")
        w = self.api.sim.world
        player = w.character(self.api.player_id)
        if not player:
            return
        titles = [
            t for t in w.titles.values()
            if t.holder == self.api.player_id and t.id != player.primary_title
        ]
        if not titles:
            print("\n  没有可授予的头衔（主头衔不可授予）")
            pause()
            return

        print("\n  你的头衔：")
        for i, t in enumerate(titles, 1):
            print(f"    {i}. {t.name}")
        print(f"    0. 返回")

        try:
            choice = int(input("\n选择头衔 > ").strip())
        except ValueError:
            return
        if choice == 0 or not (1 <= choice <= len(titles)):
            return
        title = titles[choice - 1]

        snap = self.api.snapshot()
        chars = [c for c in snap.get("characters", []) if c.get("id") != self.api.player_id and c.get("is_alive")]
        print(f"\n  授予给谁（{title.name}）：")
        for i, c in enumerate(chars, 1):
            print(f"    {i}. {c['name']} ({c.get('title') or '无头衔'})")
        print(f"    0. 返回")

        try:
            cchoice = int(input("\n选择 > ").strip())
        except ValueError:
            return
        if cchoice == 0 or not (1 <= cchoice <= len(chars)):
            return
        target = chars[cchoice - 1]

        res = self.api.action({"action": "grant_title", "title_id": title.id, "target_id": target["id"]})
        msgs = [m for m in res.get("messages", []) if m]
        print(f"\n  {msgs[-1] if msgs else '已授予'}")
        pause()

    # ---------- 伪造宣称 ----------
    def _action_claim(self) -> None:
        self.render("伪造宣称")
        snap = self.api.snapshot()
        foreign = [c for c in snap.get("counties", []) if not c.get("is_player")]
        if not foreign:
            print("\n  没有可伪造宣称的省份")
            pause()
            return

        print("\n  选择省份（花费 50 金）：")
        for i, c in enumerate(foreign, 1):
            print(f"    {i}. {c['name']} (领主:{c.get('holder_name', '?')})")
        print(f"    0. 返回")

        try:
            choice = int(input("\n选择 > ").strip())
        except ValueError:
            return
        if choice == 0 or not (1 <= choice <= len(foreign)):
            return
        county = foreign[choice - 1]

        res = self.api.action({"action": "fabricate_claim", "county_id": county["id"]})
        msgs = [m for m in res.get("messages", []) if m]
        print(f"\n  {msgs[-1] if msgs else '操作完成'}")
        pause()

    # ---------- 发展领地 ----------
    def _action_develop(self) -> None:
        self.render("发展领地")
        snap = self.api.snapshot()
        owned = [c for c in snap.get("counties", []) if c.get("is_player")]
        if not owned:
            print("\n  你没有领地")
            pause()
            return

        print("\n  选择省份（每级花费 10 金）：")
        for i, c in enumerate(owned, 1):
            cap = c.get("dev_cap")
            cap_str = f"/{cap}" if cap else ""
            print(f"    {i}. {c['name']} (发展:{c['development']}{cap_str} 税收:{c['tax']:.1f})")
        print(f"    0. 返回")

        try:
            choice = int(input("\n选择 > ").strip())
        except ValueError:
            return
        if choice == 0 or not (1 <= choice <= len(owned)):
            return
        county = owned[choice - 1]

        res = self.api.action({"action": "develop_county", "county_id": county["id"]})
        msgs = [m for m in res.get("messages", []) if m]
        print(f"\n  {msgs[-1] if msgs else '操作完成'}")
        pause()

    # ---------- 招募精锐 ----------
    def _action_knights(self) -> None:
        self.render("招募精锐")
        print("\n  招募精锐部队（花费 25 金）：重骑兵+40 重步兵+80")
        print("  需要已有野战军。确认招募？(y/n)")
        if input("> ").strip().lower() not in ("y", "yes", "是"):
            return
        res = self.api.action({"action": "recruit_knights"})
        msgs = [m for m in res.get("messages", []) if m]
        print(f"\n  {msgs[-1] if msgs else '操作完成'}")
        pause()

    # ---------- 任命指挥官 ----------
    def _action_commander(self) -> None:
        self.render("任命指挥官")
        snap = self.api.snapshot()
        armies = [a for a in snap.get("armies", []) if a.get("is_player") and a.get("status") != "DISBANDED"]
        if not armies:
            print("\n  没有可指挥的军团")
            pause()
            return

        print("\n  选择军团：")
        for i, a in enumerate(armies, 1):
            print(f"    {i}. {a['name']} (兵力:{a['men']} 位置:{a.get('location_name', '?')})")
        print(f"    0. 返回")

        try:
            choice = int(input("\n选择 > ").strip())
        except ValueError:
            return
        if choice == 0 or not (1 <= choice <= len(armies)):
            return
        army = armies[choice - 1]

        chars = [c for c in snap.get("characters", []) if c.get("id") != self.api.player_id and c.get("age", 0) >= 16]
        print("\n  选择指挥官：")
        for i, c in enumerate(chars, 1):
            attrs = c.get("attrs", {})
            print(f"    {i}. {c['name']} (军略:{attrs.get('martial', 0)} 勇武:{attrs.get('prowess', 0)})")
        print(f"    0. 返回")

        try:
            cchoice = int(input("\n选择 > ").strip())
        except ValueError:
            return
        if cchoice == 0 or not (1 <= cchoice <= len(chars)):
            return
        target = chars[cchoice - 1]

        res = self.api.action({"action": "set_commander", "army_id": army["id"], "character_id": target["id"]})
        msgs = [m for m in res.get("messages", []) if m]
        print(f"\n  {msgs[-1] if msgs else '已任命'}")
        pause()

    # ---------- 查看宣称 ----------
    def _show_claims(self) -> None:
        self.render("我的宣称")
        snap = self.api.snapshot()
        claims = snap.get("player_claims", [])
        if not claims:
            print("\n  你没有任何宣称")
        else:
            print("\n  你的宣称：")
            for c in claims:
                target = c.get("title_name") or c.get("county_name") or "?"
                pressed = "已压制" if c.get("pressed") else "未压制"
                print(f"    {target} (强度:{c.get('strength', 0)} {pressed})")
        pause()

    # ---------- 查看条约 ----------
    def _show_treaties(self) -> None:
        self.render("条约/停战")
        snap = self.api.snapshot()
        treaties = snap.get("treaties", [])
        if not treaties:
            print("\n  没有生效的条约")
        else:
            print("\n  生效的条约：")
            for t in treaties:
                print(f"    {t.get('kind_zh', t.get('kind'))} ↔ {t.get('other_name', '?')} (至 {t.get('expires_year', '?')} 年)")
        pause()

    # ---------- 查看君主 ----------
    def _show_rulers(self) -> None:
        self.render("各国君主")
        snap = self.api.snapshot()
        print(f"\n  {'名字':<12} {'头衔':<12} {'金':<7} {'威望':<7} {'军略':<5} {'收入':<7} {'兵力':<7} 性格")
        print("  " + "-" * 75)
        for r in snap.get("rulers", []):
            marker = "*" if r.get("is_player") else " "
            print(
                f"  {marker}{r['name']:<11} {r.get('title', '无'):<12} "
                f"{r.get('gold', 0):<7.0f} {r.get('prestige', 0):<7.0f} "
                f"{r.get('martial', 0):<5} {r.get('income', 0):<7.1f} {r.get('men', 0):<7} "
                f"{r.get('persona', '')}"
            )
        print(f"\n  * 表示当前玩家")
        pause()

    # ---------- 新游戏 ----------
    def _action_new_game(self) -> None:
        self.render("新游戏")
        print("\n  开始新游戏将丢失当前进度（已有存档不受影响）。确认？(y/n)")
        if input("> ").strip().lower() not in ("y", "yes", "是"):
            return
        self.api.action({"action": "new_game"})
        print("\n  新局开始")
        pause()

    # ---------- 切换玩家 ----------
    def _action_switch_player(self) -> None:
        self.render("切换玩家")
        snap = self.api.snapshot()
        playable = snap.get("playable", [])
        print("\n  可选统治者：")
        for i, p in enumerate(playable, 1):
            marker = "*" if p["id"] == self.api.player_id else " "
            print(f"    {marker}{i}. {p['name']} ({p.get('title') or '无'})")
        print(f"    0. 返回")

        try:
            choice = int(input("\n选择 > ").strip())
        except ValueError:
            return
        if choice == 0 or not (1 <= choice <= len(playable)):
            return
        target = playable[choice - 1]
        self.api.action({"action": "set_player", "character_id": target["id"]})
        print(f"\n  已切换为 {target['name']}")
        pause()

    # ---------- 推进时间 ----------
    def _action_advance(self) -> None:
        self.render("推进时间")
        print("\n  推进天数（建议 1/7/30/90/365）：")
        print("    1. 1 天")
        print("    2. 1 周 (7天)")
        print("    3. 1 月 (30天)")
        print("    4. 1 季 (90天)")
        print("    5. 1 年 (365天)")
        print("    6. 自定义")
        print("    0. 返回")

        try:
            choice = int(input("\n选择 > ").strip())
        except ValueError:
            return

        days_map = {1: 1, 2: 7, 3: 30, 4: 90, 5: 365}
        if choice == 0:
            return
        elif choice in days_map:
            days = days_map[choice]
        elif choice == 6:
            try:
                days = int(input("天数 > ").strip())
            except ValueError:
                return
            days = max(1, min(365, days))
        else:
            print("无效选择")
            pause()
            return

        self.api.action({"action": "advance", "days": days})
        snap = self.api.snapshot()
        print(f"\n  时间推进 {days} 天 → {snap['date']}")
        pause()

    # ---------- 内阁 ----------
    def _menu_council(self) -> None:
        while True:
            self.render("内阁管理")
            snap = self.api.snapshot()
            council = snap.get("player_council")
            if not council:
                print("\n  暂无内阁")
                pause()
                return

            print("\n  内阁成员：")
            for m in council.get("members", []):
                print(f"  {m['position_zh']}: {m['holder_name']} | 任务: {m['task_zh']}")

            print("\n  1. 任命官员")
            print("  2. 分配任务")
            print("  0. 返回")
            cmd = input("\n选择 > ").strip()

            if cmd == "0":
                break
            elif cmd == "1":
                self._menu_appoint()
            elif cmd == "2":
                self._menu_task()

    def _menu_appoint(self) -> None:
        self.render("任命官员")
        snap = self.api.snapshot()
        positions = [
            ("CHANCELLOR", "首相"),
            ("MARSHAL", "元帅"),
            ("STEWARD", "总管"),
            ("SPYMASTER", "间谍总管"),
            ("COURT_CHAPLAIN", "宫廷神甫"),
        ]
        print("\n  选择职位：")
        for i, (ename, cname) in enumerate(positions, 1):
            print(f"    {i}. {cname}")
        print(f"    0. 返回")

        try:
            choice = int(input("\n选择 > ").strip())
        except ValueError:
            return
        if choice == 0:
            return
        if not (1 <= choice <= len(positions)):
            print("无效选择")
            pause()
            return

        pos_name = positions[choice - 1][0]
        chars = [c for c in snap.get("characters", []) if c.get("id") != self.api.player_id and c.get("age", 0) >= 16]
        if not chars:
            print("\n  没有可选人选")
            pause()
            return
        print("\n  选择人选：")
        for i, c in enumerate(chars, 1):
            print(f"    {i}. {c['name']} ({c.get('title') or '无头衔'}, {c.get('age', '?')}岁)")
        print(f"    0. 返回")

        try:
            cid = int(input("\n选择 > ").strip())
        except ValueError:
            return
        if cid == 0:
            return
        if not (1 <= cid <= len(chars)):
            print("无效选择")
            pause()
            return

        self.api.action({"action": "appoint_council", "position": pos_name, "character_id": chars[cid - 1]["id"]})
        print(f"\n  已任命")
        pause()

    def _menu_task(self) -> None:
        self.render("分配任务")
        positions = [
            ("CHANCELLOR", "首相"),
            ("MARSHAL", "元帅"),
            ("STEWARD", "总管"),
            ("SPYMASTER", "间谍总管"),
            ("COURT_CHAPLAIN", "宫廷神甫"),
        ]
        tasks = [
            ("DOMESTIC_RELATIONS", "内政外交"),
            ("FABRICATE_CLAIM", "伪造宣称"),
            ("TRAIN_COMMANDERS", "训练将领"),
            ("INCREASE_CONTROL", "强化控制"),
            ("COLLECT_TAXES", "催收税赋"),
            ("DEVELOP_COUNTY", "发展领地"),
            ("DISRUPT_SCHEMES", "破坏阴谋"),
            ("SUPPORT_MURDER", "协助密谋"),
            ("CONVERT_FAITH", "传播信仰"),
            ("RECRUIT_KNIGHTS", "招募骑士"),
            ("IMPROVE_DIPLOMACY", "改善外交"),
            ("SPREAD_CULTURE", "传播文化"),
            ("ESTABLISH_TRADE", "建立商路"),
            ("MAINTAIN_BUILDINGS", "维护建筑"),
            ("TRAIN_TROOPS", "训练部队"),
            ("GATHER_INTEL", "收集情报"),
            ("PROMOTE_CULTURE", "推广文化"),
        ]
        print("\n  选择职位：")
        for i, (ename, cname) in enumerate(positions, 1):
            print(f"    {i}. {cname}")
        print(f"    0. 返回")

        try:
            choice = int(input("\n选择 > ").strip())
        except ValueError:
            return
        if choice == 0:
            return
        if not (1 <= choice <= len(positions)):
            print("无效选择")
            pause()
            return

        pos_name = positions[choice - 1][0]
        print("\n  选择任务：")
        for i, (ename, tname) in enumerate(tasks, 1):
            print(f"    {i}. {tname}")
        print(f"    0. 返回")

        try:
            tchoice = int(input("\n选择 > ").strip())
        except ValueError:
            return
        if tchoice == 0:
            return
        if not (1 <= tchoice <= len(tasks)):
            print("无效选择")
            pause()
            return

        task_name = tasks[tchoice - 1][0]
        self.api.action({"action": "assign_council_task", "position": pos_name, "task": task_name})
        print(f"\n  任务已分配")
        pause()

    # ---------- 阴谋 ----------
    def _menu_schemes(self) -> None:
        while True:
            self.render("阴谋活动")
            snap = self.api.snapshot()
            schemes = snap.get("player_schemes", [])
            if schemes:
                print("\n  进行中阴谋：")
                for s in schemes:
                    print(f"    {s['kind_zh']} → {s['target_name']} 进度:{s['progress']:.0f}%")
            else:
                print("\n  没有进行中的阴谋")

            print("\n  1. 发起阴谋")
            print("  0. 返回")
            cmd = input("\n选择 > ").strip()

            if cmd == "0":
                break
            elif cmd == "1":
                self._action_start_scheme()

    def _action_start_scheme(self) -> None:
        self.render("发起阴谋")
        snap = self.api.snapshot()
        chars = [c for c in snap.get("characters", []) if c.get("id") != self.api.player_id and c.get("is_alive")]
        print("\n  选择目标：")
        for i, c in enumerate(chars, 1):
            print(f"    {i}. {c['name']}")
        print(f"    0. 返回")

        try:
            choice = int(input("\n选择 > ").strip())
        except ValueError:
            return
        if choice == 0:
            return
        if not (1 <= choice <= len(chars)):
            print("无效选择")
            pause()
            return

        target = chars[choice - 1]
        kinds = [
            ("MURDER", "谋杀"),
            ("ABDUCT", "绑架"),
            ("FABRICATE_HOOK", "伪造把柄"),
            ("SWAY", "拉拢"),
            ("SEDUCE", "引诱"),
            ("CLAIM_FABRICATION", "伪造宣称"),
        ]
        print("\n  选择阴谋类型：")
        for i, (ename, cname) in enumerate(kinds, 1):
            print(f"    {i}. {cname}")
        print(f"    0. 返回")

        try:
            kchoice = int(input("\n选择 > ").strip())
        except ValueError:
            return
        if kchoice == 0:
            return
        if not (1 <= kchoice <= len(kinds)):
            print("无效选择")
            pause()
            return

        kind_name = kinds[kchoice - 1][0]
        res = self.api.action({"action": "start_scheme", "scheme_kind": kind_name, "target_id": target["id"]})
        msg = res.get("messages", [""])[-1] if res.get("messages") else ""
        print(f"\n  {msg or '阴谋已发起'}")
        pause()

    # ---------- 外交 ----------
    def _menu_diplomacy(self) -> None:
        while True:
            self.render("外交行动")
            snap = self.api.snapshot()
            chars = [c for c in snap.get("characters", []) if c.get("id") != self.api.player_id and c.get("is_alive")]
            print("\n  可选角色：")
            for i, c in enumerate(chars, 1):
                print(f"    {i}. {c['name']} ({c.get('title', '无')})")
            print(f"    0. 返回主菜单")

            try:
                choice = int(input("\n选择 > ").strip())
            except ValueError:
                continue
            if choice == 0:
                break
            if not (1 <= choice <= len(chars)):
                print("无效选择")
                pause()
                continue

            target = chars[choice - 1]
            print(f"\n  对 {target['name']} 的外交行动：")
            print("    1. 结盟")
            print("    2. 签订互不侵犯")
            print("    3. 成为附庸")
            print("    4. 贸易协定")
            print("    5. 情报共享")
            print("    6. 联姻")
            print("    7. 赠送礼物")
            print("    8. 设为宿敌")
            print("    9. 邀请入宫廷 (30金)")
            print("   10. 为其举办宴会 (40金)")
            print("   11. 决斗")
            print("    0. 返回")

            act = input("\n选择 > ").strip()
            if act == "0":
                continue

            tid = target["id"]
            action_map = {
                "1": ("form_alliance", {}),
                "2": ("form_non_aggression", {}),
                "3": ("form_vassalage", {}),
                "4": ("form_trade_agreement", {}),
                "5": ("form_intelligence_sharing", {}),
                "9": ("invite_to_court", {}),
                "10": ("host_feast_for", {}),
                "11": ("duel", {}),
            }
            if act in action_map:
                aname, _ = action_map[act]
                res = self.api.action({"action": aname, "target_id": tid})
                msg = res.get("messages", [""])[-1] if res.get("messages") else ""
                print(f"\n  {msg or '操作完成'}")
                pause()
            elif act == "6":
                self.api.action({"action": "arrange_marriage", "target_id": tid})
                print(f"\n  联姻完成")
                pause()
            elif act == "7":
                try:
                    amount = float(input("金额 (1-500) > ").strip())
                except ValueError:
                    amount = 50
                res = self.api.action({"action": "send_gift", "target_id": tid, "amount": amount})
                msg = res.get("messages", [""])[-1] if res.get("messages") else ""
                print(f"\n  {msg or '赠送完成'}")
                pause()
            elif act == "8":
                res = self.api.action({"action": "set_rival", "target_id": tid})
                msg = res.get("messages", [""])[-1] if res.get("messages") else ""
                print(f"\n  {msg or '操作完成'}")
                pause()

    # ---------- 存档 / 读档 ----------
    def _menu_load(self) -> None:
        self.render("读档")
        saves = self.api.snapshot().get("saves", [])
        if not saves:
            print("\n  没有存档")
            pause()
            return

        print("\n  可用存档：")
        for i, s in enumerate(saves, 1):
            date_str = s.get("date", "未知")
            print(f"    {i}. {s['name']} ({date_str})")
        print(f"    0. 返回")

        try:
            choice = int(input("\n选择 > ").strip())
        except ValueError:
            return
        if choice == 0:
            return
        if not (1 <= choice <= len(saves)):
            print("无效选择")
            pause()
            return

        name = saves[choice - 1]["name"]
        res = self.api.action({"action": "load", "name": name})
        msg = res.get("messages", [""])[-1] if res.get("messages") else ""
        print(f"\n  {msg or '读档完成'}")
        pause()

    # ---------- 作弊 ----------
    def _toggle_cheat(self) -> None:
        self.api.action({"action": "toggle_cheat"})
        pause()


def main() -> None:
    tui = GameTUI()
    tui.run()


if __name__ == "__main__":
    main()
