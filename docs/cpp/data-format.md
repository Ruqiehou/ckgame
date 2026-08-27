# 数据格式（C++ 版）

## 存档

存档为 JSON 文件，存储于 `saves/` 目录。

主要字段（与其他版本一致）：

- `world`：世界状态（角色、省份、头衔等）
- `wars`：进行中战争列表
- `sieges`：围城状态列表
- `factions`：派系列表
- `schemes`：阴谋列表
- `diplomacy`：外交关系
- `councils`：内阁状态
- `realm_laws`：法律状态
- `player_id`：当前玩家 ID
- `date`：当前日期 `[year, month, day]`

日期序列化示例（对应 `GameDate::ToArray()` / `GameDate::Parse()`）：

```json
{"date": [1066, 1, 1]}
```

## 场景

场景数据在 `data/scenarios/` 中，包含初始地图、角色、头衔与关系。

默认场景：`1066.json`（诺曼征服）

## 地图布局

地图布局在 `data/map_layouts/` 中，定义省份坐标与邻接关系。

## JSON 处理（规划）

当前版本未引入第三方库。后续可选方案：

- [nlohmann/json](https://github.com/nlohmann/json)：单头文件，API 友好
- 手写简易解析器：零依赖，但维护成本高

```cpp
// nlohmann/json 示例
#include <nlohmann/json.hpp>
nlohmann::json j = nlohmann::json::parse(text);
int year = j["date"][0];
```