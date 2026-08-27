// Package calendar 提供游戏日历与季节系统。
// 对应 Java 版 com.ckgame.core.calendar。
package calendar

import "fmt"

// Season 季节枚举。
type Season int

const (
	Spring Season = iota
	Summer
	Autumn
	Winter
)

// String 实现 fmt.Stringer，返回中文名。
func (s Season) String() string {
	switch s {
	case Spring:
		return "春"
	case Summer:
		return "夏"
	case Autumn:
		return "秋"
	case Winter:
		return "冬"
	default:
		return "未知"
	}
}

// GameDate 游戏日期，不可变值类型，支持自然序比较与推进。
type GameDate struct {
	Year  int `json:"year"`
	Month int `json:"month"`
	Day   int `json:"day"`
}

// NewGameDate 构造日期。
func NewGameDate(year, month, day int) GameDate {
	return GameDate{Year: year, Month: month, Day: day}
}

// ParseGameDate 从 [year, month, day] 数组解析（JSON 场景格式）。
func ParseGameDate(arr []int) GameDate {
	if len(arr) != 3 {
		return NewGameDate(1066, 1, 1)
	}
	return NewGameDate(arr[0], arr[1], arr[2])
}

// ToArray 序列化为 [year, month, day] 数组。
func (d GameDate) ToArray() []int {
	return []int{d.Year, d.Month, d.Day}
}

// Season 根据月份返回季节：3-5 春 / 6-8 夏 / 9-11 秋 / 12-2 冬。
func (d GameDate) Season() Season {
	switch {
	case d.Month >= 3 && d.Month <= 5:
		return Spring
	case d.Month >= 6 && d.Month <= 8:
		return Summer
	case d.Month >= 9 && d.Month <= 11:
		return Autumn
	default:
		return Winter
	}
}

// IsLeapYear 是否闰年。
func (d GameDate) IsLeapYear() bool {
	return (d.Year%4 == 0 && d.Year%100 != 0) || d.Year%400 == 0
}

// DaysInMonth 当月天数。
func (d GameDate) DaysInMonth() int {
	switch d.Month {
	case 1, 3, 5, 7, 8, 10, 12:
		return 31
	case 4, 6, 9, 11:
		return 30
	default:
		if d.IsLeapYear() {
			return 29
		}
		return 28
	}
}

// AdvanceDays 推进指定天数，返回新日期。
func (d GameDate) AdvanceDays(days int) GameDate {
	y, m, day := d.Year, d.Month, d.Day
	remaining := days
	for remaining > 0 {
		dim := NewGameDate(y, m, 1).DaysInMonth()
		left := dim - day + 1
		if remaining < left {
			day += remaining
			break
		}
		remaining -= left
		m++
		day = 1
		if m > 12 {
			m = 1
			y++
		}
	}
	return NewGameDate(y, m, day)
}

// AdvanceOneDay 推进一天。
func (d GameDate) AdvanceOneDay() GameDate { return d.AdvanceDays(1) }

// AddMonths 推进指定月数（按儒略近似），保持日落月内。
func (d GameDate) AddMonths(months int) GameDate {
	total := d.Month - 1 + months
	y := d.Year + total/12
	m := total%12 + 1
	adjust := NewGameDate(y, m, 31)
	day := d.Day
	if adjust.DaysInMonth() < day && m == 12 {
		day = 31
	}
	g := NewGameDate(y, m, day)
	for g.Day > g.DaysInMonth() {
		g.Day--
	}
	return g
}

// IsMonthStart 是否月初（day==1）。
func (d GameDate) IsMonthStart() bool { return d.Day == 1 }

// IsYearStart 是否年初。
func (d GameDate) IsYearStart() bool { return d.Month == 1 && d.Day == 1 }

// ToOrdinal 近似儒略日序，用于年龄/继承人排序，不要求天文精度。
func (d GameDate) ToOrdinal() int {
	y, m := d.Year, d.Month
	if m <= 2 {
		y--
		m += 12
	}
	leap := y/4 - y/100 + y/400
	monthDays := (153*(m-3) + 2) / 5
	return y*365 + leap + monthDays + d.Day
}

// DaysUntil 计算到 other 的天数差。
func (d GameDate) DaysUntil(other GameDate) int {
	return other.ToOrdinal() - d.ToOrdinal()
}

// Before 判断是否早于 other。
func (d GameDate) Before(other GameDate) bool { return d.ToOrdinal() < other.ToOrdinal() }

// After 判断是否晚于 other。
func (d GameDate) After(other GameDate) bool { return d.ToOrdinal() > other.ToOrdinal() }

// Equal 判断是否同一天。
func (d GameDate) Equal(other GameDate) bool {
	return d.Year == other.Year && d.Month == other.Month && d.Day == other.Day
}

// AgeAt 计算 date 时点的周岁年龄。
func (d GameDate) AgeAt(date GameDate) int {
	age := date.Year - d.Year
	if date.Month < d.Month || (date.Month == d.Month && date.Day < d.Day) {
		age--
	}
	if age < 0 {
		age = 0
	}
	return age
}

// String 格式化为 YYYY-MM-DD。
func (d GameDate) String() string {
	return fmt.Sprintf("%04d-%02d-%02d", d.Year, d.Month, d.Day)
}
