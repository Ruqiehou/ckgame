use crate::core::calendar::{date::GameDate, season::season_name};
use crate::core::gender::gender_name;
use crate::core::stats::AttributeSet;
use crate::core::traits::find;
use crate::core::title_tier::TitleTier;

pub fn run() {
    println!("CKGame Rust 版 - 演示启动");
    let d = GameDate::new(1066, 1, 1);
    println!("初始日期: {}", d);
    println!("季节: {}", season_name(d.season()));

    println!("\n五维属性示例: ");
    let mut attrs = AttributeSet::default();
    attrs.add("d", 2);
    attrs.add("m", 3);
    println!("外交={} 军事={}", attrs.diplomacy, attrs.martial);

    println!("\n特性示例: ");
    if let Some(t) = find("brave") {
        println!("ID={} 名={}", t.id, t.name);
    }

    println!("\n性别示例: {} / {}", gender_name(crate::core::gender::Gender::Male), gender_name(crate::core::gender::Gender::Female));

    println!("\n头衔等级示例: ");
    println!("伯爵={} 威望={}", TitleTier::County.rank_name(), TitleTier::County.creation_cost());

    println!("\n[按回车退出]");
    let mut _input = String::new();
    std::io::stdin().read_line(&mut _input).ok();
}