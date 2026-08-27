# CKGame API 文档

本文档提供 CKGame 引擎的完整 API 参考。

## 📖 目录

- [核心 API](#核心-api)
- [世界系统 API](#世界系统-api)
- [军事系统 API](#军事系统-api)
- [政治系统 API](#政治系统-api)
- [事件系统 API](#事件系统-api)
- [AI 系统 API](#ai-系统-api)
- [游戏引擎 API](#游戏引擎-api)
- [用户界面 API](#用户界面-api)

---

## 核心 API

### GameDate
日期和日历系统。

```typescript
class GameDate {
  year: number;
  month: number;
  day: number;

  constructor(year: number, month: number, day: number);
  
  // 日期比较
  equals(other: GameDate): boolean;
  isAfter(other: GameDate): boolean;
  isBefore(other: GameDate): boolean;
  
  // 日期操作
  addDays(days: number): GameDate;
  addMonths(months: number): GameDate;
  addYears(years: number): GameDate;
  
  // 格式化
  toString(): string;
  toISODate(): string;
  
  // 静态方法
  static now(): GameDate;
  static fromString(dateString: string): GameDate;
}
```

#### 方法说明
- **constructor**: 创建新的游戏日期
- **equals**: 检查两个日期是否相等
- **isAfter**: 检查当前日期是否在指定日期之后
- **isBefore**: 检查当前日期是否在指定日期之前
- **addDays**: 增加指定天数
- **addMonths**: 增加指定月数
- **addYears**: 增加指定年数
- **toString**: 返回格式化的日期字符串
- **toISODate**: 返回 ISO 格式的日期字符串

### Gender
性别枚举。

```typescript
enum Gender {
  MALE = 'male',
  FEMALE = 'female'
}
```

### TitleTier
头衔等级枚举。

```typescript
enum TitleTier {
  COUNTY = 1,    // 郡
  DUCHY = 2,     // 公国
  KINGDOM = 3,   // 王国
  EMPIRE = 4     // 帝国
}
```

### AttributeSet
角色属性集合。

```typescript
class AttributeSet {
  diplomacy: number;    // 外交
  martial: number;      // 军事
  stewardship: number;  // 管理
  intrigue: number;     // 谋略
  learning: number;     // 学习

  constructor(
    diplomacy: number = 0,
    martial: number = 0,
    stewardship: number = 0,
    intrigue: number = 0,
    learning: number = 0
  );
  
  // 属性操作
  getTotal(): number;
  getAverage(): number;
  modify(attribute: string, value: number): void;
  reset(): void;
}
```

### Trait
特质系统。

```typescript
class Trait {
  id: string;
  name: string;
  description: string;
  effects: TraitEffect[];
  incompatibleTraits: string[];
  
  constructor(
    id: string,
    name: string,
    description: string,
    effects: TraitEffect[] = [],
    incompatibleTraits: string[] = []
  );
  
  // 检查是否与其他特质不兼容
  isIncompatibleWith(traitId: string): boolean;
}

interface TraitEffect {
  attribute: string;      // 影响的属性
  value: number;          // 影响的值
  type: 'additive' | 'multiplicative'; // 影响类型
}
```

---

## 世界系统 API

### Character
角色类，表示游戏中的每个角色。

```typescript
class Character {
  id: number;
  name: string;
  birthDate: GameDate;
  deathDate: GameDate | null;
  gender: Gender;
  dynasty: Dynasty;
  attributes: AttributeSet;
  traits: Trait[];
  spouse: Character | null;
  children: Character[];
  parents: Character[];
  titles: Title[];
  gold: number;
  prestige: number;
  piety: number;
  
  constructor(
    id: number,
    name: string,
    dynasty: Dynasty,
    gender: Gender,
    birthDate: GameDate,
    deathDate?: GameDate
  );
  
  // 基本信息
  getAge(currentDate: GameDate): number;
  isAlive(currentDate: GameDate): boolean;
  isAdult(currentDate: GameDate): boolean;
  
  // 关系管理
  marry(partner: Character, weddingDate: GameDate): boolean;
  divorce(divorceDate: GameDate): boolean;
  addChild(child: Character): void;
  getRelationship(other: Character): number;
  
  // 头衔管理
  addTitle(title: Title): void;
  removeTitle(titleId: number): void;
  hasTitle(titleId: number): boolean;
  getHighestTitle(): Title | null;
  
  // 经济
  addGold(amount: number): void;
  spendGold(amount: number): boolean;
  
  // 声望与虔诚
  addPrestige(amount: number): void;
  addPiety(amount: number): void;
  
  // 属性计算（考虑特质影响）
  getEffectiveAttributes(): AttributeSet;
  getAttributeWithTraits(attributeName: string): number;
}
```

### Dynasty
王朝类，管理家族血脉。

```typescript
class Dynasty {
  id: number;
  name: string;
  founder: Character;
  members: Character[];
  prestige: number;
  foundedDate: GameDate;
  
  constructor(
    id: number,
    name: string,
    founder: Character,
    foundedDate: GameDate
  );
  
  // 成员管理
  addMember(character: Character): void;
  removeMember(characterId: number): void;
  getMember(characterId: number): Character | null;
  getAllLivingMembers(currentDate: GameDate): Character[];
  
  // 继承
  getHeir(successionLaw: SuccessionLaw): Character | null;
  getPossibleHeirs(): Character[];
  
  // 声望
  addPrestige(amount: number): void;
  getRenown(): number;
}
```

### County
郡县类，表示游戏中的基本领土单位。

```typescript
class County {
  id: number;
  name: string;
  holder: Character | null;
  development: number;      // 发展度
  terrain: Terrain;         // 地形
  buildings: CountyBuilding[];
  tradeRoutes: TradeRoute[];
  fortLevel: number;        // 城堡等级
  taxIncome: number;        // 税收收入
  manpower: number;         // 人力
  
  constructor(
    id: number,
    name: string,
    terrain: Terrain
  );
  
  // 经济
  calculateTaxIncome(): number;
  calculateManpower(): number;
  develop(): void;
  
  // 建筑
  addBuilding(building: CountyBuilding): void;
  removeBuilding(buildingId: number): void;
  upgradeBuilding(buildingId: number): boolean;
  
  // 贸易
  addTradeRoute(route: TradeRoute): void;
  removeTradeRoute(routeId: number): void;
  getTradeIncome(): number;
  
  // 控制
  setHolder(character: Character): void;
  isControlledBy(character: Character): boolean;
}
```

### Title
头衔类，表示各种贵族头衔。

```typescript
class Title {
  id: number;
  name: string;
  tier: TitleTier;
  holder: Character | null;
  deJureVassals: Title[];
  deFactoVassals: Title[];
  capitalCounty: County | null;
  
  constructor(
    id: number,
    name: string,
    tier: TitleTier
  );
  
  // 领土管理
  addDeJureVassal(title: Title): void;
  removeDeJureVassal(titleId: number): void;
  addDeFactoVassal(title: Title): void;
  
  // 继承
  getHeir(successionLaw: SuccessionLaw): Character | null;
  setHolder(character: Character): void;
  
  // 等级检查
  isHigherTierThan(other: Title): boolean;
  isLowerTierThan(other: Title): boolean;
}
```

### World
世界状态类，管理整个游戏世界。

```typescript
class World {
  characters: Map<number, Character>;
  dynasties: Map<number, Dynasty>;
  counties: Map<number, County>;
  titles: Map<number, Title>;
  currentDate: GameDate;
  nextCharacterId: number;
  nextDynastyId: number;
  nextCountyId: number;
  nextTitleId: number;
  
  constructor();
  
  // 创建实体
  createCharacter(params: CreateCharacterParams): Character;
  createDynasty(name: string, founder: Character): Dynasty;
  createCounty(params: CreateCountyParams): County;
  createTitle(params: CreateTitleParams): Title;
  
  // 查找实体
  getCharacter(id: number): Character | null;
  getDynasty(id: number): Dynasty | null;
  getCounty(id: number): County | null;
  getTitle(id: number): Title | null;
  
  // 时间管理
  advanceDay(): void;
  advanceMonth(): void;
  advanceYear(): void;
  
  // 游戏逻辑
  processDeaths(): void;
  processInheritance(): void;
  processEconomy(): void;
  processDiplomacy(): void;
  
  // 数据持久化
  save(): SaveData;
  load(data: SaveData): void;
}
```

---

## 军事系统 API

### Army
军团类，表示游戏中的军事单位。

```typescript
class Army {
  id: number;
  commander: Character;
  name: string;
  size: number;
  morale: number;           // 士气 (0-100)
  discipline: number;       // 纪律 (0-100)
  supply: number;           // 补给
  location: County | null;
  status: ArmyStatus;
  unitStacks: UnitStack[];
  
  constructor(
    id: number,
    commander: Character,
    name: string
  );
  
  // 状态管理
  move(destination: County): boolean;
  setSiegeTarget(county: County): void;
  endSiege(): void;
  disband(): void;
  
  // 战斗
  updateMorale(): void;
  takeLosses(losses: number): void;
  reinforce(count: number): void;
  
  // 补给
  updateSupply(): void;
  isSupplied(): boolean;
  
  // 兵种管理
  addUnitStack(stack: UnitStack): void;
  removeUnitStack(stackId: number): void;
  getTotalStrength(): number;
}
```

### ArmyStatus
军团状态枚举。

```typescript
enum ArmyStatus {
  IDLE = 'idle',           // 待命
  MOVING = 'moving',       // 移动中
  FIGHTING = 'fighting',   // 战斗中
  SIEGING = 'sieging',     // 围城中
  RETREATING = 'retreating' // 撤退中
}
```

### BattleSimulator
战斗模拟器，处理战斗逻辑。

```typescript
class BattleSimulator {
  // 发起战斗
  static initiateBattle(
    attackers: Army[],
    defenders: Army[],
    location: County
  ): Battle;
  
  // 模拟战斗回合
  static simulateBattleRound(battle: Battle): BattleResult;
  
  // 计算战斗结果
  static calculateBattleResult(battle: Battle): BattleResult;
  
  // 处理战后
  static processAftermath(battle: Battle, result: BattleResult): void;
  
  // 计算战斗优势
  static calculateAttackerAdvantage(
    attackers: Army[],
    defenders: Army[]
  ): number;
}
```

### Siege
围城类，处理城池围攻。

```typescript
class Siege {
  id: number;
  county: County;
  attackers: Army[];
  progress: number;         // 围城进度 (0-100)
  maxProgress: number;
  startDate: GameDate;
  events: SiegeEvent[];
  
  constructor(
    id: number,
    county: County,
    attackers: Army[],
    startDate: GameDate
  );
  
  // 围城进度
  advance(): void;
  isCompleted(): boolean;
  getEstimatedCompletionDate(): GameDate;
  
  // 事件
  triggerRandomEvent(): SiegeEvent | null;
  addEvent(event: SiegeEvent): void;
  
  // 结束围城
  complete(success: boolean): void;
  abort(): void;
}
```

### War
战争类，处理战争状态。

```typescript
class War {
  id: number;
  name: string;
  attackers: WarParticipant[];
  defenders: WarParticipant[];
  startDate: GameDate;
  endDate: GameDate | null;
  casusBelli: CasusBelli;
  score: number;            // 战争分数 (-100 to 100)
  
  constructor(
    id: number,
    attackers: WarParticipant[],
    defenders: WarParticipant[],
    casusBelli: CasusBelli,
    startDate: GameDate
  );
  
  // 参与者管理
  addAttacker(participant: WarParticipant): void;
  addDefender(participant: WarParticipant): void;
  removeParticipant(characterId: number): void;
  
  // 战争进展
  advance(): void;
  calculateWarScore(): number;
  isOver(): boolean;
  
  // 和平
  proposePeace(terms: Treaty[]): boolean;
  acceptPeace(terms: Treaty[]): void;
  endWar(result: WarResult): void;
}
```

---

## 政治系统 API

### Diplomacy
外交关系管理。

```typescript
class Diplomacy {
  private world: World;
  private relations: Map<string, DiplomacyFlags>;
  
  constructor(world: World);
  
  // 关系管理
  getRelation(charA: Character, charB: Character): DiplomacyFlags;
  setRelation(charA: Character, charB: Character, flags: DiplomacyFlags): void;
  modifyRelation(charA: Character, charB: Character, modifier: number): void;
  
  // 外交行动
  proposeAlliance proposer: Character, target: Character): boolean;
  acceptAlliance(proposer: Character, target: Character): void;
  breakAlliance(charA: Character, charB: Character): void;
  
  // 宣战理由
  createCasusBelli(type: string, creator: Character, target: Character): CasusBelli;
  hasValidCasusBelli(attacker: Character, defender: Character): boolean;
  
  // 条约
  signTreaty(parties: Character[], treaty: Treaty): void;
  getActiveTreaties(character: Character): Treaty[];
}
```

### Council
内阁系统。

```typescript
class Council {
  ruler: Character;
  positions: Map<CouncilPosition, Character | null>;
  tasks: CouncilTask[];
  
  constructor(ruler: Character);
  
  // 职位管理
  appoint(position: CouncilPosition, character: Character): boolean;
  dismiss(position: CouncilPosition): void;
  getCouncilor(position: CouncilPosition): Character | null;
  
  // 任务管理
  assignTask(task: CouncilTask): void;
  completeTask(taskId: number): void;
  getActiveTasks(): CouncilTask[];
  
  // 效果
  calculateCouncilEffectiveness(): number;
  applyCouncilBonuses(): void;
}
```

### Faction
派系系统。

```typescript
class Faction {
  id: number;
  name: string;
  kind: FactionKind;
  leader: Character;
  members: Character[];
  demands: string[];
  power: number;            // 派系权力 (0-100)
  discontent: number;       // 不满程度 (0-100)
  
  constructor(
    id: number,
    name: string,
    kind: FactionKind,
    leader: Character
  );
  
  // 成员管理
  addMember(character: Character): void;
  removeMember(character: Character): void;
  getMemberCount(): number;
  
  // 权力与不满
  calculatePower(): number;
  increaseDiscontent(amount: number): void;
  decreaseDiscontent(amount: number): void;
  
  // 行动
  makeDemand(demand: string): void;
  startRebellion(): boolean;
  disband(): void;
}
```

### Scheme
阴谋系统。

```typescript
class Scheme {
  id: number;
  kind: SchemeKind;
  owner: Character;
  target: Character;
  progress: number;         // 进度 (0-100)
  startDate: GameDate;
  secret: boolean;
  
  constructor(
    id: number,
    kind: SchemeKind,
    owner: Character,
    target: Character,
    startDate: GameDate
  );
  
  // 进度管理
  advance(): void;
  isCompleted(): boolean;
  getSuccessChance(): number;
  
  // 发现
  discoverBy(character: Character): void;
  isDiscovered(): boolean;
  
  // 结果
  complete(): SchemeOutcome;
  fail(): SchemeOutcome;
  abort(): void;
}
```

---

## 事件系统 API

### EventEngine
事件引擎，处理事件触发和执行。

```typescript
class EventEngine {
  private world: World;
  private eventDefinitions: Map<string, EventDef>;
  private activeEvents: EventInstance[];
  
  constructor(world: World);
  
  // 事件管理
  registerEvent(eventDef: EventDef): void;
  triggerEvent(eventId: string, target?: Character): EventInstance | null;
  processEvent(instance: EventInstance, choiceId: string): void;
  
  // 触发检查
  checkTriggers(character: Character): EventDef[];
  getAvailableEvents(character: Character): EventDef[];
  
  // 剧情线
  startStoryline(storylineId: string): Storyline;
  advanceStoryline(storyline: Storyline): void;
}
```

### EventDef
事件定义。

```typescript
interface EventDef {
  id: string;
  title: string;
  description: string;
  trigger: EventTrigger;
  choices: EventChoice[];
  isMajor: boolean;
  canTriggerMultiple: boolean;
}

interface EventTrigger {
  conditions: TriggerCondition[];
  weight: number;
  cooldown: number;  // 冷却时间（天数）
}
```

### Storyline
剧情线。

```typescript
class Storyline {
  id: string;
  name: string;
  currentStage: number;
  stages: StorylineStage[];
  status: StorylineStatus;
  startDate: GameDate;
  
  constructor(
    id: string,
    name: string,
    stages: StorylineStage[]
  );
  
  // 进度管理
  advance(): void;
  getCurrentStage(): StorylineStage | null;
  isCompleted(): boolean;
  isFailed(): boolean;
  
  // 控制
  complete(): void;
  fail(): void;
  abandon(): void;
}
```

---

## AI 系统 API

### AiDirector
AI 决策器。

```typescript
class AiDirector {
  private world: World;
  private personalities: Map<number, AiPersonality>;
  
  constructor(world: World);
  
  // 决策
  makeMonthlyDecision(character: Character): AiAction[];
  makeDailyDecision(character: Character): AiAction[];
  evaluateAction(action: AiAction): number;
  
  // 性格管理
  setPersonality(character: Character, personality: AiPersonality): void;
  getPersonality(character: Character): AiPersonality;
  
  // 战略
  formulateGrandStrategy(character: Character): GrandStrategy;
  adjustStrategy(character: Character, circumstances: GameCircumstances): void;
}
```

### AiPersonality
AI 性格档案。

```typescript
class AiPersonality {
  profile: PersonalityProfile;
  traits: PersonalityTrait[];
  preferences: AIPreferences;
  riskTolerance: number;     // 风险容忍度 (0-100)
  aggression: number;        // 攻击性 (0-100)
  diplomacy: number;         // 外交倾向 (0-100)
  
  constructor(profile: PersonalityProfile);
  
  // 行为预测
  predictAction(context: AIContext): AiAction;
  evaluateRisk(action: AiAction): number;
  getPreferredActions(context: AIContext): AiAction[];
}
```

---

## 游戏引擎 API

### GameSimulation
游戏模拟核心。

```typescript
class GameSimulation {
  private world: World;
  private eventEngine: EventEngine;
  private aiDirector: AiDirector;
  private isPaused: boolean;
  private gameSpeed: number;
  
  constructor();
  
  // 初始化
  initialize(scenarioId: string): void;
  loadSaveData(data: SaveData): void;
  
  // 游戏循环
  start(): void;
  pause(): void;
  resume(): void;
  stop(): void;
  
  // 时间控制
  setSpeed(speed: number): void;
  advanceDay(): void;
  advanceMonth(): void;
  
  // 状态
  getCurrentDate(): GameDate;
  getWorld(): World;
  getGameState(): GameState;
  
  // 数据
  createSaveData(): SaveData;
}
```

### ScenarioLoader
场景加载器。

```typescript
class ScenarioLoader {
  // 加载场景
  static loadScenario(scenarioId: string): World;
  
  // 解析数据
  private static parseCharacters(data: any): Map<number, Character>;
  private static parseDynasties(data: any): Map<number, Dynasty>;
  private static parseCounties(data: any): Map<number, County>;
  private static parseTitles(data: any): Map<number, Title>;
  
  // 验证场景
  private static validateScenario(data: any): boolean;
}
```

---

## 用户界面 API

### GameAPI
游戏 API，提供前端接口。

```typescript
class GameAPI {
  private simulation: GameSimulation;
  
  constructor(simulation: GameSimulation);
  
  // 状态查询
  getGameState(): GameState;
  getCharacter(id: number): CharacterData;
  getCounty(id: number): CountyData;
  getArmy(id: number): ArmyData;
  
  // 操作
  createArmy(commanderId: number, name: string): number;
  moveArmy(armyId: number, destinationId: number): boolean;
  declareWar(attackerId: number, defenderId: number): boolean;
  
  // 事件
  getActiveEvents(): EventInstance[];
  selectEventChoice(eventId: string, choiceId: string): void;
  
  // 保存/加载
  saveGame(slot: number): void;
  loadGame(slot: number): void;
}
```

### GameTUI
文字用户界面。

```typescript
class GameTUI {
  private api: GameAPI;
  private currentView: string;
  
  constructor(api: GameAPI);
  
  // 界面控制
  start(): void;
  showMainMenu(): void;
  showGameView(): void;
  showEventView(event: EventInstance): void;
  
  // 输入处理
  handleInput(input: string): void;
  processCommand(command: string): void;
  
  // 显示
  displayCharacterInfo(character: Character): void;
  displayCountyInfo(county: County): void;
  displayArmyInfo(army: Army): void;
}
```

---

## 数据类型

### GameState
```typescript
interface GameState {
  currentDate: GameDate;
  playerCharacterId: number;
  isPaused: boolean;
  gameSpeed: number;
  activeEvents: EventInstance[];
  activeWars: War[];
}
```

### SaveData
```typescript
interface SaveData {
  version: string;
  saveDate: Date;
  world: WorldData;
  gameState: GameState;
  customData?: Record<string, any>;
}
```

### CharacterData
```typescript
interface CharacterData {
  id: number;
  name: string;
  dynasty: string;
  age: number;
  gender: Gender;
  attributes: AttributeSet;
  traits: string[];
  titles: number[];
  gold: number;
  prestige: number;
  piety: number;
}
```

---

## 错误处理

### GameError
```typescript
class GameError extends Error {
  code: string;
  details?: Record<string, any>;
  
  constructor(message: string, code: string, details?: Record<string, any>);
}

// 错误代码
const ERROR_CODES = {
  CHARACTER_NOT_FOUND: 'CHARACTER_NOT_FOUND',
  INVALID_ACTION: 'INVALID_ACTION',
  INSUFFICIENT_RESOURCES: 'INSUFFICIENT_RESOURCES',
  EVENT_ERROR: 'EVENT_ERROR',
  SAVE_LOAD_ERROR: 'SAVE_LOAD_ERROR'
};
```

---

## 使用示例

### 基本使用
```typescript
// 初始化游戏
const simulation = new GameSimulation();
simulation.initialize('1066');

// 获取玩家角色
const gameState = simulation.getGameState();
const player = simulation.getWorld().getCharacter(gameState.playerCharacterId);

// 创建军队
const armyId = simulation.getWorld().createArmy(player.id, "皇家卫队");

// 移动军队
simulation.getWorld().getArmy(armyId)?.move(destinationCounty);

// 推进时间
simulation.start();
```

### 事件处理
```typescript
// 获取可用事件
const availableEvents = simulation.getEventEngine().getAvailableEvents(player);

// 处理事件选择
if (availableEvents.length > 0) {
  const event = availableEvents[0];
  const choice = event.choices[0];
  simulation.getEventEngine().processEvent(event, choice.id);
}
```

### 外交行动
```typescript
// 创建同盟
const diplomacy = new Diplomacy(simulation.getWorld());
const success = diplomacy.proposeAlliance(player, targetCharacter);

if (success) {
  console.log("同盟建立成功！");
}
```

---

## API 版本历史

### v1.0.0 (当前版本)
- 完整的核心 API
- 世界、军事、政治、事件系统
- AI 决策系统
- 用户界面接口
- 保存/加载功能

---

## 获取支持

如有 API 使用问题，请参考：
- [开发指南](development.md)
- [示例代码](../examples/)
- [问题反馈](https://github.com/your-username/ckgame-ts/issues)