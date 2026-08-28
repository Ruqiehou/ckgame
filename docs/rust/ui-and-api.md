# UI 与接口（Rust 版）

## TUI 界面

[ui/tui.rs](../../ckgame-rust/src/ui/tui.rs) 当前为演示入口，输出 core 模块自检信息（日期、季节、属性、特性、性别、头衔）。

完整文字菜单（与其他版本一致，规划中）：

- `[1-12]` 信息查看：状态、角色、领地、军队、战争、宣称、条约、统治者、围城、日志、地图
- `[A-L]` 军事行动：征召、移动、解散、宣战、议和、改善关系、举办宴会、建造、发展、伪造宣称、授予头衔、招募骑士
- `[M-Q]` 管理功能：内阁、阴谋、外交、法律、存档
- `[R]` 推进时间、`[S]` 切换角色、`[T]` 作弊、`[0]` 退出

## GameAPI 接口（规划）

```rust
// 状态查询
let snap = api.snapshot()?;

// 操作执行
let res = api.action(Action::RaiseArmy { county_id: 1 })?;
```

常用 action 类型（使用枚举确保类型安全）：
- `Advance { days }`：推进时间
- `RaiseArmy { county_id }` / `MoveArmy { army_id, county_id }` / `DisbandArmy { army_id }`：军队管理
- `DeclareWar { target_id }` / `WhitePeace { war_id }`：战争
- `FormAlliance { target_id }`：结盟
- `StartScheme { scheme_kind, target_id }`：发起阴谋
- `SetSuccessionLaw` / `SetCrownAuthority` / `SetGenderLaw`：改法
- `UpgradeBuilding { building_kind }` / `DevelopCounty { county_id }`：建造与发展
- `FabricateClaim { county_id }` / `GrantTitle { title_id, target_id }`：宣称与头衔
- `RecruitKnights` / `HoldFeast` / `ImproveRelations { target_id }`：宫廷互动
- `SetPlayer { character_id }`：切换角色
- `Save { name }` / `Load { name }` / `DeleteSave { name }` / `NewGame`：存档管理
- `ToggleCheat`：作弊开关

## UTF-8 支持

Rust 字符串原生 UTF-8，终端中文显示依赖：
- Windows：设置控制台代码页为 UTF-8（`chcp 65001`）
- Linux/macOS：默认 UTF-8

## termion 库

计划使用 `termion` 实现字符模式 TUI：
- 原生终端操作（无需 ncurses）
- 跨平台（Unix-like / Windows 10+）
- 事件驱动输入处理

```rust
use termion::{raw::IntoRawMode, screen::AlternateScreen};
use std::io::{Write, stdout};

let mut screen = AlternateScreen::from(stdout().into_raw_mode()?);
write!(screen, "{}{}", termion::clear::All, termion::cursor::Goto(1, 1))?;
```