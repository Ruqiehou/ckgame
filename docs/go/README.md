# CKGame Go 版文档

十字军之王风格大战略模拟引擎 — Go 实现版。

- [引擎架构](engine-architecture.md)：项目结构与核心流程
- [游戏机制](game-mechanics.md)：战争、派系、法律、阴谋等机制
- [UI 与接口](ui-and-api.md)：TUI 文字界面与 GameAPI
- [数据格式](data-format.md)：存档与场景数据格式
- [开发者指南](developer-guide.md)：环境搭建与运行方式

## 快速开始

```bash
go run .
```

## 版本特性

- 标准库实现，无运行时依赖
- 主入口：`main.go`，命令行演示（加载 1066 场景、打印世界概况、AI 集结、推进数月）
- 已实现：`core/` 基础模块（日期、季节、属性、特性、平衡常量），以及 `world/` 场景加载与 `military/`、`politics/`、`events/`、`ai/` 基础模块
- 待实现：GameSimulation 主循环、GameAPI、termbox-go TUI（依赖已在 go.mod 中预留）
- 编译：`go build -o ckgame-go .`