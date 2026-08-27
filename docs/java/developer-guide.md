# 开发者指南（Java 版）

## 环境

- Java 17+
- Maven 3.x

## 构建与运行

```bash
# 编译
mvn clean package

# 运行（编译后的 JAR）
java -jar target/ck-game.jar

# 直接运行主类
mvn exec:java -Dexec.mainClass="com.ckgame.ui.Main"
```

## 项目依赖

- Jackson Databind 2.17.0（JSON 处理）

## 代码结构

包结构按功能划分：
- `com.ckgame.ai`：AI 行为
- `com.ckgame.core`：核心类型与常量
- `com.ckgame.events`：事件系统
- `com.ckgame.game`：模拟与场景
- `com.ckgame.military`：军事系统
- `com.ckgame.politics`：政治系统
- `com.ckgame.ui`：用户界面
- `com.ckgame.world`：世界数据

## 添加新功能

1. 在对应包中创建新类
2. 在 `GameAPI` 中添加 action 处理
3. 在 `GameTUI` 中添加菜单项
4. 更新测试与文档

## 存档位置

- 存档目录：`saves/`（项目根目录）
- 自动存档：`saves/autosave.json`

## 常见问题

**Q: 如何修改游戏平衡参数？**
A: 修改 `core/balance/Balance.java` 中的常量

**Q: 如何添加新事件？**
A: 在 `events/` 包中扩展事件定义，并在 `EventEngine` 中注册

**Q: 如何添加新建筑？**
A: 在 `world/buildings/BuildingKind.java` 中枚举新建筑类型
