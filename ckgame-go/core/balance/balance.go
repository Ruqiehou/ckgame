// Package balance 提供游戏平衡常量。
// 对应 Java 版 com.ckgame.core.balance。
package balance

// 战争疲劳常量
const (
	WarExhaustionOnDeclare    = 15.0 // 宣战时初始疲劳
	WarExhaustionMonthlyAtk   = 1.0  // 进攻方月度疲劳增长
	WarExhaustionMonthlyDef   = 0.8  // 防御方月度疲劳增长
	WarExhaustionDecay        = 1.5  // 自然衰减
	WarExhaustionDeclareBlock = 60.0 // 疲劳超过此值时禁止宣战
)

// 白和条件常量
const (
	WhitePeaceMinMonths        = 12   // 战争满12个月可白和
	WhitePeaceStalemateMonths = 18   // 满18个月无变化
	WhitePeaceStalemateScore   = 15   // warScore 在 ±15 之间
	WhitePeaceFatigueScore     = 25   // warScore 绝对值小于25
	WhitePeaceFatigueThreshold = 40.0 // 双方疲劳都超过40
	WhitePeaceMaxMonths        = 36   // 最长36个月
	WhitePeaceMaxScore         = 50   // warScore 绝对值不超过50
	WhitePeaceTruceYears       = 3    // 白和休战年限
	VictoryTruceYears          = 5    // 强制和约休战年限
)

// 军队常量
const (
	ArmyMaintenanceMult    = 1.25 // 军队维护倍率
	SupplyRecoverFriendly  = 2.5  // 友方领土补员
	SupplyDrainEnemy       = 0.7  // 敌方领土消耗
	SupplyDrainWinter      = 1.2  // 冬季额外消耗
	SupplyLowThreshold     = 25.0 // 补给阈值
	SupplyMoveSlowThreshold = 30.0 // 移动减缓阈值
	MoraleRecoverFriendly  = 0.3  // 友方士气恢复
	MoraleDrainLowSupply   = 0.8  // 低补给士气消耗
)

// 季节移动倍率
const (
	MoveChanceWinter      = 0.55
	MoveChanceAutumn      = 0.85
	MoveChanceLowSupply   = 0.7
	MoveChanceMin         = 0.2
)

// 季节战斗倍率
const (
	SeasonCombatSpring = 1.0
	SeasonCombatSummer = 1.05
	SeasonCombatAutumn = 0.92
	SeasonCombatWinter = 0.8
)

// 派系相关常量
const (
	FactionAppeaseDefault      = 25.0 // 普通平复门槛
	FactionAppeasePlayer       = 30.0 // 玩家角色平复门槛
	FactionAppeaseGold         = 25.0 // 金钱平复量
	FactionFeastAppease        = 10.0 // 宴会平复量
	FactionDissolveDiscontent  = 5.0  // 不满可解散门槛
	FactionDissolvePower       = 40.0 // 强权可解散门槛
)

// 外交玩家操作常量
const (
	ImproveRelationsGold = 10 // 改善关系消耗金钱
	FeastGold           = 20 // 宴会消耗金钱
	FeastPrestige       = 15 // 宴会获得威望
	FeastStress         = -10 // 宴会减少压力
)