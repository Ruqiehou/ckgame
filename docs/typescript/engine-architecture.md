# 引擎架构（TypeScript 版）

## 项目结构

```
ckgame-ts/
  src/
    ai/           - AI 行为与决策
    core/         - 基础类型、平衡常量、日期
    events/       - 事件引擎与剧情
    game/         - 模拟循环、场景加载、主入口
    military/     - 军团、战斗、围城、战争
    politics/     - 法律、内阁、外交、派系、阴谋
    ui/           - GameTUI 文字界面、GameAPI、Main
    world/        - 角色、头衔、地图、世界状态
  data/
    scenarios/    - 场景数据（1066.json）
  dist/          - TypeScript 编译输出
```

## 核心流程

- [game/GameSimulation.ts](../../ckgame-ts/src/game/GameSimulation.ts) 是主模拟循环
- `tickDay()` 推进日期、处理军队移动、遭遇战与围城
- `tickMonth()` 结算经济、内阁、事件、派系、阴谋与战争
- [ui/GameAPI.ts](../../ckgame-ts/src/ui/GameAPI.ts) 提供统一操作接口
  - `snapshot()` 获取当前状态快照
  - `action(payload)` 执行操作
- [ui/GameTUI.ts](../../ckgame-ts/src/ui/GameTUI.ts) 实现完整文字界面
- [ui/Main.ts](../../ckgame-ts/src/ui/Main.ts) 主入口

## 数据流

1. TUI 通过 `GameAPI.snapshot()` 获取完整状态
2. 用户通过 `GameAPI.action()` 下发操作
3. 模拟状态变更通过返回快照渲染到界面

## 关键类

- `GameSimulation`：主循环，处理时间推进与月度结算
- `GameAPI`：统一接口层，封装所有操作
- `GameTUI`：文字界面，菜单与交互（部分功能占位）
- `World`：世界状态，包含角色、省份、战争等

## 编译与运行

```bash
# 编译
tsc

# 运行
node dist/ui/Main.js

# 开发模式（编译+运行）
npm run dev
```