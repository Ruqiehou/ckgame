# CKGame 文档

十字军之王风格大战略模拟引擎 — 五个语言实现的统一文档，按版本分子目录存放。

## 版本文档目录

| 版本 | 目录 | 说明 |
|------|------|------|
| Python | [python/](python/README.md) | 标准库实现，PyInstaller 打包 |
| Java | [java/](java/README.md) | Maven 构建 + Jackson，Java 17 |
| TypeScript | [typescript/](typescript/README.md) | tsc 编译为 Node.js 运行 |
| Go | [go/](go/README.md) | 标准库，termbox-go TUI，Go 1.21 |
| C++ | [cpp/](cpp/README.md) | C++17 + CMake，无第三方依赖 |

每个版本目录下包含统一结构的文档：

- [引擎架构](python/engine-architecture.md)：项目结构与核心流程
- [游戏机制](python/game-mechanics.md)：战争、派系、法律、阴谋等机制
- [UI 与接口](python/ui-and-api.md)：TUI 文字界面与 GameAPI
- [数据格式](python/data-format.md)：存档与场景数据格式
- [开发者指南](python/developer-guide.md)：环境搭建与运行方式

## 通用设计

五个版本共享同一套机制设计：

- **架构**：`GameSimulation` 主循环（按日推进、月度结算）+ `GameAPI` 统一接口（snapshot/action）
- **界面**：均为 TUI 命令行文字对话模式
- **场景**：1066 年诺曼征服模拟器
- **存档**：JSON 格式，存储于 `saves/` 目录