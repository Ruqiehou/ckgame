package core

// Gender 角色性别枚举，对应 Java 版 Gender。
type Gender int

const (
	Male Gender = iota
	Female
)

// String 实现 fmt.Stringer。
func (g Gender) String() string {
	switch g {
	case Male:
		return "男"
	case Female:
		return "女"
	default:
		return "未知"
	}
}
