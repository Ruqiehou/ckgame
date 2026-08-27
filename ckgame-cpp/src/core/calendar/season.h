#pragma once
// 季节枚举。
namespace ckgame {
namespace calendar {

enum class Season { Spring, Summer, Autumn, Winter };

// 中文季节名。
const char* SeasonName(Season s);

}  // namespace calendar
}  // namespace ckgame
