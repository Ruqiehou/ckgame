# 开发指南

本文档提供 CKGame 项目的开发环境设置、代码规范和最佳实践。

## 🛠️ 开发环境设置

### 前置要求
- Node.js 18.0 或更高版本
- npm 9.0 或更高版本（或 yarn）
- Git
- 代码编辑器（推荐 VS Code）

### 安装步骤

1. **克隆仓库**
```bash
git clone https://github.com/your-username/ckgame-ts.git
cd ckgame-ts
```

2. **安装依赖**
```bash
npm install
```

3. **验证安装**
```bash
npm run type-check
npm run build
```

### 推荐的 VS Code 扩展
- ESLint - 代码质量检查
- Prettier - 代码格式化
- TypeScript Vue Plugin (Volar) - TypeScript 支持
- GitLens - Git 增强功能
- TODO Highlight - 高亮显示 TODO 注释

## 📂 项目结构详解

```
ckgame-ts/
├── src/                    # 源代码目录
│   ├── core/              # 核心类型和常量
│   │   ├── balance/       # 游戏平衡常数
│   │   ├── calendar/      # 日期和季节系统
│   │   ├── stats/         # 属性系统
│   │   └── traits/        # 特质系统
│   ├── world/             # 世界系统
│   │   ├── buildings/     # 建筑系统
│   │   ├── Character.ts   # 角色类
│   │   ├── County.ts      # 郡县类
│   │   ├── Dynasty.ts     # 王朝类
│   │   ├── Title.ts       # 头衔类
│   │   ├── World.ts       # 世界状态类
│   │   └── ...
│   ├── military/          # 军事系统
│   │   ├── Army.ts        # 军团类
│   │   ├── BattleSimulator.ts # 战斗模拟
│   │   ├── Siege.ts       # 围城系统
│   │   ├── War.ts         # 战争系统
│   │   └── ...
│   ├── politics/          # 政治外交
│   │   ├── Council.ts     # 内阁系统
│   │   ├── Diplomacy.ts   # 外交关系
│   │   ├── Faction.ts     # 派系系统
│   │   ├── Scheme.ts      # 阴谋系统
│   │   └── ...
│   ├── events/            # 事件系统
│   │   ├── EventEngine.ts # 事件引擎
│   │   ├── EventDef.ts    # 事件定义
│   │   ├── Storyline.ts   # 剧情线
│   │   └── ...
│   ├── ai/                # AI 系统
│   │   ├── AiDirector.ts  # AI 决策器
│   │   ├── AiPersonality.ts # AI 性格
│   │   └── ...
│   ├── game/              # 游戏逻辑
│   │   ├── GameSimulation.ts # 游戏模拟
│   │   ├── Scenario1066.ts # 1066 场景
│   │   ├── ScenarioLoader.ts # 场景加载器
│   │   └── ...
│   └── ui/                # 用户界面
│       ├── GameAPI.ts     # 游戏 API
│       ├── GameTUI.ts     # 文字界面
│       └── Main.ts        # 主程序
├── data/                  # 游戏数据
│   └── scenarios/         # 场景配置
│       └── 1066.json      # 1066 英格兰场景
├── dist/                  # 编译输出目录
├── docs/                  # 文档目录
├── tests/                 # 测试文件
├── package.json           # 项目配置
├── tsconfig.json          # TypeScript 配置
└── README.md             # 项目说明
```

## 🎯 开发工作流

### 1. 创建新功能
```bash
# 创建功能分支
git checkout -b feature/your-feature-name

# 进行开发
# ... 编写代码 ...

# 运行类型检查
npm run type-check

# 运行测试
npm test

# 格式化代码
npm run format

# 提交代码
git add .
git commit -m "feat: add your feature description"
git push origin feature/your-feature-name
```

### 2. 修复 Bug
```bash
# 创建修复分支
git checkout -b fix/bug-description

# 修复问题
# ... 编写代码 ...

# 运行测试确保修复有效
npm test

# 提交修复
git add .
git commit -m "fix: describe the bug fix"
git push origin fix/bug-description
```

