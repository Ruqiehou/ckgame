# CK-Style Grand Strategy Engine

十字军之王风格大战略模拟引擎，提供四个语言实现。  
默认场景：**1066 英格兰**（诺曼征服前夜）。

作者：Ruqiehou  
更新：2026-08-27

---

## 四个版本

| 版本 | 目录 | 环境 | 快速开始 |
|------|------|------|----------|
| Python | `ck_engine/` | Python 3.10+，标准库 | `python -m ck_engine.game.tui` |
| Java | `ckgame-java/` | Java 17+，Maven + Jackson | `mvn clean package && java -jar ckgame-java/target/ck-game.jar` |
| TypeScript | `ckgame-ts/` | Node.js 20+，TypeScript | `cd ckgame-ts && npm install && npm run dev` |
| Go | `ckgame-go/` | Go 1.21+，标准库 + termbox-go | `cd ckgame-go && go run .` |

四个版本共享同一套机制设计：`GameSimulation` 主循环（按日推进、月度结算）+ `GameAPI` 统一接口（snapshot/action）+ TUI 命令行文字界面，存档均为 JSON（`saves/` 目录）。

---

## 文档

统一文档位于 [docs/](docs/README.md)，按版本分子目录：

- [docs/python/](docs/python/README.md) — Python 版
- [docs/java/](docs/java/README.md) — Java 版
- [docs/typescript/](docs/typescript/README.md) — TypeScript 版
- [docs/go/](docs/go/README.md) — Go 版

每版包含：引擎架构、游戏机制、UI 与接口、数据格式、开发者指南。

---

## 已实现系统

| 模块 | 内容 |
|------|------|
| 世界 | 人物、王朝、头衔、邻接地图、税收/征召、生育、死亡与继承 |
| 法律 | 继承法（长子/幼子/选举等）、王权等级、性别法 |
| 政治 | 外交（同盟/停战/联姻）、内阁任务、派系、阴谋 |
| 军事 | 征召、行军、战斗、围城、占城同步头衔、战争分数与和约 |
| 事件 | 月度触发与 AI 自动决议 |
| AI | 宣战、结盟、婚姻、宴会、发展、阴谋 |
| 场景 | 14 省、英格兰/诺曼底主要贵族 |

注：各版本功能完成度略有差异。Go 版当前仅完成 `core/` 基础模块与命令行演示入口，核心引擎与 TUI 待实现。详见对应版本文档。

---

## 开发说明

以原型开发为主，接口与数据格式可能随时调整。