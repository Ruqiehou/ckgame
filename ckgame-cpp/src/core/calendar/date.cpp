#include "core/calendar/date.h"
#include <cstdio>

namespace ckgame {
namespace calendar {

Season GameDate::GetSeason() const {
    if (month >= 3 && month <= 5) return Season::Spring;
    if (month >= 6 && month <= 8) return Season::Summer;
    if (month >= 9 && month <= 11) return Season::Autumn;
    return Season::Winter;
}

bool GameDate::IsLeapYear() const {
    return (year % 4 == 0 && year % 100 != 0) || year % 400 == 0;
}

int GameDate::DaysInMonth() const {
    switch (month) {
        case 1: case 3: case 5: case 7: case 8: case 10: case 12:
            return 31;
        case 4: case 6: case 9: case 11:
            return 30;
        default:
            return IsLeapYear() ? 29 : 28;
    }
}

GameDate GameDate::AdvanceDays(int days) const {
    int y = year, m = month, d = day;
    int remaining = days;
    while (remaining > 0) {
        int dim = GameDate(y, m, 1).DaysInMonth();
        int left = dim - d + 1;
        if (remaining < left) {
            d += remaining;
            break;
        }
        remaining -= left;
        m++;
        d = 1;
        if (m > 12) {
            m = 1;
            y++;
        }
    }
    return GameDate(y, m, d);
}

GameDate GameDate::AddMonths(int months) const {
    int total = month - 1 + months;
    int y = year + total / 12;
    int m = total % 12 + 1;
    int d = day;
    GameDate g(y, m, d);
    while (g.day > g.DaysInMonth()) g.day--;
    return g;
}

int GameDate::ToOrdinal() const {
    int y = year, m = month;
    if (m <= 2) {
        y--;
        m += 12;
    }
    int leap = y / 4 - y / 100 + y / 400;
    int monthDays = (153 * (m - 3) + 2) / 5;
    return y * 365 + leap + monthDays + day;
}

int GameDate::AgeAt(const GameDate& date) const {
    int age = date.year - year;
    if (date.month < month || (date.month == month && date.day < day)) age--;
    if (age < 0) age = 0;
    return age;
}

std::string GameDate::ToString() const {
    char buf[16];
    std::snprintf(buf, sizeof(buf), "%04d-%02d-%02d", year, month, day);
    return buf;
}

}  // namespace calendar
}  // namespace ckgame
