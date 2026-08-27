# 贡献指南

感谢您对 CKGame 项目的关注！我们欢迎各种形式的贡献，包括代码、文档、错误报告、功能建议等。

## 📋 目录

- [行为准则](#行为准则)
- [如何贡献](#如何贡献)
- [开发流程](#开发流程)
- [代码规范](#代码规范)
- [提交规范](#提交规范)
- [问题报告](#问题报告)
- [功能请求](#功能请求)
- [代码审查](#代码审查)
- [发布流程](#发布流程)

---

## 行为准则

### 我们的承诺
为了营造开放和友好的环境，我们承诺：

- 使用包容和友好的语言
- 尊重不同的观点和经验
- 优雅地接受建设性批评
- 关注对社区最有利的事情
- 对其他社区成员表示同理心

### 不可接受的行为
- 使用性化的语言或图像
- 人身攻击或政治攻击
- 公开或私下骚扰
- 未经许可发布他人的私人信息
- 其他不专业或不适当的行为

### 执行
项目维护者有权删除、编辑或拒绝不符合行为准则的评论、提交、代码、维基编辑、问题和其他贡献。

---

## 如何贡献

### 报告 Bug
如果您发现了 Bug，请：

1. 检查 [Issues](https://github.com/your-username/ckgame-ts/issues) 确认问题是否已被报告
2. 创建新的 Issue，使用 Bug 模板
3. 提供详细的错误信息和重现步骤
4. 包含相关的日志和截图

### 提出新功能
如果您想提出新功能：

1. 先在 Discussions 中讨论想法
2. 创建 Feature Request Issue
3. 说明功能的用例和预期效果
4. 考虑提供实现思路或伪代码

### 改进文档
文档改进总是受欢迎的：

1. 直接修改相关文档文件
2. 确保格式正确，链接有效
3. 添加示例代码
4. 更新过时信息

### 贡献代码
代码贡献的步骤：

1. Fork 项目仓库
2. 创建功能分支
3. 进行开发和测试
4. 提交 Pull Request
5. 参与代码审查

---

## 开发流程

### 1. 设置开发环境

```bash
# Fork 仓库并克隆
git clone https://github.com/YOUR_USERNAME/ckgame-ts.git
cd ckgame-ts

# 添加上游仓库
git remote add upstream https://github.com/your-username/ckgame-ts.git

# 安装依赖
npm install

# 验证环境
npm run type-check
npm test
```

### 2. 创建分支

```bash
# 更新主分支
git checkout main
git pull upstream main

# 创建功能分支
git checkout -b feature/your-feature-name
```

**分支命名规范：**
- `feature/` - 新功能
- `fix/` - Bug 修复
- `docs/` - 文档更新
- `refactor/` - 代码重构
- `test/` - 测试相关
- `perf/` - 性能优化

### 3. 开发和测试

```bash
# 开发中实时编译
npm run watch

# 运行测试
npm test

# 类型检查
npm run type-check

# 代码格式化
npm run format

# 代码检查
npm run lint
```

### 4. 提交更改

```bash
# 查看更改
git status
git diff

# 暂存文件
git add .

# 提交更改
git commit -m "feat: add your commit message"
```

### 5. 推送和创建 PR

```bash
# 推送到你的 Fork
git push origin feature/your-feature-name

# 在 GitHub 上创建 Pull Request
```

---

## 代码规范

### TypeScript 规范

#### 类型定义
```typescript
// ✅ 好的做法
interface Character {
  id: number;
  name: string;
  attributes: AttributeSet;
}

// ❌ 不好的做法
interface Character {
  id: number;
  name: any;
  attributes: any;
}
```

#### 类设计
```typescript
// ✅ 好的做法
class GameEngine {
  private config: GameConfig;
  public currentState: GameState;

  constructor(config: GameConfig) {
    this.config = config;
    this.currentState = new GameState();
  }

  public initialize(): void {
    // 初始化逻辑
  }
}

// ❌ 不好的做法
class GameEngine {
  config; // 缺少类型和访问修饰符
  currentState;

  constructor(config) {
    this.config = config;
  }
}
```

#### 错误处理
```typescript
// ✅ 好的做法
try {
  const result = await loadGameData();
  return result;
} catch (error) {
  if (error instanceof NetworkError) {
    throw new GameError('Failed to load data', 'LOAD_ERROR', error);
  }
  throw error;
}

// ❌ 不好的做法
try {
  const result = await loadGameData();
  return result;
} catch (error) {
  console.error(error); // 只记录日志，不处理错误
  return null; // 吞掉错误
}
```

### 命名规范

#### 文件命名
```
src/core/
  ├── Constants.ts          ✅ PascalCase
  ├── gameDate.ts          ❌ 应该是 GameDate.ts
  └── balance.ts           ✅ camelCase

tests/
  ├── Character.test.ts    ✅ *.test.ts
  └── character.spec.ts    ❌ 不一致的命名
```

#### 变量和函数命名
```typescript
// ✅ 正确的命名
const maxArmySize = 10000;
const playerGold = 500;

function calculateIncome(character: Character): number {
  // 计算收入
}

function processEvent(event: EventInstance): void {
  // 处理事件
}

// ❌ 不好的命名
const MaxArmySize = 10000;
const player_gold = 500;

function CalculateIncome(c: Character): number { }

function ProcessEvent(e: EventInstance): void { }
```

#### 类和接口命名
```typescript
// ✅ 正确的命名
class GameEngine { }
interface GameState { }
enum Gender { }
type CharacterData = Character | null;

// ❌ 不好的命名
class gameEngine { }
interface game_state { }
enum gender { }
type character_data = Character | null;
```

### 注释规范

#### JSDoc 注释
```typescript
/**
 * 计算角色的总收入
 * @param character - 要计算收入的角色
 * @param currentDate - 当前游戏日期
 * @returns 角色的月收入
 * @throws GameError 如果角色不存在或数据无效
 * 
 * @example
 * ```typescript
 * const income = calculateMonthlyIncome(character, currentDate);
 * console.log(`角色 ${character.name} 的收入: ${income} 金币`);
 * ```
 */
function calculateMonthlyIncome(character: Character, currentDate: GameDate): number {
  // 实现逻辑
}
```

#### 行内注释
```typescript
// 计算基础收入：基于领地发展度
let baseIncome = character.titles.reduce((sum, title) => {
  return sum + (title.county?.development || 0) * 0.1;
}, 0);

// TODO: 添加贸易收入计算
// FIXME: 当前计算有偏差，需要修正
// NOTE: 这个算法性能较差，建议优化
baseIncome *= 1.2; // 王国收入加成
```

### 测试规范

#### 单元测试
```typescript
describe('Character', () => {
  describe('constructor', () => {
    it('should create a character with correct properties', () => {
      const character = new Character(1, 'Test Character', testDynasty, Gender.MALE);
      
      expect(character.id).toBe(1);
      expect(character.name).toBe('Test Character');
      expect(character.dynasty).toBe(testDynasty);
      expect(character.gender).toBe(Gender.MALE);
    });

    it('should throw error if dynasty is null', () => {
      expect(() => {
        new Character(1, 'Test', null, Gender.MALE);
      }).toThrow('Dynasty cannot be null');
    });
  });

  describe('age calculation', () => {
    it('should calculate age correctly', () => {
      const birthDate = new GameDate(1000, 1, 1);
      const currentDate = new GameDate(1050, 1, 1);
      const character = new Character(1, 'Test', testDynasty, Gender.MALE, birthDate);
      
      expect(character.getAge(currentDate)).toBe(50);
    });
  });
});
```

#### 集成测试
```typescript
describe('GameSimulation Integration', () => {
  let simulation: GameSimulation;

  beforeEach(() => {
    simulation = new GameSimulation();
    simulation.initialize('1066');
  });

  it('should simulate a full game year correctly', () => {
    const initialDate = simulation.getCurrentDate();
    const initialCharacterCount = simulation.getWorld().characters.size;

    // 模拟一年
    for (let month = 0; month < 12; month++) {
      simulation.advanceMonth();
    }

    const finalDate = simulation.getCurrentDate();
    expect(finalDate.year).toBe(initialDate.year + 1);
    
    // 检查角色数量变化（可能有出生和死亡）
    const finalCharacterCount = simulation.getWorld().characters.size;
    expect(finalCharacterCount).toBeGreaterThan(0);
  });
});
```

---

## 提交规范

### Commit Message 格式

我们使用 [Conventional Commits](https://www.conventionalcommits.org/) 规范：

```
<type>(<scope>): <subject>

<body>

<footer>
```

#### 类型 (type)
- `feat`: 新功能
- `fix`: Bug 修复
- `docs`: 文档更新
- `style`: 代码格式（不影响代码功能）
- `refactor`: 重构（既不是新功能也不是 Bug 修复）
- `perf`: 性能优化
- `test`: 测试相关
- `chore`: 构建过程或辅助工具的变动
- `ci`: CI 配置文件和脚本的变动

#### 范围 (scope)
范围可以是以下模块之一：
- `core`: 核心功能
- `world`: 世界系统
- `military`: 军事系统
- `politics`: 政治系统
- `events`: 事件系统
- `ai`: AI 系统
- `game`: 游戏逻辑
- `ui`: 用户界面

#### 示例

```bash
# 功能添加
feat(world): add marriage system
Implement marriage mechanics between characters including
wedding ceremonies and divorce proceedings.

Closes #123

# Bug 修复
fix(military): correct army supply calculation
Fix issue where army supply was not decreasing properly
during long sieges. Also improve supply consumption formula.

Fixes #456

# 文档更新
docs: update API documentation for Character class
Add missing method documentation and update examples.

# 重构
refactor(core): simplify GameDate arithmetic
Reduce complexity of date calculation methods and
improve performance.

# 测试
test(military): add comprehensive battle simulation tests
Cover various battle scenarios including army size
differences and terrain bonuses.
```

### Commit Message 最佳实践

```bash
# ✅ 好的提交信息
feat(world): implement dynasty prestige system
Add dynasty prestige mechanics that affect character
relationships and succession calculations.

- Dynasty prestige accumulates over time
- Prestige affects marriage proposals
- High prestige dynasties get better marriage matches

Closes #789

# ❌ 不好的提交信息
fix stuff
update code
wip
```

---

## 问题报告

### Bug 报告模板

```markdown
**描述**
清晰简洁地描述 Bug。

**重现步骤**
1. 进入 '...'
2. 点击 '....'
3. 滚动到 '....'
4. 看到错误

**预期行为**
描述你期望发生的事情。

**实际行为**
描述实际发生的事情。

**截图**
如果适用，添加截图来帮助解释问题。

**环境信息**
- OS: [e.g. Windows 10, macOS 12.5, Ubuntu 22.04]
- Node.js 版本: [e.g. 18.15.0]
- 项目版本: [e.g. v1.0.0]

**附加信息**
添加任何其他关于问题的信息。
```

### 功能请求模板

```markdown
**功能描述**
清晰简洁地描述你想要的功能。

**问题背景**
你遇到的问题是什么？这个功能会帮助解决什么问题？

**建议的解决方案**
你希望这个功能如何工作？

**替代方案**
考虑过或描述过的替代解决方案。

**附加信息**
添加任何其他关于功能请求的信息。
```

---

## 代码审查

### Pull Request 模板

```markdown
**描述**
简要描述这个 PR 的内容。

**更改类型**
- [ ] Bug 修复
- [ ] 新功能
- [ ] 代码重构
- [ ] 文档更新
- [ ] 性能优化
- [ ] 测试更新

**测试**
- [ ] 添加了新测试
- [ ] 更新了现有测试
- [ ] 所有测试通过

**检查清单**
- [ ] 代码遵循项目风格指南
- [ ] 添加了必要的注释
- [ ] 更新了相关文档
- [ ] 没有引入新的警告
- [ ] 提交信息符合规范

**相关问题**
Closes #(issue number)
Related to #(issue number)
```

### 审查流程

1. **自动检查**: CI/CD 自动运行测试和代码检查
2. **人工审查**: 至少一名维护者审查代码
3. **反馈处理**: 根据反馈进行修改
4. **批准合并**: 所有审查通过后合并

### 审查要点

- 代码质量和可读性
- 测试覆盖率和测试质量
- 文档完整性
- 性能影响
- 向后兼容性
- 安全考虑

---

## 发布流程

### 版本号规范

我们使用 [语义化版本](https://semver.org/)：

- `MAJOR.MINOR.PATCH`
- `MAJOR`: 不兼容的 API 变更
- `MINOR`: 向后兼容的功能新增
- `PATCH`: 向后兼容的 Bug 修复

### 发布步骤

1. **准备发布**
```bash
# 更新版本号
npm version patch|minor|major

# 创建发布分支
git checkout -b release/v1.0.1
```

2. **更新文档**
```bash
# 更新 CHANGELOG.md
nano CHANGELOG.md

# 更新版本信息
nano package.json
```

3. **测试和构建**
```bash
# 运行完整测试套件
npm test

# 构建生产版本
npm run build

# 创建发布标签
git tag -a v1.0.1 -m "Release version 1.0.1"
git push origin v1.0.1
```

4. **发布到 npm**
```bash
# 发布到 npm
npm publish
```

5. **更新主分支**
```bash
# 合并到主分支
git checkout main
git merge release/v1.0.1

# 推送到上游
git push upstream main
```

### 发布检查清单

- [ ] 所有测试通过
- [ ] 文档已更新
- [ ] CHANGELOG 已更新
- [ ] 版本号已更新
- [ ] 发布笔记已准备
- [ ] 已在测试环境验证
- [ ] 已创建 GitHub Release
- [ ] 已推送到 npm

---

## 贡献者指南

### 成为贡献者

1. **首次贡献**: 从简单的问题开始
2. **社区参与**: 加入 Discussions 和评论
3. **持续贡献**: 定期参与项目
4. **成为维护者**: 表现出色者可成为维护者

### 贡献者权益

- 在贡献者列表中显示
- 参与重要决策讨论
- 访问开发者工具
- 项目周边产品优先权

### 贡献统计

贡献包括：
- 代码提交
- 问题报告
- 文档改进
- 代码审查
- 社区支持

---

## 社区参与

### 讨论区
- [GitHub Discussions](https://github.com/your-username/ckgame-ts/discussions)
- [Discord 服务器](https://discord.gg/ckgame)
- [Reddit 社区](https://reddit.com/r/ckgame)

### 社交媒体
- [Twitter](https://twitter.com/ckgame)
- [Facebook](https://facebook.com/ckgame)

### 会议和活动
- 定期的社区会议
- 代码审查会议
- 功能规划会议

---

## 获得帮助

### 常见问题
查看 [FAQ](docs/FAQ.md) 获取常见问题解答。

### 技术支持
- 📧 邮件: support@ckgame.com
- 💬 Discord: [CKGame 社区](https://discord.gg/ckgame)
- 📖 Wiki: [项目 Wiki](https://github.com/your-username/ckgame-ts/wiki)

### 开发者资源
- [开发指南](development.md)
- [API 文档](api.md)
- [部署文档](deployment.md)

---

## 许可证

通过向 CKGame 项目贡献代码，您同意您的贡献将根据项目的 MIT 许可证进行许可。

---

## 致谢

感谢所有为 CKGame 项目做出贡献的开发者！

**主要贡献者**:
- [@username1](https://github.com/username1) - 创始人和主要开发者
- [@username2](https://github.com/username2) - AI 系统开发
- [@username3](https://github.com/username3) - UI/UX 设计

**特别感谢**:
- Paradox Interactive 的《十字军之王》系列游戏
- TypeScript 社区的支持
- 所有测试人员和反馈者

---

**开始贡献吧！** 🚀✨