#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum TitleTier {
    Barony = 1,
    County = 2,
    Duchy = 3,
    Kingdom = 4,
    Empire = 5,
}

impl TitleTier {
    pub fn rank_name(&self) -> &'static str {
        match self {
            TitleTier::Barony => "男爵",
            TitleTier::County => "伯爵",
            TitleTier::Duchy => "公爵",
            TitleTier::Kingdom => "国王",
            TitleTier::Empire => "皇帝",
        }
    }

    pub fn tier_prefix(&self) -> &'static str {
        match self {
            TitleTier::Barony => "b",
            TitleTier::County => "c",
            TitleTier::Duchy => "d",
            TitleTier::Kingdom => "k",
            TitleTier::Empire => "e",
        }
    }

    pub fn creation_cost(&self) -> f64 {
        match self {
            TitleTier::Barony => 0.0,
            TitleTier::County => 50.0,
            TitleTier::Duchy => 200.0,
            TitleTier::Kingdom => 500.0,
            TitleTier::Empire => 1000.0,
        }
    }

    pub fn is_destroyable(&self) -> bool {
        (*self as u8) >= (TitleTier::Duchy as u8)
    }
}