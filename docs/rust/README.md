# CKGame Rust 版文档

十字军之王风格大战略模拟引擎 — Rust 实现版。

- [引擎架构](engine-architecture.md)：项目结构与核心流程
- [游戏机制](game-mechanics.md)：战争、派系、法律、阴谋等机制
- [UI 与接口](ui-and-api.md)：TUI 文字界面与 GameAPI
- [数据格式](data-format.md)：存档与场景数据格式
- [开发者指南](developer-guide.md)：环境搭建与运行方式

## 快速开始

```bash
cargo build --release
cargo run --release
```

## 版本特性

- Rust 2021 Edition
- 使用 `chrono` 进行日期计算
- 使用 `serde` + `serde_json` 进行 JSON 序列化
- `termion` 用于 TUI 界面（基础演示可用）
- 模块化设计：`core/`（核心）、`ui/`（界面）
- 当前已完成 `core/` 基础模块与演示入口
- 主入口：`src/main.rs`