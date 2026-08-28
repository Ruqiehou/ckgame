pub mod balance {
    pub const BASE_LEVY_SIZE: i32 = 500;
    pub const LEVY_RECOVERY_PER_MONTH: f64 = 0.05;
    pub const SUPPLY_LIMIT_BASE: f64 = 1000.0;
    pub const SUPPLY_BASE_COST: f64 = 0.1;

    pub const BASE_TAX_RATE: f64 = 0.2;
    pub const FESTIVAL_COST: i32 = 200;
    pub const KNIGHT_COST: i32 = 50;
    pub const BUILDING_BASE_COST: i32 = 100;
    pub const DEVELOP_COST_PER_LEVEL: i32 = 150;

    pub const COUNCIL_TASK_DURATION_MONTHS: i32 = 3;
    pub const DIPLOMACY_BASE_SUCCESS: f64 = 0.5;
    pub const BASE_RELATION_CHANGE: i32 = 10;
    pub const CLAIM_FABRICATION_COST: i32 = 150;
    pub const CLAIM_FABRICATION_TIME_MONTHS: i32 = 3;

    pub const WAR_SCORE_WHITE_PEACE_THRESHOLD: i32 = -50;
    pub const WAR_SCORE_ENFORCE_THRESHOLD: i32 = 100;
    pub const WAR_DECAY_PER_MONTH: f64 = 0.5;

    pub const FACTION_POWER_THRESHOLD: f64 = 1.2;
    pub const SCHEME_BASE_PROGRESS: i32 = 10;
    pub const SCHEME_BASE_EXPOSURE: i32 = 15;
}