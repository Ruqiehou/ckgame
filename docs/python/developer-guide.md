# 开发者指南（Python 版）

## 环境

- Python 3.10+
- 运行依赖：仅 Python 标准库
- 可选开发依赖：`pytest`（仅用于习惯 pytest 的运行方式）

## 运行

```bash
python -m ck_engine.game.tui
```

启动 WebUI：

```bash
python -m ck_engine.web.server
# 浏览器访问 http://127.0.0.1:8000
```

可通过参数指定监听地址、端口和场景：

```bash
python -m ck_engine.web.server --host 127.0.0.1 --port 8000 --scenario 867
```

## 测试

```bash
python -m unittest discover -s tests -v
```

如果已安装 pytest，也可以运行：

```bash
python -m pytest tests/ -v
```

测试覆盖：场景加载与构建不变量、主循环稳定性、GameAPI 操作边界、事件与决策、玩法、存档读档、派系最后通牒，以及 WebUI HTTP 路由。

## 打包

使用 PyInstaller 打包为独立可执行文件：

```bash
pyinstaller --name CKGameTUI --onefile ck_engine/game/tui.py
```

## 添加新功能

1. 在对应包中创建新模块，并保持现有数据模型和错误处理风格
2. 如果需要用户操作，在 `GameAPI` 中添加 action 处理
3. 在 `GameTUI` 或 WebUI 中接入操作和状态展示
4. 为正常路径、失败路径和存档往返补充测试
5. 更新相关文档与场景数据说明

## 常见问题

**Q: 如何修改游戏平衡参数？**
A: 修改 `core/balance.py` 中的常量

**Q: 如何添加新事件？**
A: 在 `events/engine.py` 的 `builtin_events()` 中新增 `EventDef`（含标题、权重、选项与效果）；多阶段剧情请扩展 `events/event_chains.py`（事件链）或 `events/storylines.py`（剧情线）
