#pragma once
// 游戏平衡常量，对应 Go 版 balance/balance.go。
namespace ckgame {
namespace balance {

// 军事系统
inline constexpr int kBaseLevySize = 500;
inline constexpr double kLevyRecoveryPerMonth = 0.05;
inline constexpr double kSupplyLimitBase = 1000.0;
inline constexpr double kSupplyBaseCost = 0.1;

// 经济系统
inline constexpr double kBaseTaxRate = 0.2;
inline constexpr int kFestivalCost = 200;
inline constexpr int kKnightCost = 50;
inline constexpr int kBuildingBaseCost = 100;
inline constexpr int kDevelopCostPerLevel = 150;

// 政治系统
inline constexpr int kCouncilTaskDurationMonths = 3;
inline constexpr double kDiplomacyBaseSuccess = 0.5;
inline constexpr int kBaseRelationChange = 10;
inline constexpr int kClaimFabricationCost = 150;
inline constexpr int kClaimFabricationTimeMonths = 3;

// 战争系统
inline constexpr int kWarScoreWhitePeaceThreshold = -50;
inline constexpr int kWarScoreEnforceThreshold = 100;
inline constexpr double kWarDecayPerMonth = 0.5;

// 派系与阴谋
inline constexpr double kFactionPowerThreshold = 1.2;
inline constexpr int kSchemeBaseProgress = 10;
inline constexpr int kSchemeBaseExposure = 15;

}  // namespace balance
}  // namespace ckgame