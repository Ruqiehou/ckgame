#pragma once
// 头衔等级，对应 Go 版 core/title_tier.go。
namespace ckgame {

enum class TitleTier {
    Barony = 1,   // 男爵
    County = 2,   // 伯爵
    Duchy = 3,    // 公爵
    Kingdom = 4,  // 国王
    Empire = 5,   // 皇帝
};

// 中文爵位名。
inline const char* RankName(TitleTier t) {
    switch (t) {
        case TitleTier::Barony: return "男爵";
        case TitleTier::County: return "伯爵";
        case TitleTier::Duchy: return "公爵";
        case TitleTier::Kingdom: return "国王";
        case TitleTier::Empire: return "皇帝";
    }
    return "未知";
}

// 头衔 ID 前缀（b/c/d/k/e）。
inline const char* TierPrefix(TitleTier t) {
    switch (t) {
        case TitleTier::Barony: return "b";
        case TitleTier::County: return "c";
        case TitleTier::Duchy: return "d";
        case TitleTier::Kingdom: return "k";
        case TitleTier::Empire: return "e";
    }
    return "?";
}

// 创建该级头衔所需威望。
inline double CreationCost(TitleTier t) {
    switch (t) {
        case TitleTier::Barony: return 0.0;
        case TitleTier::County: return 50.0;
        case TitleTier::Duchy: return 200.0;
        case TitleTier::Kingdom: return 500.0;
        case TitleTier::Empire: return 1000.0;
    }
    return 0.0;
}

// 公爵及以上头衔可被销毁。
inline bool IsDestroyable(TitleTier t) { return static_cast<int>(t) >= static_cast<int>(TitleTier::Duchy); }

}  // namespace ckgame
