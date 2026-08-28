"""GameTUI 的只读视图（状态/列表/详情/地图渲染）部分。

以 Mixin 形式并入 GameTUI，见 ck_engine/game/tui.py。
"""

from __future__ import annotations

import os


def clear() -> None:
    os.system("cls" if os.name == "nt" else "clear")


def pause(msg: str = "按回车继续...") -> None:
    input(msg)


class TUIViewMixin:
    """只读展示方法，依赖宿主提供 api/_name/render 等属性与方法。"""

    # ---------- 渲染 ----------
    _SEASONS = {"SPRING": "春", "SUMMER": "夏", "AUTUMN": "秋", "WINTER": "冬"}
    _TERRAINS = {
        "PLAINS": "平原", "HILLS": "丘陵", "MOUNTAINS": "山地", "FOREST": "森林",
        "DESERT": "沙漠", "WETLAND": "湿地", "FARMLAND": "农田", "COASTAL": "沿海",
    }

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

    # ---------- 角色详情 ----------
    def _show_char_detail(self) -> None:
        self.render("角色详情")
        snap = self.api.snapshot()
        chars = snap.get("characters", [])
        print("\n  选择角色：")
        for i, c in enumerate(chars, 1):
            tag = "★" if c.get("id") == self.api.player_id else " "
            rel = ""
            if c.get("relation_allied"):
                rel += "[盟]"
            if c.get("relation_rival"):
                rel += "[敌]"
            if c.get("relation_at_war"):
                rel += "[战]"
            if c.get("relation_marriage"):
                rel += "[姻]"
            print(f"    {tag}{i}. {c['name']} {c.get('age', '?')}岁 {c.get('title') or '无'} {rel}")
        print(f"     0. 返回")

        try:
            choice = int(input("\n选择 > ").strip())
        except ValueError:
            return
        if choice == 0 or not (1 <= choice <= len(chars)):
            return
        c = chars[choice - 1]
        self._render_char_detail(c)

    def _render_char_detail(self, c: dict) -> None:
        self.render(f"角色详情 — {c['name']}")
        attrs = c.get("attrs") or {}
        print(f"\n  姓名: {c['name']}  性别: {c.get('gender', '?')}  年龄: {c.get('age', '?')}")
        print(f"  家族: {c.get('dynasty_name') or '无'}  头衔: {c.get('title') or '无'}")
        print(f"  金币: {c.get('gold', 0):.0f}  威望: {c.get('prestige', 0):.0f}  等级: {c.get('level', 1)}")
        if attrs:
            print(
                f"  外交:{attrs.get('diplomacy', 0)} 军略:{attrs.get('martial', 0)} "
                f"管理:{attrs.get('stewardship', 0)} 阴谋:{attrs.get('intrigue', 0)} "
                f"学识:{attrs.get('learning', 0)} 勇武:{attrs.get('prowess', 0)}"
            )
        # 好感
        print(f"\n  对你的好感: {c.get('opinion_of_player', 0):+d}  你对其好感: {c.get('player_opinion', 0):+d}")
        # 关系标签
        rels = []
        if c.get("relation_allied"):
            rels.append("同盟")
        if c.get("relation_rival"):
            rels.append("宿敌")
        if c.get("relation_at_war"):
            rels.append("交战中")
        if c.get("relation_marriage"):
            rels.append("联姻")
        if c.get("relation_vassalage"):
            rels.append("附庸")
        if c.get("relation_trade_agreement"):
            rels.append("贸易")
        if c.get("relation_intelligence_sharing"):
            rels.append("情报共享")
        print(f"  关系: {'  '.join(rels) if rels else '无特殊关系'}")
        # 婚姻
        spouse_ids = c.get("spouse_ids") or []
        if spouse_ids:
            spouse_names = [self._name(sid) for sid in spouse_ids]
            print(f"  配偶: {'、'.join(spouse_names)}")
        else:
            print(f"  配偶: 无")
        # 持有头衔
        w = self.api.sim.world
        held = c.get("held_title_ids") or []
        if held:
            names = []
            for tid in held:
                t = w.title(tid)
                names.append(t.name if t else f"#{tid}")
            print(f"  持有头衔: {'、'.join(names)}")
        # 特质
        char = w.character(c["id"])
        if char and char.traits:
            from ck_engine.core.traits import builtin_traits
            tmap = {t.id: t.name for t in builtin_traits()}
            tnames = [tmap.get(tid, f"#{tid}") for tid in char.traits]
            print(f"  特质: {'、'.join(tnames)}")
        print()
        pause()

    # ---------- 省份详情 ----------
    def _show_county_detail(self) -> None:
        self.render("省份详情")
        snap = self.api.snapshot()
        print("\n  选择省份：")
        for i, c in enumerate(snap.get("counties", []), 1):
            marker = "*" if c.get("is_player") else " "
            siege_tag = " [围城中]" if c.get("siege") else ""
            print(f"    {marker}{i}. {c['name']} ({c.get('holder_name', '?')}){siege_tag}")
        print(f"     0. 返回")

        try:
            choice = int(input("\n选择 > ").strip())
        except ValueError:
            return
        if choice == 0 or not (1 <= choice <= len(snap.get("counties", []))):
            return
        c = snap["counties"][choice - 1]
        self._render_county_detail(c)

    def _render_county_detail(self, c: dict) -> None:
        self.render(f"省份详情 — {c['name']}")
        terrain_zh = self._TERRAINS.get(c.get("terrain", ""), c.get("terrain", "?"))
        print(f"\n  省份: {c['name']}  地形: {terrain_zh}")
        print(f"  领主: {c.get('holder_name', '无主')}  {'★ 己方领地' if c.get('is_player') else ''}")
        print(f"  发展: {c.get('development', 0)}/{c.get('dev_cap', '?')}  控制: {c.get('control', 0):.1f}")
        print(f"  税收: {c.get('tax', 0):.1f}  征召: {c.get('levies', 0)}  堡垒: {c.get('fort', 0)}")
        # 建筑
        buildings = c.get("buildings") or []
        if buildings:
            print(f"\n  建筑：")
            for b in buildings:
                tag = f"Lv.{b['level']}/{b['max_level']}" if b.get("max_level") else ""
                print(f"    {b['name']} {tag} — {b.get('description', '')}")
        else:
            print(f"\n  建筑：无")
        # 邻接
        neighbors = c.get("neighbors") or []
        if neighbors:
            w = self.api.sim.world
            names = []
            for nid in neighbors:
                nc = w.map.get(nid)
                names.append(nc.name if nc else f"#{nid}")
            print(f"  邻接: {'、'.join(names)}")
        # 驻军
        armies = c.get("armies") or []
        if armies:
            w = self.api.sim.world
            print(f"\n  驻扎军团：")
            for aid in armies:
                a = self.api.sim.wars.army(aid)
                if a:
                    owner = w.character(a.owner)
                    print(f"    {a.name} — {owner.name if owner else '?'} ({a.total_men()}人)")
        # 围城
        siege = c.get("siege")
        if siege:
            print(f"\n  ⚔ 围城进行中！")
            print(f"    进度: {siege.get('progress', 0)}/{siege.get('required', 0)}")
            atk = self.api.sim.world.character(siege.get("attacker", 0))
            print(f"    攻方: {atk.name if atk else '?'}")
        print()
        pause()

    # ---------- 围城总览 ----------
    def _show_sieges(self) -> None:
        self.render("围城总览")
        snap = self.api.snapshot()
        sieges = [c for c in snap.get("counties", []) if c.get("siege")]
        if not sieges:
            print("\n  当前无围城")
        else:
            w = self.api.sim.world
            for c in sieges:
                s = c["siege"]
                atk = w.character(s.get("attacker", 0))
                holder = c.get("holder_name", "?")
                bar_len = 20
                filled = int(bar_len * s.get("progress", 0) / max(1, s.get("required", 1)))
                bar = "█" * filled + "░" * (bar_len - filled)
                involved = "★" if c.get("is_player") or (atk and atk.id == self.api.player_id) else " "
                print(
                    f"\n  {involved}{c['name']} (守方:{holder})"
                )
                print(f"    攻方: {atk.name if atk else '?'}")
                print(f"    [{bar}] {s.get('progress', 0)}/{s.get('required', 0)}")
        pause()

    # ---------- 事件日志 ----------
    def _show_log(self) -> None:
        self.render("事件日志")
        snap = self.api.snapshot()
        log = snap.get("log", [])
        if not log:
            print("\n  暂无日志")
        else:
            print(f"\n  最近 {len(log)} 条事件：\n")
            for line in log:
                print(f"  {line}")
        pause()

    # ---------- 文字地图 ----------
    def _show_map(self) -> None:
        self.render("文字地图")
        snap = self.api.snapshot()
        counties = snap.get("counties", [])
        if not counties:
            print("\n  无地图数据")
            pause()
            return

        # 用 cx/cy 映射到字符网格
        import math
        cols, rows = 56, 24
        grid = [[" " for _ in range(cols)] for _ in range(rows)]
        # 计算 cx/cy 范围
        xs = [c.get("cx", 0) for c in counties]
        ys = [c.get("cy", 0) for c in counties]
        min_x, max_x = min(xs), max(xs)
        min_y, max_y = min(ys), max(ys)
        range_x = max(1, max_x - min_x)
        range_y = max(1, max_y - min_y)

        def to_grid(cx, cy):
            gx = int((cx - min_x) / range_x * (cols - 8) + 3)
            gy = int((cy - min_y) / range_y * (rows - 4) + 1)
            return gx, gy

        # 画邻接线
        by_id = {c["id"]: c for c in counties}
        drawn = set()
        for c in counties:
            gx1, gy1 = to_grid(c.get("cx", 0), c.get("cy", 0))
            for nid in c.get("neighbors", []):
                nc = by_id.get(nid)
                if not nc:
                    continue
                key = (min(c["id"], nid), max(c["id"], nid))
                if key in drawn:
                    continue
                drawn.add(key)
                gx2, gy2 = to_grid(nc.get("cx", 0), nc.get("cy", 0))
                steps = max(abs(gx2 - gx1), abs(gy2 - gy1), 1)
                for s in range(steps + 1):
                    px = int(gx1 + (gx2 - gx1) * s / steps)
                    py = int(gy1 + (gy2 - gy1) * s / steps)
                    if 0 <= py < rows and 0 <= px < cols and grid[py][px] == " ":
                        grid[py][px] = "·"

        # 画省份
        for c in counties:
            gx, gy = to_grid(c.get("cx", 0), c.get("cy", 0))
            if 0 <= gy < rows and 0 <= gx < cols:
                if c.get("is_player"):
                    grid[gy][gx] = "★"
                elif c.get("siege"):
                    grid[gy][gx] = "⚔"
                else:
                    holder = c.get("holder_name", "")
                    grid[gy][gx] = holder[0] if holder else "?"

        # 输出地图
        print()
        border = "═" * cols
        print(f"  ╔{border}╗")
        for row in grid:
            print(f"  ║{''.join(row)}║")
        print(f"  ╚{border}╝")

        # 图例
        print(f"\n  ★ 己方  ⚔ 围城中  · 邻接  [其他为首字母]")
        # 列出省份对应
        print(f"\n  省份图例：")
        for c in counties:
            holder = c.get("holder_name", "?")
            ch = "★" if c.get("is_player") else ("⚔" if c.get("siege") else (holder[0] if holder else "?"))
            print(f"    {ch} {c['name']} ({holder})")
        pause()
