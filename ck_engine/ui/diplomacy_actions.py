"""GameAPI 的外交类操作（结盟/条约/联姻/赠礼/宿敌/决斗等）。

以 Mixin 形式并入 GameAPI，见 ck_engine/ui/api.py。
"""

from __future__ import annotations

import random
from typing import Any

from ck_engine.core import NONE_ID
from ck_engine.politics.diplomacy import Treaty, TreatyKind


class DiplomacyActionsMixin:
    """外交操作方法，依赖宿主提供 sim/player_id/notify 等属性。"""

    def _form_alliance(self, target_id: int) -> None:
        if target_id == self.player_id:
            raise ValueError("不能与自己结盟")
        dip = self.sim.diplomacy
        if dip.are_allied(self.player_id, target_id):
            raise ValueError("已是同盟")
        if dip.flags(self.player_id, target_id).at_war:
            raise ValueError("交战中无法结盟")
        player = self.sim.world.character(self.player_id)
        target = self.sim.world.character(target_id)
        if not player or not target:
            raise ValueError("目标无效")
        # 需要好感达标或已有联姻
        op = self.sim.world.opinion(target_id, self.player_id)
        has_marriage = dip.flags(self.player_id, target_id).marriage_pact
        if op < 30 and not has_marriage:
            raise ValueError(f"好感不足（需 30，当前 {op}）或需先联姻")
        dip.form_alliance(self.player_id, target_id, self.sim.world.date)
        self.sim.world.push_log(f"{player.name} 与 {target.name} 缔结同盟")
        self.notify(f"已与 {target.name} 缔结同盟")

    def _form_non_aggression(self, target_id: int) -> None:
        if target_id == self.player_id:
            raise ValueError("无效目标")
        dip = self.sim.diplomacy
        if dip.flags(self.player_id, target_id).non_aggression:
            raise ValueError("已有互不侵犯条约")
        if dip.flags(self.player_id, target_id).at_war:
            raise ValueError("交战中无法签订")
        target = self.sim.world.character(target_id)
        if not target:
            raise ValueError("目标无效")
        f = dip.flags_mut(self.player_id, target_id)
        f.non_aggression = True
        dip.treaties.append(self._make_treaty(target_id, TreatyKind.NON_AGGRESSION, 20))
        self.sim.world.push_log(f"{self._name(self.player_id)} 与 {target.name} 签订互不侵犯条约")
        self.notify(f"已与 {target.name} 签订互不侵犯条约")

    def _form_vassalage(self, target_id: int) -> None:
        if target_id == self.player_id:
            raise ValueError("无效目标")
        dip = self.sim.diplomacy
        if dip.flags(self.player_id, target_id).vassalage:
            raise ValueError("已是附庸关系")
        target = self.sim.world.character(target_id)
        if not target:
            raise ValueError("目标无效")
        dip.form_vassalage(self.player_id, target_id, self.sim.world.date)
        self.sim.world.push_log(f"{self._name(self.player_id)} 成为 {target.name} 的附庸")
        self.notify(f"已成为 {target.name} 的附庸")

    def _form_trade_agreement(self, target_id: int) -> None:
        if target_id == self.player_id:
            raise ValueError("无效目标")
        dip = self.sim.diplomacy
        if dip.flags(self.player_id, target_id).trade_agreement:
            raise ValueError("已有贸易协定")
        target = self.sim.world.character(target_id)
        if not target:
            raise ValueError("目标无效")
        dip.form_trade_agreement(self.player_id, target_id, self.sim.world.date)
        self.sim.world.push_log(f"{self._name(self.player_id)} 与 {target.name} 签订贸易协定")
        self.notify(f"已与 {target.name} 签订贸易协定")

    def _form_intelligence_sharing(self, target_id: int) -> None:
        if target_id == self.player_id:
            raise ValueError("无效目标")
        dip = self.sim.diplomacy
        if dip.flags(self.player_id, target_id).intelligence_sharing:
            raise ValueError("已有情报共享")
        target = self.sim.world.character(target_id)
        if not target:
            raise ValueError("目标无效")
        dip.form_intelligence_sharing(self.player_id, target_id, self.sim.world.date)
        self.sim.world.push_log(f"{self._name(self.player_id)} 与 {target.name} 建立情报共享")
        self.notify(f"已与 {target.name} 建立情报共享")

    def _make_treaty(self, target_id: int, kind: TreatyKind, years: int) -> Treaty:
        return Treaty(
            a=self.player_id, b=target_id, kind=kind,
            start=self.sim.world.date, expires_year=self.sim.world.date.year + years,
        )

    def _arrange_marriage(self, target_id: int) -> None:
        w = self.sim.world
        player = w.character(self.player_id)
        target = w.character(target_id)
        if not player or not target:
            raise ValueError("目标无效")
        if not target.is_alive():
            raise ValueError("目标已故")
        if player.gender == target.gender:
            raise ValueError("同性无法成婚")
        if player.is_married():
            raise ValueError("玩家已有配偶")
        if target.is_married():
            raise ValueError("目标已有配偶")
        if not player.is_adult(w.date) or not target.is_adult(w.date):
            raise ValueError("未成年无法成婚")
        op = w.opinion(target_id, self.player_id)
        if op < 0:
            raise ValueError(f"对方好感不足（当前 {op}）")
        ok = w.marry(self.player_id, target_id)
        if not ok:
            raise ValueError("婚姻失败")
        # 联姻自动设 marriage_pact
        dip = self.sim.diplomacy
        dip.flags_mut(self.player_id, target_id).marriage_pact = True
        dip.treaties.append(self._make_treaty(target_id, TreatyKind.MARRIAGE_PACT, 50))
        self.notify(f"已与 {target.name} 成婚（联姻协定生效）")

    def _send_gift(self, target_id: int, amount: float) -> None:
        if target_id == self.player_id:
            raise ValueError("不能给自己送礼")
        amount = max(1.0, min(500.0, amount))
        player = self.sim.world.character(self.player_id)
        if not player or player.gold < amount:
            raise ValueError(f"金币不足（需要 {amount:.0f}）")
        target = self.sim.world.character(target_id)
        if not target:
            raise ValueError("目标无效")
        player.add_gold(-amount)
        target.add_gold(amount)
        gain = self.sim.diplomacy.gift_opinion_gain(amount)
        self.sim.world.modify_opinion(target_id, self.player_id, gain)
        self.sim.world.modify_opinion(self.player_id, target_id, gain // 3)
        self.notify(f"向 {target.name} 赠送 {amount:.0f} 金（好感 +{gain}）")

    def _set_rival(self, target_id: int) -> None:
        if target_id == self.player_id:
            raise ValueError("无效目标")
        dip = self.sim.diplomacy
        if dip.flags(self.player_id, target_id).rival:
            raise ValueError("已是宿敌")
        target = self.sim.world.character(target_id)
        if not target:
            raise ValueError("目标无效")
        dip.set_rival(self.player_id, target_id)
        dip.set_rival(target_id, self.player_id)
        self.sim.world.modify_opinion(self.player_id, target_id, -30)
        self.sim.world.modify_opinion(target_id, self.player_id, -30)
        self.sim.world.push_log(f"{self._name(self.player_id)} 视 {target.name} 为宿敌")
        self.notify(f"已将 {target.name} 设为宿敌")

    def _invite_to_court(self, target_id: int) -> None:
        if target_id == self.player_id:
            raise ValueError("不能邀请自己")
        target = self.sim.world.character(target_id)
        if not target or not target.is_alive():
            raise ValueError("目标无效")
        if not target.is_adult(self.sim.world.date):
            raise ValueError("目标未成年")
        player = self.sim.world.character(self.player_id)
        if not player:
            raise ValueError("玩家无效")
        if player.gold < 30:
            raise ValueError("金币不足（需要 30）")
        player.add_gold(-30)
        self.sim.world.modify_opinion(target_id, self.player_id, 15)
        self.sim.world.push_log(f"{player.name} 邀请 {target.name} 访问宫廷")
        self.notify(f"已邀请 {target.name} 访问宫廷（花费 30 金）")

    def _host_feast_for(self, target_id: int) -> None:
        if target_id == self.player_id:
            raise ValueError("不能宴请自己")
        target = self.sim.world.character(target_id)
        if not target or not target.is_alive():
            raise ValueError("目标无效")
        player = self.sim.world.character(self.player_id)
        if not player:
            raise ValueError("玩家无效")
        if player.gold < 40:
            raise ValueError("金币不足（需要 40）")
        player.add_gold(-40)
        gain = 20
        self.sim.world.modify_opinion(target_id, self.player_id, gain)
        player.add_prestige(10)
        player.add_stress(-8)
        self.sim.world.push_log(f"{player.name} 为 {target.name} 举办宴会")
        self.notify(f"已为 {target.name} 举办宴会（好感 +{gain}，威望 +10）")

    def _duel(self, target_id: int) -> None:
        if target_id == self.player_id:
            raise ValueError("不能与自己决斗")
        target = self.sim.world.character(target_id)
        if not target or not target.is_alive():
            raise ValueError("目标无效")
        if not target.is_adult(self.sim.world.date):
            raise ValueError("目标未成年")
        player = self.sim.world.character(self.player_id)
        if not player:
            raise ValueError("玩家无效")
        attrs = self.sim.world.effective_attrs(self.player_id)
        target_attrs = self.sim.world.effective_attrs(target_id)
        player_score = (attrs.prowess if attrs else 0) + random.uniform(0, 20)
        target_score = (target_attrs.prowess if target_attrs else 0) + random.uniform(0, 20)
        if player_score > target_score:
            self.sim.world.modify_opinion(target_id, self.player_id, -10)
            self.sim.world.modify_opinion(self.player_id, target_id, -15)
            player.add_prestige(15)
            target.add_stress(10)
            self.sim.world.push_log(f"{player.name} 在决斗中击败了 {target.name}")
            self.notify(f"决斗胜利！威望 +15")
        elif player_score < target_score:
            self.sim.world.modify_opinion(target_id, self.player_id, 5)
            self.sim.world.modify_opinion(self.player_id, target_id, -20)
            player.add_stress(15)
            player.health -= 0.1
            self.sim.world.push_log(f"{player.name} 在决斗中被 {target.name} 击败")
            self.notify(f"决斗失败，受伤了")
        else:
            self.sim.world.push_log(f"{player.name} 与 {target.name} 的决斗不分胜负")
            self.notify("决斗平局")

    # ---------- 子嗣联姻 ----------

    def _player_children(self) -> List:
        """返回玩家的在世子女（按年龄从大到小）。"""
        player = self.sim.world.character(self.player_id)
        if not player:
            return []
        result = []
        for cid in player.children:
            c = self.sim.world.character(cid)
            if c and c.is_alive() and c.id != self.player_id:
                result.append(c)
        result.sort(key=lambda c: c.age_at(self.sim.world.date), reverse=True)
        return result

    def _arrange_child_marriage(self, child_id: int, target_id: int) -> None:
        """为子嗣安排联姻（可订婚或直接成婚）。"""
        w = self.sim.world
        player = w.character(self.player_id)
        child = w.character(child_id)
        target = w.character(target_id)
        if not player or not child or not target:
            raise ValueError("角色无效")
        if child not in player.children:
            raise ValueError("非玩家子嗣")
        if not child.is_alive() or not target.is_alive():
            raise ValueError("当事人已故")
        if child.gender == target.gender:
            raise ValueError("同性无法成婚")
        if child.is_married() or target.is_married():
            raise ValueError("一方已有配偶")
        if child.betrothed_to != NONE_ID or target.betrothed_to != NONE_ID:
            raise ValueError("一方已有婚约")
        if not child.is_adult(w.date) or not target.is_adult(w.date):
            # 任一方未成年 → 订婚，双方成年后自动成婚
            child.betrothed_to = target_id
            target.betrothed_to = child_id
            dip = self.sim.diplomacy
            dip.flags_mut(self.player_id, target_id).marriage_pact = True
            dip.treaties.append(self._make_treaty(target_id, TreatyKind.MARRIAGE_PACT, 50))
            self.notify(f"已与 {target.name} 家族为 {child.name} 订婚")
            w.push_log(f"{child.name} 与 {target.name} 订婚")
        else:
            # 至少一方成年 → 直接成婚
            ok = w.marry(child_id, target_id)
            if not ok:
                raise ValueError("婚姻失败")
            dip = self.sim.diplomacy
            dip.flags_mut(self.player_id, target_id).marriage_pact = True
            dip.treaties.append(self._make_treaty(target_id, TreatyKind.MARRIAGE_PACT, 50))
            self.notify(f"{child.name} 与 {target.name} 成婚")
            w.push_log(f"{child.name} 与 {target.name} 成婚")

    def _set_child_education(self, child_id: int, focus: str) -> None:
        """为未成年子女指定教育方向。"""
        child = self.sim.world.character(child_id)
        player = self.sim.world.character(self.player_id)
        focuses = {"diplomacy", "martial", "stewardship", "intrigue", "learning", "prowess"}
        if not child or not player or child_id not in player.children:
            raise ValueError("非玩家子女")
        if child.is_adult(self.sim.world.date):
            raise ValueError("成年角色无法更改教育方向")
        if focus not in focuses:
            raise ValueError("无效教育方向")
        child.education_focus = focus
        self.notify(f"已为 {child.name} 选择教育方向：{focus}")

    def _educate_children_yearly(self) -> None:
        """每年让受教育的未成年子女获得一点对应能力。"""
        player = self.sim.world.character(self.player_id)
        if not player:
            return
        for child_id in player.children:
            child = self.sim.world.character(child_id)
            if not child or not child.is_alive() or child.is_adult(self.sim.world.date):
                continue
            focus = child.education_focus
            if focus and hasattr(child.base_attrs, focus):
                setattr(child.base_attrs, focus, min(100, getattr(child.base_attrs, focus) + 1))
                child.education_years += 1
                self.sim.world.push_log(f"{child.name} 的{focus}教育取得进展")

    def _break_engagement(self, child_id: int) -> None:
        """解除婚约。"""
        w = self.sim.world
        child = w.character(child_id)
        if not child or child.betrothed_to == NONE_ID:
            raise ValueError("该角色无婚约")
        target_id = child.betrothed_to
        target = w.character(target_id)
        if target:
            target.betrothed_to = NONE_ID
        child.betrothed_to = NONE_ID
        dip = self.sim.diplomacy
        dip.flags_mut(self.player_id, target_id).marriage_pact = False
        # 移除联姻协定条约
        self.sim.diplomacy.treaties = [
            t for t in self.sim.diplomacy.treaties
            if t.kind != TreatyKind.MARRIAGE_PACT or t.a != self.player_id
        ]
        self.notify("已解除婚约")
        w.push_log("婚约解除")
