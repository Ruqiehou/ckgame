package core

// TitleTier 头衔等级，对应 Java 版 TitleTier。
type TitleTier int

const (
	Barony  TitleTier = 1 // 男爵
	County  TitleTier = 2 // 伯爵
	Duchy   TitleTier = 3 // 公爵
	Kingdom TitleTier = 4 // 国王
	Empire  TitleTier = 5 // 皇帝
)

// RankName 返回等级对应的中文爵位名。
func (t TitleTier) RankName() string {
	switch t {
	case Barony:
		return "男爵"
	case County:
		return "伯爵"
	case Duchy:
		return "公爵"
	case Kingdom:
		return "国王"
	case Empire:
		return "皇帝"
	default:
		return "未知"
	}
}

// Prefix 返回头衔前缀（用于头衔命名）。
func (t TitleTier) Prefix() string {
	switch t {
	case Barony:
		return "b"
	case County:
		return "c"
	case Duchy:
		return "d"
	case Kingdom:
		return "k"
	case Empire:
		return "e"
	default:
		return "?"
	}
}

// CreationCost 创建该级头衔所需威望。
func (t TitleTier) CreationCost() float64 {
	switch t {
	case Barony:
		return 0.0
	case County:
		return 50.0
	case Duchy:
		return 200.0
	case Kingdom:
		return 500.0
	case Empire:
		return 1000.0
	default:
		return 0.0
	}
}

// IsDestroyable 公爵及以上头衔可被销毁。
func (t TitleTier) IsDestroyable() bool {
	return t >= Duchy
}
