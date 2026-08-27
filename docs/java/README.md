# CKGame Java 版文档

十字军之王风格大战略模拟引擎 — Java 实现版。

- [引擎架构](engine-architecture.md)：项目结构与核心流程
- [游戏机制](game-mechanics.md)：战争、派系、法律、阴谋等机制
- [UI 与接口](ui-and-api.md)：TUI 文字界面与 GameAPI
- [数据格式](data-format.md)：存档与场景数据格式
- [开发者指南](developer-guide.md)：环境搭建与运行方式

## 快速开始

```bash
# 编译
mvn clean package

# 运行
java -jar target/ck-game.jar
```

## 版本特性

- 完整 TUI 文字界面（GameTUI.java）
- GameAPI 统一接口（snapshot()/action()）
- Maven 构建，Jackson 处理 JSON
- 主入口：com.ckgame.ui.Main
- 存档管理：SaveManager.java
