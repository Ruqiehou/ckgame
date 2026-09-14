# 开发者指南（Python 版）

## 环境

- Python 3.10+
- 依赖：标准库 + pytest

## 运行

```bash
python -m ck_engine.game.tui
```

## 测试

```bash
python -m pytest tests/ -v
```

（无 pytest 时可用标准库运行：`python -m unittest discover -s tests -v`）

覆盖：1066 场景构建不变量、主循环稳定性（按日/按月推进）、存档读档往返、派系最后通牒接受/拒绝闭环。

## 打包

使用 PyInstaller 打包为独立可执行文件：

```bash
pyinstaller --name CKGameTUI --onefile ck_engine/game/tui.py
```

## 添加新功能

1. 在对应包中创建新模块
2. 在 `GameAPI` 中添加 action 处理
3. 在 `GameTUI` 中添加菜单项
4. 更新测试与文档

## 常见问题

**Q: 如何修改游戏平衡参数？**
A: 修改 `core/balance.py` 中的常量

**Q: 如何添加新事件？**
A: 在 `events/engine.py` 的 `builtin_events()` 中新增 `EventDef`（含标题、权重、选项与效果）；多阶段剧情请扩展 `events/event_chains.py`（事件链）或 `events/storylines.py`（剧情线）
