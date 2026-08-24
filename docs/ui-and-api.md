# UI 与接口

## 前端

- [ui/static/index.html](ui/static/index.html)：主页面
- [ui/static/app.js](ui/static/app.js)：前端逻辑与渲染
- [ui/static/style.css](ui/static/style.css)：样式

## 后端 API

### 状态

- `GET /api/state`：返回完整游戏快照

### 操作

- `POST /api/action`，body 示例：
  ```json
  { "action": "advance", "days": 30 }
  ```

常用操作：

- `advance`：推进时间
- `declare_war`：宣战
- `white_peace`：求和白和
- `form_alliance`：结盟
- `start_scheme`：发起阴谋
- `appoint_council`：任命内阁
- `set_succession_law` / `set_crown_authority` / `set_gender_law`：改法
- `upgrade_building` / `develop_county`：建造与发展
- `fabricate_claim`：伪造宣称
- `grant_title`：授予头衔
- `invite_to_court` / `host_feast_for` / `duel`：宫廷互动
- `set_player`：切换玩家

## 地图

- 地图布局在 [ui/map_layout.py](ui/map_layout.py) 中配置
- 省份坐标与多边形在 `layout_for()` 中定义
