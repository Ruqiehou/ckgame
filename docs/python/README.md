# CKGame Python 版文档

十字军之王风格大战略模拟引擎 — Python 实现版。

- [引擎架构](engine-architecture.md)：项目结构与核心流程
- [游戏机制](game-mechanics.md)：战争、派系、法律、阴谋等机制
- [玩法目录](gameplay.md)：玩法系统清单（狩猎、养生）与扩展方式
- [UI 与接口](ui-and-api.md)：TUI 文字界面与 GameAPI
- [数据格式](data-format.md)：存档与场景数据格式
- [开发者指南](developer-guide.md)：环境搭建与运行方式

## 快速开始

```bash
# 运行
python -m ck_engine.game.tui
```

## 版本特性

- 完整 TUI 文字界面（tui.py）
- GameAPI 统一接口（snapshot()/action()）
- 标准库实现，无第三方依赖
- PyInstaller 打包支持（CKGameTUI）
