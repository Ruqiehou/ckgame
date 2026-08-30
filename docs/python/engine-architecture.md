# 引擎架构（Python 版）

## 项目结构

```
ck_engine/
  ai/           - AI 行为与决策
  core/         - 基础类型、平衡常量、日期
  data/         - 地图布局与场景数据
  events/       - 事件引擎、多阶段事件链与剧情
  game/         - 模拟循环、场景加载、TUI 入口
  military/     - 军团、战斗、围城、战争
  politics/     - 法律、内阁、外交、派系、阴谋、重大决策
  ui/           - GameAPI、地图布局、教程
  world/        - 角色、头衔、地图、世界状态
```

## 核心流程

- [game/simulation.py](../../ck_engine/game/simulation.py) 的 `GameSimulation` 是主循环。
- `tick_day()` 推进日期、处理军队移动、遭遇战与围城。
- `tick_month()` 结算经济、内阁、事件、派系、阴谋与战争。
- [ui/api.py](../../ck_engine/ui/api.py) 的 `GameAPI` 提供统一操作接口：
  - `snapshot()` 获取当前状态快照
  - `action(payload)` 执行操作
- [game/tui.py](../../ck_engine/game/tui.py) 的 `GameTUI` 实现文字界面主循环。

## 数据流

1. TUI 通过 `GameAPI.snapshot()` 获取完整状态快照。
2. 用户通过 `GameAPI.action()` 下发操作。
3. 状态变更通过返回消息与新快照渲染到界面。
