package military

import (
    "fmt"
    "github.com/ckgame/ckgame-go/world"
)

type ArmyStatus string
const ( ArmyRaised ArmyStatus="raised"; ArmyMoving ArmyStatus="moving"; ArmyBesieging ArmyStatus="besieging"; ArmyDisbanded ArmyStatus="disbanded" )
type Army struct { ID, Owner, Location string; Soldiers, MaxSoldiers int; Morale, Supply float64; Status ArmyStatus }
type War struct { ID, Attacker, Defender, Target string; Score float64; Months int; Active bool; Started string; TruceUntil string }
type Manager struct { Armies map[string]*Army; Wars map[string]*War; nextArmy,nextWar int }
func NewManager()*Manager{return &Manager{Armies:map[string]*Army{},Wars:map[string]*War{}}}
func(m *Manager) Raise(w *world.World,owner,county string)(*Army,error){c,e:=w.County(county);if e!=nil{return nil,e};m.nextArmy++;a:=&Army{ID:fmt.Sprintf("army_%d",m.nextArmy),Owner:owner,Location:county,Soldiers:c.Levies,MaxSoldiers:c.Levies,Morale:100,Supply:100,Status:ArmyRaised};m.Armies[a.ID]=a;w.Log(fmt.Sprintf("%s 在 %s 集结了 %d 人",owner,c.Name,a.Soldiers));return a,nil}
func(m *Manager) Move(w *world.World,id,to string)error{a,ok:=m.Armies[id];if !ok{return fmt.Errorf("army not found: %s",id)};if !w.CanMove(a.Location,to){return fmt.Errorf("无法从 %s 移动到 %s",a.Location,to)};a.Location=to;a.Status=ArmyMoving;a.Supply-=5;if a.Supply<0{a.Supply=0};w.Log(fmt.Sprintf("军队 %s 行军至 %s",id,to));return nil}
func(m *Manager) Disband(id string)error{a,ok:=m.Armies[id];if !ok{return fmt.Errorf("army not found: %s",id)};a.Status=ArmyDisbanded;a.Soldiers=0;return nil}
func(m *Manager) Declare(w *world.World,attacker,defender,target string)(*War,error){m.nextWar++;id:=fmt.Sprintf("war_%d",m.nextWar);war:=&War{ID:id,Attacker:attacker,Defender:defender,Target:target,Active:true,Started:w.Date.String()};m.Wars[id]=war;w.Log(fmt.Sprintf("%s 向 %s 宣战，战争目标：%s",attacker,defender,target));return war,nil}
func(m *Manager) WhitePeace(w *world.World,id string)error{war,ok:=m.Wars[id];if !ok{return fmt.Errorf("war not found: %s",id)};war.Active=false;war.TruceUntil=w.Date.AddMonths(36).String();w.Log("战争以白和结束");return nil}
func(m *Manager) TickMonth(w *world.World){for _,a:=range m.Armies{if a.Status==ArmyDisbanded{continue};if a.Supply<100{a.Supply+=2;if a.Supply>100{a.Supply=100}};if a.Supply<25{a.Morale-=3}};for _,war:=range m.Wars{if war.Active{war.Months++;war.Score*=.98}}}
