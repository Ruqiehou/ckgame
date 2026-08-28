# 引擎架构（Go 版）

## 项目结构

```
ckgame-go/
  main.go        - 命令行演示入口（加载场景、推进数月）
  core/          - 基础类型、平衡常量、日期、属性、特性
  ai/            - AI 行为与决策（月度集结）
  events/        - 事件引擎（骨架）
  military/      - 军团、宣战、白和、月度结算（战斗与围城待实现）
  politics/      - 条约与关系（法律、内阁、派系、阴谋待实现）
  world/         - 角色、头衔、地图、世界状态与场景加载
  go.mod
```

## 已实现模块

- `main.go`：命令行演示入口
- `world/world.go`：World/Dynasty/County/Title/Character 与 `LoadScenario()`（兼容 ckgame-java/ckgame-ts 的 1066.json 场景格式）
- `military/military.go`：集结/行军/解散/宣战/白和与月度补给结算
- `politics/politics.go`：条约缔结与关系改善
- `events/events.go`：事件实例与决议（触发逻辑待实现）
- `ai/ai.go`：月度集结决策（其余决策待实现）
- `core/`：基础类型与常量
  - `balance/balance.go`：游戏平衡参数
  - `calendar/date.go`：日期与季节
  - `stats/attributeset.go`：角色属性集合
  - `traits/traits.go`：特性定义
  - `constants.go`、`gender.go`、`title_tier.go`：核心常量
- `go.mod`：依赖 `go-runewidth`、`termbox-go`（为 TUI 预留，当前未引入）

## 核心流程

- `game/GameSimulation.go`（待实现）：主循环
  - `tickDay()` 推进日期、处理军队移动、遭遇战与围城
  - `tickMonth()` 结算经济、内阁、事件、派系、阴谋与战争
- `ui/GameAPI.go`（待实现）：统一操作接口
  - `snapshot()` 获取当前状态快照
  - `action(payload)` 执行操作
- `ui/GameTUI.go`（待实现）：文字界面
- `ui/Main.go`（待实现）：主入口

## 数据流

1. TUI 通过 `GameAPI.snapshot()` 获取完整状态
2. 用户通过 `GameAPI.action()` 下发操作
3. 模拟状态变更通过返回快照渲染到界面

## 界面技术

- `termbox-go`：字符模式 TUI 渲染
- `go-runewidth`：终端列宽计算（支持 CJK）