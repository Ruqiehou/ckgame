#pragma once
// 游戏日期，对应 Go 版 calendar/date.go。
#include "season.h"
#include <string>
#include <vector>

namespace ckgame {
namespace calendar {

class GameDate {
public:
    int year, month, day;

    GameDate(int y = 1066, int m = 1, int d = 1) : year(y), month(m), day(d) {}

    // 从数组解析（JSON 场景格式）。
    static GameDate Parse(const std::vector<int>& arr) {
        if (arr.size() == 3) return GameDate(arr[0], arr[1], arr[2]);
        return GameDate();
    }

    // 序列化为数组。
    std::vector<int> ToArray() const { return {year, month, day}; }

    // 根据月份返回季节。
    Season GetSeason() const;

    // 是否闰年。
    bool IsLeapYear() const;

    // 当月天数。
    int DaysInMonth() const;

    // 推进指定天数，返回新日期。
    GameDate AdvanceDays(int days) const;

    // 推进一天。
    GameDate AdvanceOneDay() const { return AdvanceDays(1); }

    // 推进指定月数（近似）。
    GameDate AddMonths(int months) const;

    // 是否月初或年初。
    bool IsMonthStart() const { return day == 1; }
    bool IsYearStart() const { return month == 1 && day == 1; }

    // 儒略日序，用于年龄/排序。
    int ToOrdinal() const;

    // 天数差。
    int DaysUntil(const GameDate& other) const { return other.ToOrdinal() - ToOrdinal(); }

    bool Before(const GameDate& other) const { return ToOrdinal() < other.ToOrdinal(); }
    bool After(const GameDate& other) const { return ToOrdinal() > other.ToOrdinal(); }
    bool Equal(const GameDate& other) const { return year == other.year && month == other.month && day == other.day; }

    // 计算 date 时的周岁年龄。
    int AgeAt(const GameDate& date) const;

    // 格式化为 YYYY-MM-DD。
    std::string ToString() const;
};

}  // namespace calendar
}  // namespace ckgame