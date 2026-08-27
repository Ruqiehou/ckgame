#pragma once
// 角色五维属性，对应 Go 版 stats/attributeset.go。
namespace ckgame {
namespace stats {

struct AttributeSet {
    int diplomacy = 5;   // 外交
    int martial = 5;     // 军事
    int stewardship = 5; // 管理
    int intrigue = 5;    // 谋略
    int learning = 5;    // 学识

    int Total() const { return diplomacy + martial + stewardship + intrigue + learning; }

    // 按字段名取值。
    int Get(const char* name) const {
        if (name[0] == 'd') return diplomacy;
        if (name[0] == 'm') return martial;
        if (name[0] == 's') return stewardship;
        if (name[0] == 'i') return intrigue;
        if (name[0] == 'l') return learning;
        return 0;
    }

    // 按字段名加值（限幅 0-100）。
    void Add(const char* name, int v);
};

}  // namespace stats
}  // namespace ckgame