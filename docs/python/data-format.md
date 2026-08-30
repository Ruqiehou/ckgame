# 数据格式（Python 版）

## 存档

存档为 JSON 文件，存储于 `saves/` 目录（如 `autosave.json`），由 `GameAPI` 的存档相关 action 生成。

主要字段：

- `date` / `tick`：当前日期与帧计数
- `scenario`：场景 id（读档时按场景重建初始世界再覆盖动态状态）
- `player_id`：当前玩家
- `characters`：人物动态状态（资源、健康、亲属、持有头衔、婚约、教育方向与受教育年数、特质、好感缓存等）
- `counties`：省份持有者、控制度、发展度与建筑
- `titles`：头衔持有者与法理层级
- `wars` / `armies` / `sieges`：战争（含参战方与战争分数）、军团、围城
- `diplomacy`：关系标记、条约、宣称、停战、战争疲劳
- `factions` / `pending_ultimatums`：派系与挂起的最后通牒
- `schemes`：进行中的阴谋
- `councils`：内阁成员与任务分配
- `decisions`：重大决策的冷却与历史记录
- `chains`：事件链进度（id、是否激活/完成、当前阶段、参与者、开始日期与阶段起始日）
- `log` / `messages`：近期日志与消息

## 场景

场景数据在 [data/scenarios/](../../ck_engine/data/scenarios/) 中，包含初始地图、角色、头衔与关系。

默认场景：`1066.json`（诺曼征服）

## 地图布局

地图布局在 [data/map_layouts/](../../ck_engine/data/map_layouts/) 中，定义省份坐标与邻接关系。
