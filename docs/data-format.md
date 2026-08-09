# 数据格式

## 存档

存档为 JSON 文件，由 `GameAPI._save()` 生成。

主要字段：

- `world`：世界状态
- `wars`：进行中战争
- `sieges`：围城状态
- `factions`：派系
- `schemes`：阴谋
- `diplomacy`：外交关系
- `councils`：内阁
- `realm_laws`：法律
- `player_id`：当前玩家
- `date`：当前日期

## 场景

场景数据在 [data/scenarios/](data/scenarios/) 中，包含初始地图、角色、头衔与关系。
