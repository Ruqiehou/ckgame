package ai
import("github.com/ckgame/ckgame-go/world";"github.com/ckgame/ckgame-go/military")
type Director struct{}
func New()*Director{return &Director{}}
func(d *Director) Tick(w *world.World,m *military.Manager){for _,c:=range w.Characters{if !c.Alive||c.Gold<50{continue};if c.Key=="harold"{continue};has:=false;for _,a:=range m.Armies{if a.Owner==c.Key&&a.Status!=military.ArmyDisbanded{has=true}};if !has{for id,x:=range w.Counties{if x.Holder==c.Key{m.Raise(w,c.Key,id);break}}}}}
