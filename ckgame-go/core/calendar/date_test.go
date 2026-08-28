package calendar

import "testing"

func TestAdvanceDaysAcrossMonths(t *testing.T) {
	cases := []struct {
		start GameDate
		days  int
		want  GameDate
	}{
		{NewGameDate(1066, 1, 30), 3, NewGameDate(1066, 2, 2)},
		{NewGameDate(1066, 12, 31), 1, NewGameDate(1067, 1, 1)},
		{NewGameDate(1066, 2, 28), 1, NewGameDate(1066, 3, 1)},
		{NewGameDate(1064, 2, 28), 1, NewGameDate(1064, 2, 29)}, // 1064 为闰年
	}
	for _, c := range cases {
		if got := c.start.AdvanceDays(c.days); !got.Equal(c.want) {
			t.Errorf("AdvanceDays(%s, %d) = %s, want %s", c.start, c.days, got, c.want)
		}
	}
}

func TestAddMonthsClampsDay(t *testing.T) {
	if got := NewGameDate(1066, 1, 31).AddMonths(1); !got.Equal(NewGameDate(1066, 2, 28)) {
		t.Errorf("1月31日 + 1月 = %s, want 1066-02-28", got)
	}
	if got := NewGameDate(1066, 10, 14).AddMonths(36); !got.Equal(NewGameDate(1069, 10, 14)) {
		t.Errorf("停战 36 月计算错误: %s", got)
	}
}

func TestSeason(t *testing.T) {
	cases := map[int]Season{1: Winter, 3: Spring, 7: Summer, 10: Autumn, 12: Winter}
	for month, want := range cases {
		if got := NewGameDate(1066, month, 15).Season(); got != want {
			t.Errorf("月份 %d 季节 = %v, want %v", month, got, want)
		}
	}
}

func TestAgeAt(t *testing.T) {
	birth := NewGameDate(1028, 9, 1)
	if got := birth.AgeAt(NewGameDate(1066, 9, 1)); got != 38 {
		t.Errorf("生日当天年龄 = %d, want 38", got)
	}
	if got := birth.AgeAt(NewGameDate(1066, 8, 31)); got != 37 {
		t.Errorf("生日前一天年龄 = %d, want 37", got)
	}
}

func TestDaysUntilAndOrdering(t *testing.T) {
	a := NewGameDate(1066, 1, 1)
	b := NewGameDate(1066, 1, 2)
	if got := a.DaysUntil(b); got != 1 {
		t.Errorf("DaysUntil = %d, want 1", got)
	}
	if !a.Before(b) || !b.After(a) || !a.Equal(NewGameDate(1066, 1, 1)) {
		t.Errorf("日期序比较不正确")
	}
}

func TestIsLeapYear(t *testing.T) {
	if !NewGameDate(1064, 1, 1).IsLeapYear() {
		t.Error("1064 应为闰年")
	}
	if NewGameDate(1066, 1, 1).IsLeapYear() {
		t.Error("1066 不应为闰年")
	}
}
