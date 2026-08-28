#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum Season {
    Spring,
    Summer,
    Autumn,
    Winter,
}

pub fn season_name(s: Season) -> &'static str {
    match s {
        Season::Spring => "春",
        Season::Summer => "夏",
        Season::Autumn => "秋",
        Season::Winter => "冬",
    }
}