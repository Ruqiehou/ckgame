use super::season::Season;
use chrono::NaiveDate;

#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub struct GameDate {
    pub year: i32,
    pub month: u32,
    pub day: u32,
}

impl GameDate {
    pub fn new(year: i32, month: u32, day: u32) -> Self {
        GameDate { year, month, day }
    }

    pub fn from_array(arr: &[i64]) -> Self {
        if arr.len() == 3 {
            GameDate { year: arr[0] as i32, month: arr[1] as u32, day: arr[2] as u32 }
        } else {
            GameDate::new(1066, 1, 1)
        }
    }

    pub fn to_array(&self) -> Vec<i64> {
        vec![self.year as i64, self.month as i64, self.day as i64]
    }

    pub fn to_naive_date(&self) -> Option<NaiveDate> {
        NaiveDate::from_ymd_opt(self.year, self.month, self.day)
    }

    pub fn season(&self) -> Season {
        match self.month {
            3..=5 => Season::Spring,
            6..=8 => Season::Summer,
            9..=11 => Season::Autumn,
            _ => Season::Winter,
        }
    }

    pub fn is_leap_year(&self) -> bool {
        (self.year % 4 == 0 && self.year % 100 != 0) || self.year % 400 == 0
    }

    pub fn days_in_month(&self) -> u32 {
        match self.month {
            1 | 3 | 5 | 7 | 8 | 10 | 12 => 31,
            4 | 6 | 9 | 11 => 30,
            _ => if self.is_leap_year() { 29 } else { 28 },
        }
    }

    pub fn advance_days(&self, days: i64) -> Self {
        if let Some(naive) = self.to_naive_date() {
            if let Some(result) = naive.checked_add_signed(chrono::Duration::days(days)) {
                GameDate { year: result.year(), month: result.month(), day: result.day() }
            } else {
                *self
            }
        } else {
            *self
        }
    }

    pub fn advance_one_day(&self) -> Self {
        self.advance_days(1)
    }

    pub fn to_ordinal(&self) -> i64 {
        if let Some(naive) = self.to_naive_date() {
            naive.num_days_from_ce()
        } else {
            0
        }
    }

    pub fn age_at(&self, birth: Self) -> i32 {
        if let Some(naive) = self.to_naive_date() {
            if let Some(birth_naive) = birth.to_naive_date() {
                let mut age = naive.year() - birth_naive.year();
                if naive.month() < birth_naive.month() || (naive.month() == birth_naive.month() && naive.day() < birth_naive.day()) {
                    age -= 1;
                }
                if age < 0 { age = 0 }
                return age;
            }
        }
        0
    }
}

impl std::fmt::Display for GameDate {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "{:04}-{:02}-{:02}", self.year, self.month, self.day)
    }
}