### 3. 代码审查
- 所有代码变更需要通过 Pull Request 提交
- 至少需要一名维护者审查
- 所有检查必须通过（类型检查、测试、代码格式化）

## 📝 代码规范

### TypeScript 规范

1. **类型定义**
```typescript
// ✅ 好的做法 - 明确的类型定义
interface Character {
  id: number;
  name: string;
  age: number;
  gender: Gender;
  traits: Trait[];
}

// ❌ 不好的做法 - 使用 any
interface Character {
  id: number;
  name: any;
  age: any;
}
```

2. **类设计**
```typescript
// ✅ 好的做法 - 使用访问修饰符
class Character {
  private _id: number;
  public name: string;
  protected dynasty: Dynasty;

  constructor(id: number, name: string, dynasty: Dynasty) {
    this._id = id;
    this.name = name;
    this.dynasty = dynasty;
  }

  get id(): number {
    return this._id;
  }
}
```

3. **错误处理**
```typescript
// ✅ 好的做法 - 明确的错误类型
class GameError extends Error {
  constructor(message: string, public code: string) {
    super(message);
    this.name = 'GameError';
  }
}

try {
  // 游戏逻辑
} catch (error) {
  if (error instanceof GameError) {
    console.error(`Game Error [${error.code}]: ${error.message}`);
  } else {
    console.error('Unexpected error:', error);
  }
}
```

4. **异步编程**
```typescript
// ✅ 好的做法 - 使用 async/await
async function loadScenario(scenarioId: string): Promise<Scenario> {
  try {
    const data = await fetchScenarioData(scenarioId);
    return parseScenario(data);
  } catch (error) {
    throw new GameError(`Failed to load scenario: ${scenarioId}`, 'SCENARIO_LOAD_ERROR');
  }
}
```

### 命名规范

1. **文件命名**
- TypeScript 文件: `PascalCase.ts` (如 `Character.ts`)
- 测试文件: `*.test.ts` (如 `Character.test.ts`)

2. **变量和函数命名**
```typescript
// 变量和函数使用 camelCase
const characterCount: number = 100;
function calculateIncome(character: Character): number { ... }

// 类和接口使用 PascalCase
class GameEngine { }
interface GameState { }

// 常量使用 UPPER_SNAKE_CASE
const MAX_ARMY_SIZE: number = 10000;
const DEFAULT_STARTING_GOLD: number = 500;
```

### 注释规范

```typescript
/**
 * 角色类，表示游戏中的每个角色
 * 包含属性、特质、关系等信息
 */
class Character {
  /**
   * 创建新角色
   * @param id - 角色唯一标识
   * @param name - 角色名称
   * @param dynasty - 所属王朝
   * @param gender - 性别
   * @returns 创建的角色对象
   */
  constructor(
    id: number,
    name: string,
    dynasty: Dynasty,
    gender: Gender
  ) {
    // 初始化代码
  }

  // TODO: 添加更多角色属性
  // FIXME: 修复年龄计算问题
  // NOTE: 这个方法需要重构
}
```

## 🧪 测试

### 单元测试
```typescript
// Character.test.ts
import { Character } from './Character';

describe('Character', () => {
  describe('constructor', () => {
    it('should create a character with correct properties', () => {
      const character = new Character(1, 'Test Character', testDynasty, Gender.MALE);
      expect(character.id).toBe(1);
      expect(character.name).toBe('Test Character');
    });
  });

  describe('age', () => {
    it('should calculate age correctly based on birth year', () => {
      const birthDate = new GameDate(1000, 1, 1);
      const currentDate = new GameDate(1050, 1, 1);
      const character = new Character(1, 'Test', testDynasty, Gender.MALE, birthDate);
      
      expect(character.getAge(currentDate)).toBe(50);
    });
  });
});
```

