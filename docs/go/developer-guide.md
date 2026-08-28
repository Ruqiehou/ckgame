# 开发者指南（Go 版）

## 环境

- Go 1.21+

## 安装依赖

```bash
go mod download
```

## 运行

```bash
# 运行
go run .

# 编译
go build -o ckgame-go .

# 安装到系统
go install .
```

## 测试

```bash
go test ./...
```

覆盖：`core/calendar` 日期算术（跨月/闰月/季节/年龄）、`world` 场景加载（授衔/婚姻/宗主链、真实 1066.json 回归）。

## 项目结构

包结构按功能划分：
- `core/`：核心类型与常量
- `ai/`：AI 行为
- `events/`：事件系统
- `game/`：模拟与场景
- `military/`：军事系统
- `politics/`：政治系统
- `ui/`：用户界面
- `world/`：世界数据

## 添加新功能

1. 在对应包中创建新文件
2. 在 `GameAPI` 中添加 action 处理
3. 在 `GameTUI` 中添加菜单项
4. 运行 `go build` 编译

## 存档位置

- 存档目录：`saves/`（项目根目录）
- 自动存档：`saves/autosave.json`

## 常见问题

**Q: 如何修改游戏平衡参数？**
A: 修改 `core/balance/balance.go` 中的常量

**Q: 如何添加新事件？**
A: 在 `events/` 包中扩展事件定义，并在 `EventEngine` 中注册

**Q: 界面如何适配不同终端尺寸？**
A: 使用 `go-runewidth` 计算每行显示宽度，`termbox-go` 处理渲染