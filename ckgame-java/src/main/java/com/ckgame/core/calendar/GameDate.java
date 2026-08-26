package com.ckgame.core.calendar;

import java.util.Objects;

/**
 * 游戏日期。不可变，支持自然序比较。
 */
public final class GameDate implements Comparable<GameDate> {
    private final int year;
    private final int month;
    private final int day;

    public GameDate(int year, int month, int day) {
        this.year = year;
        this.month = month;
        this.day = day;
    }

    public int year() { return year; }
    public int month() { return month; }
    public int day() { return day; }

    public Season season() {
        if (3 <= month && month <= 5) return Season.SPRING;
        if (6 <= month && month <= 8) return Season.SUMMER;
        if (9 <= month && month <= 11) return Season.AUTUMN;
        return Season.WINTER;
    }

    public boolean isLeapYear() {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
    }

    public int daysInMonth() {
        return switch (month) {
            case 1, 3, 5, 7, 8, 10, 12 -> 31;
            case 4, 6, 9, 11 -> 30;
            default -> isLeapYear() ? 29 : 28;
        };
    }

    public GameDate advanceDays(int days) {
        int y = year;
        int m = month;
        int d = day;
        int remaining = days;
        while (remaining > 0) {
            int dim = new GameDate(y, m, 1).daysInMonth();
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
        return new GameDate(y, m, d);
    }

    public GameDate advanceOneDay() {
        return advanceDays(1);
    }

    public boolean isMonthStart() {
        return day == 1;
    }

    public boolean isYearStart() {
        return month == 1 && day == 1;
    }

    /**
     * 近似儒略日序，用于年龄/继承人排序，不要求天文精度。
     */
    public int toOrdinal() {
        int y = year;
        int m = month;
        int d = day;
        if (m <= 2) {
            y -= 1;
            m += 12;
        }
        int leap = y / 4 - y / 100 + y / 400;
        int monthDays = (153 * (m - 3) + 2) / 5;
        return y * 365 + leap + monthDays + d;
    }

    public int daysUntil(GameDate other) {
        return other.toOrdinal() - this.toOrdinal();
    }

    @Override
    public int compareTo(GameDate o) {
        int cmp = Integer.compare(year, o.year);
        if (cmp != 0) return cmp;
        cmp = Integer.compare(month, o.month);
        if (cmp != 0) return cmp;
        return Integer.compare(day, o.day);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GameDate)) return false;
        GameDate that = (GameDate) o;
        return year == that.year && month == that.month && day == that.day;
    }

    @Override
    public int hashCode() {
        return Objects.hash(year, month, day);
    }

    @Override
    public String toString() {
        return String.format("%04d-%02d-%02d", year, month, day);
    }
}
