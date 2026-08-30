# 玩法目录

「玩法」是围绕角色与领地、可独立扩展的游戏机制（狩猎、养生等）。
所有玩法统一登记在 `ck_engine/gameplay/` 目录下，由 `GameplayManager` 统一调度，
本文档即玩法的总目录：既列出已实现与规划中的玩法，也说明扩展方式。

## 目录结构

```
ck_engine/gameplay/
├── __init__.py     # 对外导出（Gameplay / GameplayEntry / GameplayManager / catalog）
├── base.py         # Gameplay 基类与目录条目 GameplayEntry
├── registry.py     # 玩法目录注册表（GAMEPLAYS / PLANNED_GAMEPLAYS）
├── manager.py      # GameplayManager：月度结算、玩家操作、存档
├── hunting.py      # 狩猎
└── wellness.py     # 养生
```

## 玩法清单

### 已实现

| 玩法 | id | 分类 | 一句话说明 |
|------|----|------|------------|
| 狩猎 | `hunting` | 生活 | 组织围猎消遣：缓解压力、赢得威望，秋季收获最丰，但可能坠马受伤 |
| 养生 | `wellness` | 生活 | 聘请宫廷医师：按月付薪，缓慢恢复健康、纾解压力，欠薪则医师出走 |

### 规划中

| 玩法 | id | 分类 | 设想 |
|------|----|------|------|
| 朝圣 | `pilgrimage` | 信仰 | 远赴圣地朝拜，换取虔诚与声望，路途或有风险 |
| 营造奇观 | `monument` | 建设 | 投入金币营造传世建筑，持续提供威望与领地繁荣 |

## 玩法机制

### 狩猎

- 玩家操作「组织狩猎」：花费 60 金，冷却 3 个月。
- 效果：压力 -20；威望 +（10 + 秋季加成 8 + 军略/4）。
- 随机结果：15% 猎获巨鹿（威望 +15）；10% 坠马受伤（健康 -1、压力 +5），两者不叠加。
- AI 君主在秋季有 8% 概率自发组织狩猎（花费 40 金，压力 -10，威望 +5）。

### 养生

- 玩家操作「聘请宫廷医师」：聘礼 30 金，此后每月薪水 10 金。
- 月度效果：健康 +0.2（上限 10）、压力 -3。
- 金币不足以支付月薪时医师离开宫廷；也可随时主动辞退。

## 调度与接口

- **月度结算**：`GameSimulation.tick_month` 末尾调用 `gameplays.tick_month(sim)`，对每位统治者依序结算各玩法的被动效果（冷却递减、医师薪水、AI 自发行为）。
- **玩家操作**：统一入口为
  `GameAPI.action({"action": "gameplay_action", "gameplay": "hunting", "op": "organize"})`，
  不可执行时抛出 `ValueError` 并以「操作失败：原因」通知。
- **快照**：`snapshot()["gameplays"]` 返回：
  - `catalog`：完整玩法目录（含规划中条目，带 `status` 字段）；
  - `player`：玩家在各玩法中的 `state` 与可用 `actions`。
- **存档**：存档 JSON 中的 `gameplays` 键保存各玩法的运行时状态（狩猎冷却、宫廷医师），读档时经 `GameplayManager.load_state` 恢复。

## 如何新增玩法

1. 在 `ck_engine/gameplay/` 下新建模块，继承 `Gameplay` 并设置 `entry` 元数据；按需实现 `tick_month`（被动结算）、`actions` / `can_perform` / `perform`（玩家操作）、`state`（快照展示）、`save_state` / `load_state`（持久化）。
2. 在 `registry.py` 的 `GAMEPLAYS` 中登记 `(entry, 构造器)`；仅规划中的玩法放入 `PLANNED_GAMEPLAYS`，主循环不会调度它们。
3. 如有运行时状态，确保 `save_state / load_state` 完整——存档会自动带上 `gameplays` 键，无需改动存档代码。
4. 在本文档的「玩法清单」中补一行，并在 `tests/test_gameplay.py` 增加用例。

相关模块：重大决策（`ck_engine/politics/decisions.py`）提供战略级一次性决策，玩法目录则承载可重复进行的生活/活动类机制；二者共用 `GameSimulation` 月度结算与 `GameAPI` 操作入口。
