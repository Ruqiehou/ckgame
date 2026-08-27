#include "ui/GameTUI.h"
#include "core/calendar/date.h"
#include "core/gender.h"
#include "core/title_tier.h"
#include "core/stats/attribute_set.h"
#include "core/traits/traits.h"

namespace ckgame {
namespace ui {

void GameTUI::Run() {
    std::cout << "CKGame C++ 版 - 演示启动\n";
    calendar::GameDate d(1066, 1, 1);
    std::cout << "初始日期: " << d.ToString() << "\n";
    std::cout << "季节: " << calendar::SeasonName(d.GetSeason()) << "\n";

    std::cout << "\n五维属性示例: ";
    stats::AttributeSet attrs;
    attrs.Add("d", 2);
    attrs.Add("m", 3);
    std::cout << "外交=" << attrs.diplomacy << " 军事=" << attrs.martial << "\n";

    std::cout << "\n特性示例: ";
    if (auto t = traits::Find("brave")) std::cout << "ID=" << t->id << " 名=" << t->name << "\n";

    std::cout << "\n性别示例: " << GenderName(Gender::Male) << " / " << GenderName(Gender::Female) << "\n";

    std::cout << "\n头衔等级示例: ";
    std::cout << "伯爵=" << RankName(TitleTier::County) << " 威望=" << CreationCost(TitleTier::County) << "\n";

    std::cout << "\n[按回车退出]";
    std::string tmp;
    std::getline(std::cin, tmp);
}

}  // namespace ui
}  // namespace ckgame