# UI 与接口（Go 版）

## TUI 界面

基于 `termbox-go` 实现字符模式终端界面，通过键盘输入菜单选项驱动所有交互。

主菜单功能（与 Python/Java/TS 版一致）：

- `[1-12]` 信息查看：状态、角色、领地、军队、战争、宣称、条约、统治者、围城、日志、地图
- `[A-L]` 军事行动：征召、移动、解散、宣战、议和、改善关系、举办宴会、建造、发展、伪造宣称、授予头衔、招募骑士
- `[M-Q]` 管理功能：内阁、阴谋、外交、法律、存档
- `[R]` 推进时间、`[S]` 切换角色、`[T]` 作弊、`[0]` 退出

## GameAPI 接口

提供统一操作接口：

### 状态查询

```go
snap := api.Snapshot()
```

返回包含以下字段：
- `player`：当前玩家信息（金币、威望、虔诚、压力、健康、属性、法律）
- `date`：当前日期（YYYY-MM-DD）
- `season`：季节（SPRING/SUMMER/AUTUMN/WINTER）
- `characters`：角色列表
- `counties`：省份列表
- `armies`：军队列表
- `wars`：战争列表
- `treaties`：条约列表
- `schemes`：阴谋列表
- `factions`：派系列表
- `log`：事件日志

### 操作执行

```go
res := api.Action(ActionRequest{
    Action: "raise_army",
    CountyID: 1,
})
```

常用 action 类型：
- `advance`：推进时间（days 参数）
- `raise_army`：征召军队（county_id）
- `move_army`：移动军队（army_id, county_id）
- `disband_army`：解散军队（army_id）
- `declare_war`：宣战（target_id）
- `white_peace`：白和（war_id）
- `form_alliance`：结盟（target_id）
- `start_scheme`：发起阴谋（scheme_kind, target_id）
- `set_succession_law` / `set_crown_authority` / `set_gender_law`：改法
- `upgrade_building` / `develop_county`：建造与发展
- `fabricate_claim`：伪造宣称（county_id）
- `grant_title`：授予头衔（title_id, target_id）
- `recruit_knights`：招募精锐
- `hold_feast`：举办宴会
- `improve_relations`：改善关系（target_id）
- `set_player`：切换角色（character_id）
- `save` / `load` / `delete_save`：存档管理
- `new_game`：新游戏
- `toggle_cheat`：作弊开关

## 界面渲染

使用 `termbox-go` 绘制字符界面：
- 光标定位、颜色设置、字符输出
- 处理键盘输入与事件循环
- 适配终端尺寸，自动计算布局宽度