### 集成测试
```typescript
// GameSimulation.test.ts
import { GameSimulation } from './GameSimulation';

describe('GameSimulation', () => {
  let simulation: GameSimulation;

  beforeEach(() => {
    simulation = new GameSimulation();
  });

  it('should initialize with 1066 scenario', () => {
    simulation.initialize('1066');
    expect(simulation.getWorld().characters.size).toBeGreaterThan(0);
  });

  it('should advance time correctly', () => {
    simulation.initialize('1066');
    const initialDate = simulation.getCurrentDate();
    
    simulation.advanceDay();
    const newDate = simulation.getCurrentDate();
    
    expect(newDate.isAfter(initialDate)).toBe(true);
  });
});
```

## 🔄 持续集成

项目使用 GitHub Actions 进行持续集成：

### 工作流程
- **类型检查**: 每次提交都运行 `npm run type-check`
- **代码格式化**: 检查代码格式是否符合规范
- **单元测试**: 运行所有单元测试
- **构建测试**: 确保项目可以成功构建

### 分支保护
- `main` 分支需要 Pull Request
- 至少需要一次审查批准
- 所有 CI 检查必须通过

## 🚀 性能优化

### 1. 内存管理
```typescript
// 避免内存泄漏
class GameEngine {
  private timers: NodeJS.Timeout[] = [];

  setGameLoop(callback: () => void, interval: number): void {
    const timer = setInterval(callback, interval);
    this.timers.push(timer);
  }

  cleanup(): void {
    this.timers.forEach(timer => clearInterval(timer));
    this.timers = [];
  }
}
```

### 2. 计算优化
```typescript
// 使用缓存避免重复计算
class World {
  private characterCache = new Map<number, Character>();

  getCharacter(id: number): Character {
    if (!this.characterCache.has(id)) {
      const character = this.loadCharacter(id);
      this.characterCache.set(id, character);
    }
    return this.characterCache.get(id)!;
  }

  clearCache(): void {
    this.characterCache.clear();
  }
}
```

### 3. 异步加载
```typescript
// 异步加载大型数据集
async function loadLargeDataset(): Promise<DataSet> {
  // 使用 Web Worker 处理大数据
  const worker = new Worker('./data-processor.js');
  return new Promise((resolve, reject) => {
    worker.onmessage = (e) => resolve(e.data);
    worker.onerror = (e) => reject(e.error);
    worker.postMessage({ action: 'load' });
  });
}
```

## 🐛 调试技巧

### 1. 使用 TypeScript 类型系统
```typescript
// 使用类型守卫进行调试
function isCharacter(obj: any): obj is Character {
  return obj && typeof obj.id === 'number' && typeof obj.name === 'string';
}

if (isCharacter(someObject)) {
  // TypeScript 现在知道 someObject 是 Character 类型
  console.log(someObject.name);
}
```

### 2. 日志记录
```typescript
class Logger {
  private static instance: Logger;
  private logs: string[] = [];

  static getInstance(): Logger {
    if (!Logger.instance) {
      Logger.instance = new Logger();
    }
    return Logger.instance;
  }

  log(message: string, level: 'info' | 'warn' | 'error' = 'info'): void {
    const timestamp = new Date().toISOString();
    const logEntry = `[${timestamp}] [${level.toUpperCase()}] ${message}`;
    this.logs.push(logEntry);
    
    if (level === 'error') {
      console.error(logEntry);
    } else {
      console.log(logEntry);
    }
  }

  getLogs(): string[] {
    return [...this.logs];
  }
}
```

## 📚 资源链接

- [TypeScript 官方文档](https://www.typescriptlang.org/docs/)
- [Node.js 文档](https://nodejs.org/docs/)
- [ESLint 规则](https://eslint.org/docs/rules/)
- [Prettier 配置](https://prettier.io/docs/en/options.html)

## 🤝 获取帮助

- 📧 邮件: support@ckgame.com
- 💬 Discord: [CKGame 社区](https://discord.gg/ckgame)
- 📖 Wiki: [项目 Wiki](https://github.com/your-username/ckgame-ts/wiki)
- 🐛 问题反馈: [GitHub Issues](https://github.com/your-username/ckgame-ts/issues)