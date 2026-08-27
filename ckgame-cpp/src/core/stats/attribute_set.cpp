#include "core/stats/attribute_set.h"
#include <algorithm>

namespace ckgame {
namespace stats {

void AttributeSet::Add(const char* name, int v) {
    int* target = nullptr;
    switch (name[0]) {
        case 'd': target = &diplomacy; break;
        case 'm': target = &martial; break;
        case 's': target = &stewardship; break;
        case 'i': target = &intrigue; break;
        case 'l': target = &learning; break;
        default: return;
    }
    *target = std::clamp(*target + v, 0, 100);
}

}  // namespace stats
}  // namespace ckgame
