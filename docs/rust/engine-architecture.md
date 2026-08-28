# 引擎架构（Rust 版）

## 项目结构

```
ckgame-rust/
  Cargo.toml
  src/
    main.rs          - 主入口
    core/            - 核心模块
      mod.rs         - 模块导出
      constants.rs   - 基础常量
      gender.rs      - 性别枚举
      title_tier.rs  - 头衔等级
      calendar/      - 日期与季节
        mod.rs
        season.rs    - 季节枚举与名称
        date.rs      - 游戏日期（基于 chrono）
      stats.rs       - 角色属性（五维）
      traits.rs      - 特性定义
      balance.rs     - 平衡常量
    ui/              - 用户界面
      mod.rs
      tui.rs         - 文字界面（演示）
  data/
    scenarios/       - 场景数据（1066.json）
    map_layouts/     - 地图布局
  saves/             - 存档目录
```

## 模块设计

### core 模块

| 模块 | 文件 | 说明 |
|------|------|------|
| 常量 | [constants.rs](../../ckgame-rust/src/core/constants.rs) | `pub const NONE_ID: i32 = -1` |
| 性别 | [gender.rs](../../ckgame-rust/src/core/gender.rs) | `Gender` 枚举 + `gender_name()` |
| 头衔 | [title_tier.rs](../../ckgame-rust/src/core/title_tier.rs) | 五级爵位、前缀、创建威望、可销毁 |
| 日期 | [calendar/date.rs](../../ckgame-rust/src/core/calendar/date.rs) | `GameDate` 结构体：季节、闰年、推进、序数、年龄（基于 `chrono::NaiveDate`） |
| 属性 | [stats.rs](../../ckgame-rust/src/core/stats.rs) | 五维属性、加值限幅 0-100 |
| 特性 | [traits.rs](../../ckgame-rust/src/core/traits.rs) | 28 个内置特性（静态切片 `ALL_TRAITS`） |
| 平衡 | [balance.rs](../../ckgame-rust/src/core/balance.rs) | 军事/经济/政治/战争/派系常量 |

### UI 模块

- [ui/tui.rs](../../ckgame-rust/src/ui/tui.rs)：演示入口，输出 core 模块自检信息

## 命名约定

- 结构体与枚举：`PascalCase`
- 函数：`snake_case`
- 常量：`SCREAMING_SNAKE_CASE`
- 模块：`snake_case`

## 核心流程（规划）

- `game/GameSimulation`：主循环（`tick_day()` / `tick_month()`）
- `ui/GameAPI`：统一接口（`snapshot()` / `action()`）
- `ui/GameTUI`：完整文字菜单

## 与其他版本的对应

| Go | C++ | Rust |
|----|-----|------|
| `core.NoneID` | `ckgame::kNoneId` | `core::NONE_ID` |
| `core.Gender` | `ckgame::Gender` | `core::gender::Gender` |
| `core.TitleTier` | `ckgame::TitleTier` | `core::title_tier::TitleTier` |
| `calendar.GameDate` | `ckgame::calendar::GameDate` | `core::calendar::date::GameDate` |
| `stats.AttributeSet` | `ckgame::stats::AttributeSet` | `core::stats::AttributeSet` |
| `traits.Find(id)` | `ckgame::traits::Find(id)` | `core::traits::find(id)` |
| `balance.*` | `ckgame::balance::k*` | `core::balance::*` |

## 所有权与借用

- 核心数据结构使用 `Copy` + `Clone`（简单值类型）
- 特性表使用 `&'static [Trait]` 静态切片，无动态分配
- 大型数据结构（如 `World`）将使用 `Rc<RefCell<...>>` 或 `Arc<RwLock<...>>` 进行共享访问