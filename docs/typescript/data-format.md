# 数据格式（TypeScript 版）

## 存档

存档为 JSON 文件，通过 `fs.writeFileSync()` 写入，默认存储于项目根目录的 `saves/` 目录。

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

场景数据在 `data/scenarios/` 中，包含初始地图、角色、头衔与关系。

默认场景：`1066.json`（诺曼征服）

## JSON 处理

使用 Node.js 内置 `fs` 模块读写 JSON：

```typescript
import fs from 'fs';

// 读取
const data = JSON.parse(fs.readFileSync('path/to/file.json', 'utf-8'));

// 写入
fs.writeFileSync('path/to/file.json', JSON.stringify(data, null, 2));
```

## 类型安全

虽然运行时数据为 JSON 对象，源代码通过 TypeScript 接口定义确保类型安全。