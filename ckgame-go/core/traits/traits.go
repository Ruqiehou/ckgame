// Package traits 提供角色特质系统。
// 对应 Java 版 com.ckgame.core.traits。
package traits

import "github.com/ckgame/ckgame-go/core/stats"

// TraitKind 特质类别。
type TraitKind int

const (
	Personality TraitKind = iota
	Education
	Congenital
	Lifestyle
	Health
	Commander
	Cultural
	Religious
)

// String 实现 fmt.Stringer。
func (t TraitKind) String() string {
	switch t {
	case Personality:
		return "性格"
	case Education:
		return "教育"
	case Congenital:
		return "先天"
	case Lifestyle:
		return "生活方式"
	case Health:
		return "健康"
	case Commander:
		return "统帅"
	case Cultural:
		return "文化"
	case Religious:
		return "宗教"
	default:
		return "未知"
	}
}

// Trait 角色特质定义。
type Trait struct {
	ID           int               `json:"id"`
	Name         string            `json:"name"`
	Kind         TraitKind         `json:"kind"`
	AttrBonus    stats.AttributeSet `json:"attrBonus"`
	OpinionSelf  int               `json:"opinionSelf"`
	OpinionOthers int               `json:"opinionOthers"`
	FertilityMod float64           `json:"fertilityMod"`
	HealthMod    float64           `json:"healthMod"`
	Description  string            `json:"description"`
}

// NewTrait 构造特质。
func NewTrait(id int, name string, kind TraitKind, attrBonus stats.AttributeSet,
	opinionSelf, opinionOthers int, fertilityMod, healthMod float64, description string) Trait {
	return Trait{
		ID:           id,
		Name:         name,
		Kind:         kind,
		AttrBonus:    attrBonus,
		OpinionSelf:  opinionSelf,
		OpinionOthers: opinionOthers,
		FertilityMod: fertilityMod,
		HealthMod:    healthMod,
		Description:  description,
	}
}

// SimpleTrait 构造无额外效果的特质。
func SimpleTrait(id int, name string, kind TraitKind) Trait {
	return NewTrait(id, name, kind, stats.Zero(), 0, 0, 0.0, 0.0, "")
}

// Builtins 返回内置特质表（12个）。
func Builtins() []Trait {
	return []Trait{
		{
			ID:           1,
			Name:         "勇敢",
			Kind:         Personality,
			AttrBonus:    stats.NewAttributeSet(0, 1, 0, 0, 0, 2),
			OpinionSelf:  0,
			OpinionOthers: 5,
			FertilityMod: 0.0,
			HealthMod:    0.0,
			Description:  "",
		},
		{
			ID:           2,
			Name:         "狡诈",
			Kind:         Personality,
			AttrBonus:    stats.NewAttributeSet(-1, 0, 0, 3, 0, 0),
			OpinionSelf:  0,
			OpinionOthers: -5,
			FertilityMod: 0.0,
			HealthMod:    0.0,
			Description:  "",
		},
		{
			ID:           3,
			Name:         "公正",
			Kind:         Personality,
			AttrBonus:    stats.NewAttributeSet(2, 0, 1, 0, 0, 0),
			OpinionSelf:  0,
			OpinionOthers: 10,
			FertilityMod: 0.0,
			HealthMod:    0.0,
			Description:  "",
		},
		{
			ID:           4,
			Name:         "贪婪",
			Kind:         Personality,
			AttrBonus:    stats.NewAttributeSet(-1, 0, 2, 0, 0, 0),
			OpinionSelf:  0,
			OpinionOthers: -8,
			FertilityMod: 0.0,
			HealthMod:    0.0,
			Description:  "",
		},
		{
			ID:           5,
			Name:         "慷慨",
			Kind:         Personality,
			AttrBonus:    stats.NewAttributeSet(2, 0, -1, 0, 0, 0),
			OpinionSelf:  0,
			OpinionOthers: 8,
			FertilityMod: 0.0,
			HealthMod:    0.0,
			Description:  "",
		},
		{
			ID:           6,
			Name:         "军事天才",
			Kind:         Commander,
			AttrBonus:    stats.NewAttributeSet(0, 4, 0, 0, 0, 2),
			OpinionSelf:  0,
			OpinionOthers: 5,
			FertilityMod: 0.0,
			HealthMod:    0.0,
			Description:  "",
		},
		{
			ID:           7,
			Name:         "病弱",
			Kind:         Health,
			AttrBonus:    stats.NewAttributeSet(0, 0, 0, 0, 0, -2),
			OpinionSelf:  0,
			OpinionOthers: 0,
			FertilityMod: -0.1,
			HealthMod:    -0.5,
			Description:  "",
		},
		{
			ID:           8,
			Name:         "博学",
			Kind:         Education,
			AttrBonus:    stats.NewAttributeSet(0, 0, 0, 0, 3, 0),
			OpinionSelf:  0,
			OpinionOthers: 3,
			FertilityMod: 0.0,
			HealthMod:    0.0,
			Description:  "",
		},
		{
			ID:           9,
			Name:         "野心勃勃",
			Kind:         Personality,
			AttrBonus:    stats.NewAttributeSet(0, 1, 1, 1, 0, 0),
			OpinionSelf:  0,
			OpinionOthers: -3,
			FertilityMod: 0.0,
			HealthMod:    0.0,
			Description:  "",
		},
		{
			ID:           10,
			Name:         "忠诚",
			Kind:         Personality,
			AttrBonus:    stats.NewAttributeSet(1, 0, 0, 0, 0, 0),
			OpinionSelf:  0,
			OpinionOthers: 12,
			FertilityMod: 0.0,
			HealthMod:    0.0,
			Description:  "",
		},
		{
			ID:           11,
			Name:         "残忍",
			Kind:         Personality,
			AttrBonus:    stats.NewAttributeSet(-2, 0, 0, 2, 0, 0),
			OpinionSelf:  0,
			OpinionOthers: -15,
			FertilityMod: 0.0,
			HealthMod:    0.0,
			Description:  "",
		},
		{
			ID:           12,
			Name:         "魅力四射",
			Kind:         Personality,
			AttrBonus:    stats.NewAttributeSet(3, 0, 0, 0, 0, 0),
			OpinionSelf:  0,
			OpinionOthers: 10,
			FertilityMod: 0.0,
			HealthMod:    0.0,
			Description:  "",
		},
	}
}

// GetByID 根据 ID 查找特质，返回 nil 表示未找到。
func GetByID(id int) *Trait {
	for _, t := range Builtins() {
		if t.ID == id {
			return &t
		}
	}
	return nil
}