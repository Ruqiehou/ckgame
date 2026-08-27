#include "core/calendar/season.h"

namespace ckgame {
namespace calendar {

const char* SeasonName(Season s) {
    switch (s) {
        case Season::Spring: return "春";
        case Season::Summer: return "夏";
        case Season::Autumn: return "秋";
        case Season::Winter: return "冬";
    }
    return "未知";
}

}  // namespace calendar
}  // namespace ckgame
