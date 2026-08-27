#pragma once
// 角色性别枚举，对应 Go 版 core/gender.go。
namespace ckgame {

enum class Gender { Male, Female };

// 中文性别名。
inline const char* GenderName(Gender g) {
    switch (g) {
        case Gender::Male: return "男";
        case Gender::Female: return "女";
    }
    return "未知";
}

}  // namespace ckgame
