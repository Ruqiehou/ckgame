# CKGame TypeScript 版文档

十字军之王风格大战略模拟引擎 — TypeScript 实现版。

- [引擎架构](engine-architecture.md)：项目结构与核心流程
- [游戏机制](game-mechanics.md)：战争、派系、法律、阴谋等机制
- [UI 与接口](ui-and-api.md)：TUI 文字界面与 GameAPI
- [数据格式](data-format.md)：存档与场景数据格式
- [开发者指南](developer-guide.md)：环境搭建与运行方式

## 快速开始

```bash
# 安装依赖
npm install

# 构建
npm run build

# 运行
npm start
```

## 版本特性

- 完整 TUI 文字界面（GameTUI.ts）
- GameAPI 统一接口（snapshot()/action()）
- TypeScript 编译为 Node.js 运行
- npm scripts 管理（build/start/dev）
- 部分功能为占位实现（建造、授予头衔）