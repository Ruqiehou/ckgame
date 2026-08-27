package events

type Choice struct{ID,Text string; Gold,Prestige int}
type Instance struct{ID,Title,Description string; Choices []Choice; Character string; Resolved bool; Result string}
type Engine struct{Pending []*Instance; next int}
func New()*Engine{return &Engine{}}
func(e *Engine) Add(title,description,character string,choices []Choice)*Instance{e.next++;v:=&Instance{ID:"event_"+itoa(e.next),Title:title,Description:description,Character:character,Choices:choices};e.Pending=append(e.Pending,v);return v}
func(e *Engine) Resolve(id string,choice int) *Instance{for _,v:=range e.Pending{if v.ID==id&&!v.Resolved{v.Resolved=true;if choice>=0&&choice<len(v.Choices){v.Result=v.Choices[choice].Text};return v}};return nil}
func itoa(v int)string{if v==0{return "0"};out:="";for v>0{out=string(rune('0'+v%10))+out;v/=10};return out}
