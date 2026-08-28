package world

import (
	"os"
	"path/filepath"
	"testing"
)

const minimalScenario = `{
  "start_date": [1066, 10, 14],
  "dynasties": [{"key": "norm", "name": "诺曼底", "motto": "勇气"}],
  "counties": [
    {"key": "rouen", "name": "鲁昂", "terrain": "plains", "development": 3, "levies": 500, "fort": 2, "tax": 1.5, "has_port": true, "port_level": 2},
    {"key": "bayeux", "name": "贝叶", "terrain": "hills", "development": 2, "levies": 300, "fort": 1, "tax": 1.0}
  ],
  "connections": [["rouen", "bayeux"]],
  "trade_routes": [{"from": "rouen", "to": "bayeux", "trade_volume": 12.5, "exchange_rate": 1.2}],
  "titles": [
    {"key": "k_england", "name": "英格兰", "tier": "KINGDOM"},
    {"key": "d_normandy", "name": "诺曼底公国", "tier": "DUCHY"}
  ],
  "title_counties": {"k_england": ["rouen"], "d_normandy": ["bayeux"]},
  "characters": [
    {"key": "william", "name": "威廉", "dynasty": "norm", "gender": "MALE", "birth": [1028, 9, 1], "gold": 500, "prestige": 100, "attrs": [8, 9, 7, 6, 5]},
    {"key": "harold", "name": "哈罗德", "dynasty": "norm", "gender": "MALE", "birth": [1022, 1, 1], "gold": 400},
    {"key": "matilda", "name": "玛蒂尔达", "dynasty": "norm", "gender": "FEMALE", "birth": [1031, 1, 1]}
  ],
  "grants": [["k_england", "harold"], ["d_normandy", "william"]],
  "vassals": [["d_normandy", "k_england"]],
  "marriages": [["william", "matilda"]],
  "parents": [],
  "opinions": [["harold", "william", -40]],
  "logs": ["测试日志"]
}`

func writeTempScenario(t *testing.T) string {
	t.Helper()
	dir := t.TempDir()
	path := filepath.Join(dir, "1066.json")
	if err := os.WriteFile(path, []byte(minimalScenario), 0o644); err != nil {
		t.Fatalf("写入临时场景失败: %v", err)
	}
	return path
}

func TestLoadScenario(t *testing.T) {
	w, err := LoadScenario(writeTempScenario(t))
	if err != nil {
		t.Fatalf("LoadScenario 失败: %v", err)
	}

	if got := w.Date.String(); got != "1066-10-14" {
		t.Errorf("起始日期 = %s, want 1066-10-14", got)
	}

	// 授衔：头衔与省份持有者同步
	if k := w.Titles["k_england"]; k.Holder != "harold" {
		t.Errorf("k_england 持有者 = %q, want harold", k.Holder)
	}
	if c := w.Counties["rouen"]; c.Holder != "harold" {
		t.Errorf("rouen 持有者 = %q, want harold", c.Holder)
	}

	// snake_case 字段（回归：缺少 json 标签时这些字段静默丢失）
	if c := w.Counties["rouen"]; !c.HasPort || c.PortLevel != 2 {
		t.Errorf("has_port/port_level 加载失败: %+v", c)
	}
	if len(w.TradeRoutes) != 1 || w.TradeRoutes[0].TradeVolume != 12.5 {
		t.Errorf("trade_routes 加载失败: %+v", w.TradeRoutes)
	}

	// 婚姻双向
	if c := w.Characters["william"]; c.Spouse != "matilda" {
		t.Errorf("william 配偶 = %q, want matilda", c.Spouse)
	}
	if c := w.Characters["matilda"]; c.Spouse != "william" {
		t.Errorf("matilda 配偶 = %q, want william", c.Spouse)
	}

	// 宗主链（回归：vassals/post_grants_vassals 曾误用 parents 标签）
	if d := w.Titles["d_normandy"]; d.Liege != "k_england" {
		t.Errorf("d_normandy 宗主 = %q, want k_england", d.Liege)
	}

	// 邻接图双向
	if !w.CanMove("rouen", "bayeux") || !w.CanMove("bayeux", "rouen") {
		t.Error("邻接图应双向可通行")
	}

	// 性别与默认属性
	if c := w.Characters["matilda"]; c.Gender.String() != "女" {
		t.Errorf("matilda 性别 = %v, want 女", c.Gender)
	}
	if c := w.Characters["harold"]; c.Attributes.Total() <= 0 {
		t.Errorf("未提供 attrs 时应使用默认属性: %+v", c.Attributes)
	}
}

func TestLoadScenarioRejectsBrokenJSON(t *testing.T) {
	dir := t.TempDir()
	path := filepath.Join(dir, "broken.json")
	if err := os.WriteFile(path, []byte("{not json"), 0o644); err != nil {
		t.Fatalf("写入失败: %v", err)
	}
	if _, err := LoadScenario(path); err == nil {
		t.Error("损坏的 JSON 应返回错误")
	}
}

func TestLoadRealScenarioIfPresent(t *testing.T) {
	path := ScenarioPath()
	if _, err := os.Stat(path); err != nil {
		t.Skipf("找不到共享场景文件 %s，跳过", path)
	}
	w, err := LoadScenario(path)
	if err != nil {
		t.Fatalf("加载共享场景失败: %v", err)
	}
	if len(w.Characters) == 0 || len(w.Counties) == 0 {
		t.Error("共享场景人物/省份数量不应为 0")
	}
	// 确认 snake_case 字段在真实场景中加载
	hasPort := false
	for _, c := range w.Counties {
		if c.HasPort {
			hasPort = true
		}
	}
	if !hasPort {
		t.Error("真实场景中应有带港口的省份（has_port 字段未加载？）")
	}
	// 确认授衔与宗主链
	if w.Titles["k_england"].Holder != "harold" {
		t.Errorf("k_england 持有者 = %q, want harold", w.Titles["k_england"].Holder)
	}
}
