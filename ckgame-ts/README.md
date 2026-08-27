# CKGame - 十字军之王风格大战略游戏引擎

[![TypeScript](https://img.shields.io/badge/TypeScript-5.0-blue.svg)](https://www.typescriptlang.org/)
[![Node.js](https://img.shields.io/badge/Node.js-18%2B-green.svg)](https://nodejs.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

CKGame 是一个基于 TypeScript 开发的十字军之王风格大战略游戏引擎，专注于提供深度的角色扮演、政治军事模拟和王朝管理体验。

## 🎮 特性

### 核心系统
- **角色系统**: 完整的人物属性、特质、关系网络
- **王朝管理**: 血脉继承、家族荣誉、联姻政治
- **领土控制**: 郡、公国、王国、帝国等级制度
- **继承法**: 选举制、长子继承、萨利克法等

### 军事系统
- **军团管理**: 军团组建、移动、补给
- **战斗模拟**: 基于兵种、士气、纪律的真实战斗
- **围城系统**: 城堡围攻、攻城器械、守城策略
- **战争机制**: 宣战理由、和平条约、战利品分配

### 政治外交
- **内阁系统**: 大臣职位、任务分配、权力制衡
- **派系系统**: 支持者、反对者、权力斗争
- **阴谋系统**: 刺杀、政变、间谍活动
- **外交关系**: 联盟、贸易协定、宣战

### 经济系统
- **贸易路线**: 商路保护、贸易协定、经济繁荣
- **建筑升级**: 城堡、市场、农田等建筑管理
- **税收系统**: 基于领地发展的税收计算

### 事件系统
- **剧情线**: 主要历史事件和剧情发展
- **随机事件**: 基于人物特质和环境的动态事件
- **选择分支**: 玩家决策影响世界发展

### AI 系统
- **性格驱动**: 基于人物特质的决策系统
- **行为树**: 复杂的AI决策逻辑
- **月度决策**: AI每月进行政治、军事、经济决策

## 🚀 快速开始

### 环境要求
- Node.js 18+
- npm 或 yarn

### 安装
```bash
git clone https://github.com/your-username/ckgame-ts.git
cd ckgame-ts
npm install
```

### 运行
```bash
# 开发模式
npm run dev

# 构建生产版本
npm run build

# 运行游戏
npm start
```

### 项目结构
```
ckgame-ts/
├── src/                    # 源代码
│   ├── core/              # 核心类型和常量
│   ├── world/             # 世界系统
│   ├── military/          # 军事系统
│   ├── politics/          # 政治外交
│   ├── events/            # 事件系统
│   ├── ai/                # AI 系统
│   ├── game/              # 游戏逻辑
│   └── ui/                # 用户界面
├── data/                  # 游戏数据
│   └── scenarios/         # 场景配置
├── dist/                  # 编译输出
├── docs/                  # 文档
└── tests/                 # 测试文件
```

## 📖 场景

### 1066 英格兰
默认场景为1066年诺曼征服时期的英格兰，包含：
- 威廉一世、哈罗德二世、哈拉尔三世等历史人物
- 诺曼底、英格兰、挪威等主要势力
- 完整的领土划分和关系网络

## 🔧 开发

### 构建命令
```bash
# 开发构建（监听模式）
npm run watch

# 生产构建
npm run build

# 类型检查
npm run type-check

# 代码格式化
npm run format
```

### 测试
```bash
# 运行所有测试
npm test

# 运行特定测试
npm test -- --grep "Character"

# 测试覆盖率
npm run test:coverage
```

## 📚 文档

- [开发指南](docs/development.md) - 开发环境设置和贡献指南
- [API 文档](docs/api.md) - 完整的 API 参考
- [部署文档](docs/deployment.md) - 部署和配置说明
- [贡献指南](docs/CONTRIBUTING.md) - 如何贡献代码

## 🤝 贡献

欢迎贡献代码！请阅读 [贡献指南](docs/CONTRIBUTING.md) 了解如何参与项目开发。

## 📄 许可证

MIT License - 详见 [LICENSE](LICENSE) 文件。

## 🙏 致谢

- Paradox Interactive 的《十字军之王》系列游戏
- TypeScript 社区的强大支持
- 所有贡献者的努力

---

**开始你的大战略之旅！** 🏰⚔️👑