#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum Gender {
    Male,
    Female,
}

pub fn gender_name(g: Gender) -> &'static str {
    match g {
        Gender::Male => "男",
        Gender::Female => "女",
    }
}