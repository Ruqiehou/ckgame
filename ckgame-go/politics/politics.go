package politics

import("fmt";"github.com/ckgame/ckgame-go/world")
type Treaty struct{Kind,From,To string;Expires string;Active bool}
type RealmLaw struct{Succession, CrownAuthority, Gender string}
type Diplomacy struct{Treaties []Treaty; Claims map[string][]string}
func NewDiplomacy()*Diplomacy{return &Diplomacy{Claims:map[string][]string{}}}
func(d *Diplomacy) Form(w *world.World,kind,from,to string)error{if _,e:=w.Character(from);e!=nil{return e};if _,e:=w.Character(to);e!=nil{return e};d.Treaties=append(d.Treaties,Treaty{Kind:kind,From:from,To:to,Active:true});w.Log(fmt.Sprintf("%s 与 %s 建立%s",from,to,kind));return nil}
func(d *Diplomacy) Improve(w *world.World,from,to string)error{a,e:=w.Character(from);if e!=nil{return e};if _,e=w.Character(to);e!=nil{return e};if a.Gold<10{return fmt.Errorf("金币不足")};a.Gold-=10; aOpin:=a.Opinions;if aOpin==nil{aOpin=map[string]int{};a.Opinions=aOpin};aOpin[to]+=15;w.Log(fmt.Sprintf("%s 改善了与 %s 的关系",from,to));return nil}
