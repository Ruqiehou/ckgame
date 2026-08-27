#pragma once
// 角色特性，对应 Go 版 traits/traits.go。
#include <string>
#include <vector>

namespace ckgame {
namespace traits {

enum class Kind {
    Personality,  // 性格
    Physical,     // 身体
    Lifestyle,    // 生活方式
};

struct Trait {
    std::string id;
    std::string name;
    Kind kind;
};

// 内置特性表，按 id 查询；未找到返回 nullptr。
const Trait* Find(const std::string& id);

// 全部内置特性。
const std::vector<Trait>& All();

}  // namespace traits
}  // namespace ckgame