#include "core/traits/traits.h"

namespace ckgame {
namespace traits {

const std::vector<Trait>& All() {
    static const std::vector<Trait> kTraits = {
        {"brave", "勇敢", Kind::Personality},
        {"craven", "怯懦", Kind::Personality},
        {"just", "公正", Kind::Personality},
        {"arbitrary", "任性", Kind::Personality},
        {"ambitious", "野心勃勃", Kind::Personality},
        {"content", "安于现状", Kind::Personality},
        {"temperate", "节制", Kind::Personality},
        {"gluttonous", "暴食", Kind::Personality},
        {"charitable", "仁慈", Kind::Personality},
        {"greedy", "贪婪", Kind::Personality},
        {"diligent", "勤奋", Kind::Personality},
        {"lazy", "懒惰", Kind::Personality},
        {"patient", "耐心", Kind::Personality},
        {"wroth", "暴怒", Kind::Personality},
        {"humble", "谦逊", Kind::Personality},
        {"proud", "傲慢", Kind::Personality},
        {"honest", "诚实", Kind::Personality},
        {"deceitful", "狡诈", Kind::Personality},
        {"gregarious", "合群", Kind::Personality},
        {"shy", "害羞", Kind::Personality},
        {"zealous", "狂热", Kind::Personality},
        {"cynical", "愤世嫉俗", Kind::Personality},
        {"strong", "强壮", Kind::Physical},
        {"weak", "虚弱", Kind::Physical},
        {"beautiful", "貌美", Kind::Physical},
        {"scarred", "有伤疤", Kind::Physical},
        {"brilliant", "聪慧", Kind::Physical},
        {"slow", "迟钝", Kind::Physical},
    };
    return kTraits;
}

const Trait* Find(const std::string& id) {
    for (const auto& t : All()) {
        if (t.id == id) return &t;
    }
    return nullptr;
}

}  // namespace traits
}  // namespace ckgame
