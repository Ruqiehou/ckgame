# 引擎架构（C++ 版）

## 项目结构

```
ckgame-cpp/
  CMakeLists.txt
  src/
    core/
      calendar/    - 日期与季节（date.h/cpp, season.h/cpp）
      stats/       - 角色属性（attribute_set.h/cpp）
      traits/      - 特性定义（traits.h/cpp）
      balance/     - 平衡常量（balance.h）
      constants.h  - 基础常量（kNoneId）
      gender.h     - 性别枚举
      title_tier.h - 头衔等级
    ui/
      GameTUI.h/cpp - 文字界面（演示）
    main.cpp       - 主入口
  data/
    scenarios/     - 场景数据（1066.json）
    map_layouts/   - 地图布局
  saves/           - 存档目录
```

## 命名空间

- `ckgame`：顶层命名空间
- `ckgame::calendar`：日期与季节
- `ckgame::stats`：角色属性
- `ckgame::traits`：特性系统
- `ckgame::balance`：平衡常量（`constexpr` 内联函数）
- `ckgame::ui`：界面

## 已实现模块

| 模块 | 文件 | 说明 |
|------|------|------|
| 常量 | [constants.h](../../ckgame-cpp/src/core/constants.h) | `kNoneId` |
| 性别 | [gender.h](../../ckgame-cpp/src/core/gender.h) | `Gender` 枚举 + `GenderName()` |
| 头衔 | [title_tier.h](../../ckgame-cpp/src/core/title_tier.h) | 五级爵位、前缀、创建威望、可销毁判定 |
| 日期 | [date.h](../../ckgame-cpp/src/core/calendar/date.h) | `GameDate` 类：季节、闰年、推进、儒略日序、年龄 |
| 属性 | [attribute_set.h](../../ckgame-cpp/src/core/stats/attribute_set.h) | 五维属性（外/军/管/谋/学），加值限幅 0-100 |
| 特性 | [traits.h](../../ckgame-cpp/src/core/traits/traits.h) | 28 个内置特性（性格/身体），按 id 查询 |
| 平衡 | [balance.h](../../ckgame-cpp/src/core/balance/balance.h) | 军事/经济/政治/战争/派系常量 |
| 界面 | [GameTUI.cpp](../../ckgame-cpp/src/ui/GameTUI.cpp) | 演示输出（core 模块自检） |

## 核心流程（规划）

- `game/GameSimulation`：主循环（`TickDay()` 按日推进、`TickMonth()` 月度结算）
- `ui/GameAPI`：统一接口（`Snapshot()` / `Action(payload)`）
- `ui/GameTUI`：完整文字菜单

## 数据流

1. TUI 通过 `GameAPI::Snapshot()` 获取完整状态
2. 用户通过 `GameAPI::Action()` 下发操作
3. 模拟状态变更通过返回快照渲染到界面

## 与其他版本的对应

| Go | C++ |
|----|-----|
| `core.NoneID` | `ckgame::kNoneId` |
| `core.Gender` | `ckgame::Gender` |
| `core.TitleTier` | `ckgame::TitleTier` |
| `calendar.GameDate` | `ckgame::calendar::GameDate` |
| `stats.AttributeSet` | `ckgame::stats::AttributeSet` |
| `traits.Find(id)` | `ckgame::traits::Find(id)` |
| `balance.*` | `ckgame::balance::k*` |