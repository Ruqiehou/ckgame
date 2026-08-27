# CKGame C++ 版文档

十字军之王风格大战略模拟引擎 — C++ 实现版。

- [引擎架构](engine-architecture.md)：项目结构与核心流程
- [游戏机制](game-mechanics.md)：战争、派系、法律、阴谋等机制
- [UI 与接口](ui-and-api.md)：TUI 文字界面与 GameAPI
- [数据格式](data-format.md)：存档与场景数据格式
- [开发者指南](developer-guide.md)：环境搭建与运行方式

## 快速开始

```bash
cmake -S . -B build
cmake --build build --config Release
```

## 版本特性

- C++17 标准库实现，无第三方依赖
- CMake 构建（支持 MSVC / GCC / Clang）
- `/utf-8` 编译选项保证 Windows 中文输出
- 当前已完成 `core/` 基础模块与演示入口
- 命名空间：`ckgame`（子命名空间 `calendar`/`stats`/`traits`/`balance`/`ui`）