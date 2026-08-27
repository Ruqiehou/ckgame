# 数据格式（Java 版）

## 存档

存档为 JSON 文件，由 `SaveManager` 生成，存储于 `saves/` 目录。

主要字段：

- `world`：世界状态（角色、省份、头衔等）
- `wars`：进行中战争列表
- `sieges`：围城状态列表
- `factions`：派系列表
- `schemes`：阴谋列表
- `diplomacy`：外交关系
- `councils`：内阁状态
- `realm_laws`：法律状态
- `player_id`：当前玩家 ID
- `date`：当前日期（YYYY-MM-DD）
- `season`：季节（SPRING/SUMMER/AUTUMN/WINTER）

## 场景

场景数据在 `src/main/resources/data/scenarios/` 中，包含初始地图、角色、头衔与关系。

默认场景：`1066.json`（诺曼征服）

## 地图布局

地图布局数据在 `src/main/resources/data/map_layouts/` 中。

## JSON 处理

使用 Jackson 库（`com.fasterxml.jackson.databind`）进行序列化与反序列化：

```java
ObjectMapper mapper = new ObjectMapper();
String json = mapper.writeValueAsString(object);
Object obj = mapper.readValue(json, Object.class);
```
