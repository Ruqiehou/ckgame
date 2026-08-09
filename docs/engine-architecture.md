# 引擎架构

## 项目结构

```
ck_engine/
  ai/           - AI 行为与决策
  core/         - 基础类型、平衡常量、日期
  data/         - 地图布局与场景数据
  events/       - 事件引擎与剧情
  game/         - 模拟循环、场景加载、主入口
  military/     - 军团、战斗、围城、战争
  politics/     - 法律、内阁、外交、派系、阴谋
  ui/           - Web UI、API、地图布局
  world/        - 角色、头衔、地图、世界状态
```

## 核心流程

- [game/simulation.py](game/simulation.py) 的 `GameSimulation` 是主循环。
- `tick_day()` 推进日期、处理军队移动、遭遇战与围城。
- `tick_month()` 结算经济、内阁、事件、派系、阴谋与战争。
- [ui/api.py](ui/api.py) 的 `GameAPI` 提供前端操作入口与状态快照。
- [ui/server.py](ui/server.py) 提供 HTTP 服务。

## 数据流

1. 前端通过 `/api/state` 获取快照。
2. 通过 `/api/action` 下发操作。
3. `GameAPI._action_unlocked()` 修改模拟状态。
4. 状态变更通过快照返回前端渲染。
