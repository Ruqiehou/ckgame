# CK-Style Grand Strategy Engine

Python 实现的十字军之王风格大战略模拟引擎。  
默认场景：**1066 英格兰**（诺曼征服前夜）。

作者：Ruqiehou  
更新：2026-08-18

---

## 运行环境

- Python 3.10+
- 标准库即可（无第三方依赖）

---

## 快速开始

```bash
python -m ck_engine.game.tui
```

启动后输入 `help` 查看可用指令。

---

## 项目结构

```text
ck_engine/
├── core/                # 日期、属性、特质
├── world/               # 人物、地图、头衔、王朝、经济/继承
├── politics/            # 外交、内阁、派系、法律、阴谋
├── military/            # 军团、战斗、围城、战争
├── events/              # 事件引擎
├── ai/                  # AI 性格与月度决策
├── game/
│   ├── tui.py           # 命令行文字对话界面
│   ├── simulation.py    # 游戏主循环
│   ├── scenario.py      # 场景定义
│   └── scenario_loader.py
└── ui/
    └── api.py           # 玩家 API（状态与指令）
```

---

## 已实现系统

| 模块 | 内容 |
|------|------|
| 世界 | 人物、王朝、头衔、邻接地图、税收/征召、生育、死亡与继承 |
| 法律 | 继承法 `pick_heir`（长子/幼子/长老等）、王权税加成 |
| 政治 | 外交（同盟禁战/参战、停战、宣称）、内阁、派系、阴谋 |
| 军事 | 征召、行军、战斗、围城、占城同步头衔、战争分数与和约 |
| 事件 | 月度触发与 AI 自动决议 |
| AI | 宣战、结盟、婚姻、宴会、发展、阴谋 |
| 场景 | 14 省、英格兰/诺曼底主要贵族 |
| 存档 | `saves/autosave.json` 轻量快照 |

---

## 开发说明

- 主循环：`ck_engine/game/simulation.py`
- 场景：`ck_engine/game/scenario.py`
- 玩家 API：`ck_engine/ui/api.py`

本仓库以原型开发为主，接口与数据格式可能随时调整。
