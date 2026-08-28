#[derive(Debug, Clone, Default)]
pub struct AttributeSet {
    pub diplomacy: i32,
    pub martial: i32,
    pub stewardship: i32,
    pub intrigue: i32,
    pub learning: i32,
}

impl AttributeSet {
    pub fn total(&self) -> i32 {
        self.diplomacy + self.martial + self.stewardship + self.intrigue + self.learning
    }

    pub fn get(&self, name: &str) -> i32 {
        match name.chars().next() {
            Some('d') => self.diplomacy,
            Some('m') => self.martial,
            Some('s') => self.stewardship,
            Some('i') => self.intrigue,
            Some('l') => self.learning,
            _ => 0,
        }
    }

    pub fn add(&mut self, name: &str, value: i32) {
        let target = match name.chars().next() {
            Some('d') => &mut self.diplomacy,
            Some('m') => &mut self.martial,
            Some('s') => &mut self.stewardship,
            Some('i') => &mut self.intrigue,
            Some('l') => &mut self.learning,
            _ => return,
        };
        *target = (*target + value).clamp(0, 100);
    }
}