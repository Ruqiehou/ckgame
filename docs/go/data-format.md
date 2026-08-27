# 数据格式（Go 版）

## 存档

存档为 JSON 文件，通过 `encoding/json` 序列化，存储于 `saves/` 目录。

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

## 序列化

使用 Go 标准库 `encoding/json`：

```go
import "encoding/json"

// 序列化
data, err := json.MarshalIndent(save, "", "  ")
os.WriteFile("saves/autosave.json", data, 0644)

// 反序列化
var save SaveData
data, _ := os.ReadFile("saves/autosave.json")
json.Unmarshal(data, &save)
```

## 场景

场景数据在 `data/scenarios/` 中，包含初始地图、角色、头衔与关系。

默认场景：`1066.json`（诺曼征服）

## 地图布局

地图布局在 `data/map_layouts/` 中，定义省份坐标与邻接关系。