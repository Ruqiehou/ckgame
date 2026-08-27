# 开发者指南（Python 版）

## 环境

- Python 3.12+
- 依赖：标准库 + pytest

## 运行

```bash
python -m ck_engine.game.tui
```

## 测试

```bash
python -m pytest tests/ -v
```

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
A: 在 `events/storylines.py` 中扩展剧情定义
