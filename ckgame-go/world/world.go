package world

import (
    "encoding/json"
    "fmt"
    "os"
    "path/filepath"
    "strings"

    "github.com/ckgame/ckgame-go/core"
    "github.com/ckgame/ckgame-go/core/calendar"
    "github.com/ckgame/ckgame-go/core/stats"
)

type Dynasty struct { Key, Name, Motto string; Color []int `json:"color,omitempty"` }
type County struct { Key, Name, Terrain string; Development, Levies, Fort, PortLevel int; Tax float64; HasPort, TradeRouteProtected bool; TradeRouteMaintenanceLevel, TradeRouteProtectionLevel int; Holder string; OccupiedBy string }
type Title struct { Key, Name string; Tier core.TitleTier; Holder string; Counties []string; Liege string }
type Character struct { Key, Name, Dynasty, Culture, Faith string; Gender core.Gender; Birth calendar.GameDate; Attributes stats.AttributeSet; Gold, Prestige, Stress float64; Traits []int; Alive bool; Opinions map[string]int; Spouse string; Children []string }
type TradeRoute struct {
	From         string  `json:"from"`
	To           string  `json:"to"`
	TradeVolume  float64 `json:"trade_volume"`
	ExchangeRate float64 `json:"exchange_rate"`
}
type World struct { Date calendar.GameDate; Dynasties map[string]Dynasty; Counties map[string]*County; Titles map[string]*Title; Characters map[string]*Character; Connections map[string][]string; TradeRoutes []TradeRoute; Logs []string; Messages []string }

func New() *World { return &World{Date: calendar.NewGameDate(1066,1,1), Dynasties: map[string]Dynasty{}, Counties: map[string]*County{}, Titles: map[string]*Title{}, Characters: map[string]*Character{}, Connections: map[string][]string{}} }
func (w *World) Log(s string) { w.Logs = append(w.Logs, fmt.Sprintf("%s %s", w.Date.String(), s)); if len(w.Logs)>100 { w.Logs=w.Logs[len(w.Logs)-100:] } }
func (w *World) Character(id string) (*Character,error) { c,ok:=w.Characters[id]; if !ok { return nil,fmt.Errorf("character not found: %s",id) }; return c,nil }
func (w *World) County(id string) (*County,error) { c,ok:=w.Counties[id]; if !ok { return nil,fmt.Errorf("county not found: %s",id) }; return c,nil }
func (w *World) CanMove(from,to string) bool { for _, n:=range w.Connections[from] { if n==to{return true} }; return false }

