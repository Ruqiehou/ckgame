# 数据格式（Rust 版）

## 存档

存档为 JSON 文件，使用 `serde` + `serde_json` 序列化，存储于 `saves/` 目录。

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

日期序列化示例（对应 `GameDate::to_array()` / `from_array()`）：

```json
{"date": [1066, 1, 1]}
```

## 序列化

使用 `#[derive(Serialize, Deserialize)]` 自动生成序列化代码：

```rust
use serde::{Serialize, Deserialize};

#[derive(Serialize, Deserialize, Debug)]
pub struct SaveData {
    pub world: World,
    pub wars: Vec<War>,
    pub date: Vec<i64>,
}

// 序列化
let json = serde_json::to_string_pretty(&save)?;
std::fs::write("saves/autosave.json", json)?;

// 反序列化
let data = std::fs::read_to_string("saves/autosave.json")?;
let save: SaveData = serde_json::from_str(&data)?;
```

## 场景

场景数据在 `data/scenarios/` 中，包含初始地图、角色、头衔与关系。

默认场景：`1066.json`（诺曼征服）

## 地图布局

地图布局在 `data/map_layouts/` 中，定义省份坐标与邻接关系。

## chrono 集成

日期使用 `chrono::NaiveDate` 进行计算，存档时序列化为整数数组：

```rust
impl GameDate {
    pub fn to_naive_date(&self) -> Option<NaiveDate> {
        NaiveDate::from_ymd_opt(self.year, self.month, self.day)
    }

    pub fn from_naive_date(date: NaiveDate) -> Self {
        GameDate { year: date.year(), month: date.month(), day: date.day() }
    }
}
```