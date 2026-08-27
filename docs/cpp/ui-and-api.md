# UI 与接口（C++ 版）

## TUI 界面

[ui/GameTUI.cpp](../../ckgame-cpp/src/ui/GameTUI.cpp) 当前为演示入口，输出 core 模块自检信息（日期、季节、属性、特性、性别、头衔）。

完整文字菜单（与其他版本一致，规划中）：

- `[1-12]` 信息查看：状态、角色、领地、军队、战争、宣称、条约、统治者、围城、日志、地图
- `[A-L]` 军事行动：征召、移动、解散、宣战、议和、改善关系、举办宴会、建造、发展、伪造宣称、授予头衔、招募骑士
- `[M-Q]` 管理功能：内阁、阴谋、外交、法律、存档
- `[R]` 推进时间、`[S]` 切换角色、`[T]` 作弊、`[0]` 退出

## GameAPI 接口（规划）

```cpp
// 状态查询
Snapshot snap = api.Snapshot();

// 操作执行
ActionResult res = api.Action({"raise_army", /*county_id=*/1});
```

常用 action 类型（与其他版本对齐）：
- `advance`：推进时间（days 参数）
- `raise_army` / `move_army` / `disband_army`：军队管理
- `declare_war` / `white_peace`：战争
- `form_alliance`：结盟
- `start_scheme`：发起阴谋
- `set_succession_law` / `set_crown_authority` / `set_gender_law`：改法
- `upgrade_building` / `develop_county`：建造与发展
- `fabricate_claim` / `grant_title`：宣称与头衔
- `recruit_knights` / `hold_feast` / `improve_relations`：宫廷互动
- `set_player`：切换角色
- `save` / `load` / `delete_save` / `new_game`：存档管理
- `toggle_cheat`：作弊开关

## 中文输出

Windows 控制台中文显示依赖编译选项 `/utf-8`（已在 CMakeLists.txt 中为 MSVC 配置）。GCC/Clang 默认 UTF-8 无需处理。