// LoadScenario accepts the JSON schema used by ckgame-java and ckgame-ts.
func LoadScenario(path string) (*World,error) { b,err:=os.ReadFile(path); if err!=nil{return nil,err}; var raw struct { StartDate []int `json:"start_date"`; Dynasties []Dynasty `json:"dynasties"`; Counties []struct {Key string `json:"key"`;Name string `json:"name"`;Terrain string `json:"terrain"`;Development int `json:"development"`;Levies int `json:"levies"`;Fort int `json:"fort"`;PortLevel int `json:"port_level"`;Tax float64 `json:"tax"`;HasPort bool `json:"has_port"`;TradeRouteProtected bool `json:"trade_route_protected"`;TradeRouteMaintenanceLevel int `json:"trade_route_maintenance_level"`;TradeRouteProtectionLevel int `json:"trade_route_protection_level"`} `json:"counties"`; Connections [][]string `json:"connections"`; TradeRoutes []TradeRoute `json:"trade_routes"`; Titles []struct {Key,Name,Tier string} `json:"titles"`; TitleCounties map[string][]string `json:"title_counties"`; Vassals [][]string `json:"vassals"`; Characters []struct {Key,Name,Dynasty,Gender string; Birth []int; Culture,Faith int; Attrs []int; Gold,Prestige float64; Traits []int} `json:"characters"`; Parents [][]string `json:"parents"`;Marriages [][]string `json:"marriages"`;Grants [][]string `json:"grants"`;PostGrantsVassals [][]string `json:"post_grants_vassals"`; Opinions [][]interface{} `json:"opinions"`; Logs []string `json:"logs"` }; if err=json.Unmarshal(b,&raw);err!=nil{return nil,err}; w:=New(); w.Date=calendar.ParseGameDate(raw.StartDate); for _,d:=range raw.Dynasties{w.Dynasties[d.Key]=d}; for _,c:=range raw.Counties{w.Counties[c.Key]=&County{Key:c.Key,Name:c.Name,Terrain:c.Terrain,Development:c.Development,Levies:c.Levies,Fort:c.Fort,Tax:c.Tax,HasPort:c.HasPort,PortLevel:c.PortLevel,TradeRouteProtected:c.TradeRouteProtected,TradeRouteMaintenanceLevel:c.TradeRouteMaintenanceLevel,TradeRouteProtectionLevel:c.TradeRouteProtectionLevel}}; for _,p:=range raw.Connections{if len(p)==2{w.Connections[p[0]]=append(w.Connections[p[0]],p[1]);w.Connections[p[1]]=append(w.Connections[p[1]],p[0])}}; w.TradeRoutes=raw.TradeRoutes; for _,t:=range raw.Titles{w.Titles[t.Key]=&Title{Key:t.Key,Name:t.Name,Tier:parseTier(t.Tier),Counties:append([]string{},raw.TitleCounties[t.Key]...)}}; for _,c:=range raw.Characters{g:=core.Male;if strings.EqualFold(c.Gender,"FEMALE"){g=core.Female}; attrs:=stats.Defaults();if len(c.Attrs)>=6{attrs=stats.NewAttributeSet(c.Attrs[0],c.Attrs[1],c.Attrs[2],c.Attrs[3],c.Attrs[4],c.Attrs[5])};w.Characters[c.Key]=&Character{Key:c.Key,Name:c.Name,Dynasty:c.Dynasty,Gender:g,Birth:calendar.ParseGameDate(c.Birth),Culture:fmt.Sprint(c.Culture),Faith:fmt.Sprint(c.Faith),Attributes:attrs,Gold:c.Gold,Prestige:c.Prestige,Traits:c.Traits,Alive:true,Opinions:map[string]int{}}}; for _,p:=range raw.Grants{if len(p)==2{if t:=w.Titles[p[0]];t!=nil{t.Holder=p[1]}; for _,c:=range w.Counties{if contains(w.Titles[p[0]].Counties,c.Key){c.Holder=p[1]}}}}; for _,p:=range raw.Parents{if len(p)==3{if ch:=w.Characters[p[0]];ch!=nil{ch.Children=[]string{};for _,x:=range []string{p[1],p[2]}{if parent:=w.Characters[x];parent!=nil{parent.Children=append(parent.Children,p[0])}}}}}; for _,p:=range raw.Marriages{if len(p)==2{if a:=w.Characters[p[0]];a!=nil{a.Spouse=p[1]};if a:=w.Characters[p[1]];a!=nil{a.Spouse=p[0]}}}; for _,p:=range raw.Vassals{if len(p)==2{if t:=w.Titles[p[0]];t!=nil{t.Liege=p[1]}}}; for _,p:=range raw.PostGrantsVassals{if len(p)==2{if t:=w.Titles[p[0]];t!=nil{t.Liege=p[1]}}}; w.Logs=append(w.Logs,raw.Logs...); return w,nil }
func parseTier(s string) core.TitleTier { switch strings.ToUpper(s){case "BARONY":return core.Barony;case "COUNTY":return core.County;case "DUCHY":return core.Duchy;case "KINGDOM":return core.Kingdom;case "EMPIRE":return core.Empire};return core.County }
func contains(a []string,v string)bool{for _,x:=range a{if x==v{return true}};return false}
func ScenarioPath() string { paths:=[]string{"data/scenarios/1066.json","../ckgame-ts/data/scenarios/1066.json","../../ckgame-ts/data/scenarios/1066.json"};for _,p:=range paths{if x,_:=filepath.Abs(p); func()bool{_,e:=os.Stat(x);return e==nil}(){return x}};return paths[0] }
