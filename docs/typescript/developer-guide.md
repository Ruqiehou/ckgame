# 开发者指南（TypeScript 版）

## 环境

- Node.js 20+
- TypeScript 5.4+

## 安装与运行

```bash
# 安装依赖
npm install

# 构建
npm run build

# 运行
npm start

# 开发模式（编译+运行）
npm run dev
```

## 测试

```bash
npm test
```

（先 `tsc` 编译，再用 Node 内置 test runner 运行 `src/test/` 下的用例）
覆盖：日期算术、1066 场景加载不变量、`GameSimulation` 主循环稳定性。

## 项目配置

- `tsconfig.json`：TypeScript 编译配置
- `package.json`：npm 脚本与依赖

## 代码结构

模块按功能划分：
- `ai/`：AI 行为
- `core/`：核心类型与常量
- `events/`：事件系统
- `game/`：模拟与场景
- `military/`：军事系统
- `politics/`：政治系统
- `ui/`：用户界面
- `world/`：世界数据

## 添加新功能

1. 在对应目录中创建新模块
2. 在 `GameAPI` 中添加 action 处理
3. 在 `GameTUI` 中添加菜单项
4. 运行 `npm run build` 编译

## 存档位置

- 存档目录：`saves/`（项目根目录）
- 自动存档：`saves/autosave.json`

## 调试

```bash
# 启用 TypeScript 调试
node --inspect dist/ui/Main.js
```

## 常见问题

**Q: 如何修改游戏平衡参数？**
A: 修改 `core/balance/Balance.ts` 中的常量

**Q: 如何添加新事件？**
A: 在 `events/` 目录中扩展事件定义，并在 `EventEngine` 中注册

**Q: 为什么某功能是占位实现？**
A: TypeScript 版本为功能对齐进行中，完整功能参考 Python/Java 版