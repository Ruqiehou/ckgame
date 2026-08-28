#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum Kind {
    Personality,
    Physical,
    Lifestyle,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct Trait {
    pub id: String,
    pub name: String,
    pub kind: Kind,
}

pub fn find(id: &str) -> Option<&'static Trait> {
    ALL_TRAITS.iter().find(|t| t.id == id)
}

pub fn all() -> &'static [Trait] {
    ALL_TRAITS
}

const ALL_TRAITS: &[Trait] = &[
    Trait { id: "brave".to_string(), name: "勇敢".to_string(), kind: Kind::Personality },
    Trait { id: "craven".to_string(), name: "怯懦".to_string(), kind: Kind::Personality },
    Trait { id: "just".to_string(), name: "公正".to_string(), kind: Kind::Personality },
    Trait { id: "arbitrary".to_string(), name: "任性".to_string(), kind: Kind::Personality },
    Trait { id: "ambitious".to_string(), name: "野心勃勃".to_string(), kind: Kind::Personality },
    Trait { id: "content".to_string(), name: "安于现状".to_string(), kind: Kind::Personality },
    Trait { id: "temperate".to_string(), name: "节制".to_string(), kind: Kind::Personality },
    Trait { id: "gluttonous".to_string(), name: "暴食".to_string(), kind: Kind::Personality },
    Trait { id: "charitable".to_string(), name: "仁慈".to_string(), kind: Kind::Personality },
    Trait { id: "greedy".to_string(), name: "贪婪".to_string(), kind: Kind::Personality },
    Trait { id: "diligent".to_string(), name: "勤奋".to_string(), kind: Kind::Personality },
    Trait { id: "lazy".to_string(), name: "懒惰".to_string(), kind: Kind::Personality },
    Trait { id: "patient".to_string(), name: "耐心".to_string(), kind: Kind::Personality },
    Trait { id: "wroth".to_string(), name: "暴怒".to_string(), kind: Kind::Personality },
    Trait { id: "humble".to_string(), name: "谦逊".to_string(), kind: Kind::Personality },
    Trait { id: "proud".to_string(), name: "傲慢".to_string(), kind: Kind::Personality },
    Trait { id: "honest".to_string(), name: "诚实".to_string(), kind: Kind::Personality },
    Trait { id: "deceitful".to_string(), name: "狡诈".to_string(), kind: Kind::Personality },
    Trait { id: "gregarious".to_string(), name: "合群".to_string(), kind: Kind::Personality },
    Trait { id: "shy".to_string(), name: "害羞".to_string(), kind: Kind::Personality },
    Trait { id: "zealous".to_string(), name: "狂热".to_string(), kind: Kind::Personality },
    Trait { id: "cynical".to_string(), name: "愤世嫉俗".to_string(), kind: Kind::Personality },
    Trait { id: "strong".to_string(), name: "强壮".to_string(), kind: Kind::Physical },
    Trait { id: "weak".to_string(), name: "虚弱".to_string(), kind: Kind::Physical },
    Trait { id: "beautiful".to_string(), name: "貌美".to_string(), kind: Kind::Physical },
    Trait { id: "scarred".to_string(), name: "有伤疤".to_string(), kind: Kind::Physical },
    Trait { id: "brilliant".to_string(), name: "聪慧".to_string(), kind: Kind::Physical },
    Trait { id: "slow".to_string(), name: "迟钝".to_string(), kind: Kind::Physical },
];