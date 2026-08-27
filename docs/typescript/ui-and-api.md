# UI 与接口（TypeScript 版）

## TUI 文字界面

[ui/GameTUI.ts](../../ckgame-ts/src/ui/GameTUI.ts) 实现文字对话模式。

主菜单功能：
- `[1-12]` 信息查看：状态、角色、领地、军队、战争、宣称、条约、统治者、围城、日志、地图
- `[A-L]` 军事行动：征召、移动、解散、宣战、议和、改善关系、举办宴会、建造、发展、伪造宣称、授予头衔、招募骑士
- `[M-Q]` 管理功能：内阁、阴谋、外交、法律、存档
- `[R]` 推进时间、`[S]` 切换角色、`[T]` 作弊、`[0]` 退出

注：建筑（H）与授予头衔（K）功能当前为占位实现。

## GameAPI 接口

[ui/GameAPI.ts](../../ckgame-ts/src/ui/GameAPI.ts) 提供统一操作接口。

### 状态查询

```typescript
const snap = api.snapshot();
```

返回包含以下字段：
- `player`：当前玩家信息（金币、威望、虔诚、压力、健康、属性、法律）
- `date`：当前日期（YYYY-MM-DD）
- `season`：季节（SPRING/SUMMER/AUTUMN/WINTER）
- `characters`：角色列表
- `counties`：省份列表
- `armies`：军队列表
- `wars`：战争列表
- `treaties`：条约列表
- `schemes`：阴谋列表
- `log`：事件日志

### 操作执行

```typescript
const res = api.action({
  action: 'raise_army',
  county_id: 1
});
```

常用 action 类型：
- `advance`：推进时间（days 参数）
- `raise_army`：征召军队（county_id）
- `move_army`：移动军队（army_id, county_id）
- `disband_army`：解散军队（army_id）
- `declare_war`：宣战（target_id）
- `white_peace`：白和（war_id）
- `form_alliance`：结盟（target_id）
- `start_scheme`：发起阴谋（scheme_kind, target_id）
- `set_succession_law` / `set_crown_authority` / `set_gender_law`：改法
- `develop_county`：发展领地（county_id）
- `fabricate_claim`：伪造宣称（county_id）
- `recruit_knights`：招募精锐
- `hold_feast`：举办宴会
- `improve_relations`：改善关系（target_id）
- `set_player`：切换角色（character_id）
- `save` / `load` / `delete_save`：存档管理
- `new_game`：新游戏
- `toggle_cheat`：作弊开关

## 主入口

[ui/Main.ts](../../ckgame-ts/src/ui/Main.ts) 启动 TUI：

```typescript
new GameTUI().run().catch(err => {
  console.error('运行错误:', err);
  process.exit(1);
});
```

## 异步交互

TypeScript 版本使用 async/await 处理用户输入，所有菜单操作均为异步：
- `input()`：获取字符串输入
- `inputInt()`：获取整数输入
- `inputDouble()`：获取浮点数输入
- `pause()`：暂停等待回车