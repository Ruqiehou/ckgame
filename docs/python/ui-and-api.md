# UI 与接口（Python 版）

## TUI 文字界面

[game/tui.py](../../ck_engine/game/tui.py) 实现"命令行文字对话模式"，通过数字/字母菜单驱动所有交互：

- 信息查看：状态概览、角色详情、伯爵领列表/详情、军队、战争、宣称、条约、统治者、围城、日志、ASCII 地图
- 军事行动：征召、移动、解散军队；宣战、议和
- 内政：内阁任命与任务分配、阴谋发起、法律变更、建筑建造与领地发展
- 外交：结盟、互不侵犯、附庸、贸易协定、情报共享、联姻、赠礼、宿敌、邀请入宫廷、宴会、决斗
- 系统：推进时间、切换角色、存档管理、作弊开关

事件弹出时由 TUI 展示选项并调用 `resolve_event` 处理。

## GameAPI 接口

[ui/api.py](../../ck_engine/ui/api.py) 提供统一操作入口与状态快照。

### 状态

- `snapshot()`：返回完整游戏快照，包含 `player`、`date`、`season`、`characters`、`counties`、`armies`、`wars`、`claims`、`treaties`、`pending_events`、`log` 等字段。

### 操作

- `action(payload)`，payload 示例：

  ```python
  {"action": "advance", "days": 30}
  ```

常用操作：

- `advance`：推进时间
- `declare_war`：宣战
- `white_peace`：求和白和
- `form_alliance`：结盟
- `start_scheme`：发起阴谋
- `appoint_council` / `assign_council_task`：内阁任命与任务
- `set_succession_law` / `set_crown_authority` / `set_gender_law`：改法
- `upgrade_building` / `develop_county`：建造与发展
- `fabricate_claim`：伪造宣称
- `grant_title`：授予头衔
- `raise_army` / `move_army` / `disband_army`：军队管理
- `recruit_knights`：招募精锐
- `invite_to_court` / `host_feast_for` / `duel`：宫廷互动
- `save` / `load` / `delete_save` / `new_game`：存档管理
- `set_player`：切换玩家
- `toggle_cheat`：作弊开关
- `resolve_event`：处理弹出事件

## 地图

- 地图布局在 [ui/map_layout.py](../../ck_engine/ui/map_layout.py) 中配置
- 地图数据与场景数据位于 [data/](../../ck_engine/data/) 目录
