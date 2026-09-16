# UI 与接口（Python 版）

## TUI 文字界面

[game/tui.py](../../ck_engine/game/tui.py) 实现"命令行文字对话模式"，通过数字/字母菜单驱动所有交互：

- 信息查看：状态概览、角色详情、伯爵领列表/详情、军队、战争、宣称、条约、统治者、围城、日志、ASCII 地图
- 军事行动：征召、移动、解散军队；宣战、议和
- 内政：内阁任命与任务分配、阴谋发起、法律变更、建筑建造与领地发展、重大决策执行、子女教育与婚约管理
- 外交：结盟、互不侵犯、附庸、贸易协定、情报共享、联姻、赠礼、宿敌、邀请入宫廷、宴会、决斗
- 系统：推进时间、切换角色、存档管理、教程、作弊开关

事件弹出时由 TUI 展示选项并调用 `resolve_event` 处理；派系最后通牒挂起时由 TUI 弹出接受/拒绝菜单，调用 `respond_ultimatum` 处理。

## GameAPI 接口

[ui/api.py](../../ck_engine/ui/api.py) 提供统一操作入口与状态快照。按职责拆分为三个 Mixin：`ui/snapshot.py`（快照构建）、`ui/savegame.py`（存档/读档）、`ui/diplomacy_actions.py`（外交操作）。

### 状态

- `snapshot()`：返回完整游戏快照，包含 `player`、`date`、`season`、`characters`、`counties`、`armies`、`wars`、`player_claims`、`treaties`、`pending_events`、`pending_ultimatums`、`decisions`（可执行的重大决策）、`chains`（事件链进度）、`storylines`（剧情线）、`gameplays`（玩法状态）、`trade_routes`、`family`、`tutorial`、`log` 等字段。

### 操作

- `action(payload)`，payload 示例：

  ```python
  {"action": "advance", "days": 30}
  ```

常用操作：

- `advance`：推进时间；`days` 会限制在 `1` 到 `365` 天之间
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
- `respond_ultimatum`：回应派系最后通牒（`{"action": "respond_ultimatum", "faction_id": 1, "accept": true}`；接受则落实诉求，拒绝则立即叛乱）
- `appease_faction`：安抚派系
- `execute_decision`：执行重大决策（如 `{"action": "execute_decision", "decision_id": "hold_tournament"}`；注意键名是 `decision_id` 而非 `id`，不满足条件或冷却中会报错）
- `arrange_marriage` / `arrange_child_marriage` / `break_engagement`：联姻与婚约管理
- `set_child_education`：为未成年子女指定教育方向（`{"action": "set_child_education", "child_id": 5, "focus": "martial"}`）
- `tutorial_next` / `tutorial_skip`：教程推进与跳过

`action()` 会返回操作后的完整快照。业务校验失败不会向调用方抛出异常，错误会写入快照的 `messages`；例如金币不足、玩法冷却中或决策前提不满足。未知 action 也会保留当前状态并返回提示。

## WebUI HTTP 接口

启动方式：

```bash
python -m ck_engine.web.server --host 127.0.0.1 --port 8000 --scenario 1066
```

主要路由：

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/` | 返回 WebUI 页面 |
| `GET` | `/static/<file>` | 返回静态资源 |
| `GET` | `/api/snapshot` | 返回当前游戏快照 |
| `GET` | `/api/scenarios` | 返回可用场景列表 |
| `POST` | `/api/action` | 接收 JSON action 并返回新快照 |

`POST /api/action` 的请求体必须是 JSON 对象，例如：

```json
{"action": "advance", "days": 30}
```

非法 JSON 或非对象请求体返回 HTTP 400；合法但业务失败的 action 仍返回快照，由 `messages` 描述失败原因。

## 地图

- 地图布局在 [ui/map_layout.py](../../ck_engine/ui/map_layout.py) 中配置
- 地图数据与场景数据位于 [data/](../../ck_engine/data/) 目录
