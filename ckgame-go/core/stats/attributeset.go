// Package stats 提供角色六维属性系统。
// 对应 Java 版 com.ckgame.core.stats。
package stats

import "fmt"

// AttributeSet 六维属性集合，不可变，所有修改操作返回新对象。
type AttributeSet struct {
	Diplomacy  int `json:"diplomacy"`
	Martial    int `json:"martial"`
	Stewardship int `json:"stewardship"`
	Intrigue   int `json:"intrigue"`
	Learning   int `json:"learning"`
	Prowess    int `json:"prowess"`
}

// NewAttributeSet 构造属性集。
func NewAttributeSet(diplomacy, martial, stewardship, intrigue, learning, prowess int) AttributeSet {
	return AttributeSet{
		Diplomacy:  diplomacy,
		Martial:    martial,
		Stewardship: stewardship,
		Intrigue:   intrigue,
		Learning:   learning,
		Prowess:    prowess,
	}
}

// Zero 返回全零属性集。
func Zero() AttributeSet {
	return NewAttributeSet(0, 0, 0, 0, 0, 0)
}

// Defaults 返回默认属性集（全 8）。
func Defaults() AttributeSet {
	return NewAttributeSet(8, 8, 8, 8, 8, 8)
}

// Add 返回相加后的新属性集。
func (a AttributeSet) Add(other AttributeSet) AttributeSet {
	return NewAttributeSet(
		a.Diplomacy+other.Diplomacy,
		a.Martial+other.Martial,
		a.Stewardship+other.Stewardship,
		a.Intrigue+other.Intrigue,
		a.Learning+other.Learning,
		a.Prowess+other.Prowess,
	)
}

// Clamp 将各值限定在 [lo, hi] 区间。
func (a AttributeSet) Clamp(lo, hi int) AttributeSet {
	return NewAttributeSet(
		clampInt(a.Diplomacy, lo, hi),
		clampInt(a.Martial, lo, hi),
		clampInt(a.Stewardship, lo, hi),
		clampInt(a.Intrigue, lo, hi),
		clampInt(a.Learning, lo, hi),
		clampInt(a.Prowess, lo, hi),
	)
}

func clampInt(v, lo, hi int) int {
	if v < lo {
		return lo
	}
	if v > hi {
		return hi
	}
	return v
}

// Total 返回属性总和。
func (a AttributeSet) Total() int {
	return a.Diplomacy + a.Martial + a.Stewardship + a.Intrigue + a.Learning + a.Prowess
}

// String 调试用文本表示。
func (a AttributeSet) String() string {
	return fmt.Sprintf("AttributeSet[dip=%d,mar=%d,stew=%d,int=%d,lear=%d,prow=%d]",
		a.Diplomacy, a.Martial, a.Stewardship, a.Intrigue, a.Learning, a.Prowess)
}