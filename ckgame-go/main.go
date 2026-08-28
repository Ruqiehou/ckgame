// Package main 是 ckgame-go 的命令行演示入口。
// 加载 1066 英格兰场景，打印世界概况，并推进数月展示 AI 集结与月度结算。
// 完整的 GameSimulation 主循环、GameAPI 与 TUI 尚在开发中（见 docs/go/）。
package main

import (
	"fmt"
	"os"

	"github.com/ckgame/ckgame-go/ai"
	"github.com/ckgame/ckgame-go/military"
	"github.com/ckgame/ckgame-go/world"
)

func main() {
	w, err := world.LoadScenario(world.ScenarioPath())
	if err != nil {
		fmt.Fprintln(os.Stderr, "加载场景失败:", err)
		os.Exit(1)
	}

	fmt.Println("CKGame Go 版 — 1066 英格兰场景")
	fmt.Printf("日期：%s（%s）\n", w.Date, w.Date.Season())
	fmt.Printf("人物 %d 名、王朝 %d 个、伯爵领 %d 个、头衔 %d 个\n",
		len(w.Characters), len(w.Dynasties), len(w.Counties), len(w.Titles))

	armies := military.NewManager()
	director := ai.New()
	for i := 0; i < 3; i++ {
		w.Date = w.Date.AddMonths(1)
		director.Tick(w, armies)
		armies.TickMonth(w)
	}
	fmt.Printf("\n推进 3 个月后：%s，共集结军队 %d 支\n", w.Date, len(armies.Armies))

	fmt.Println("\n最近日志：")
	logs := w.Logs
	if len(logs) > 10 {
		logs = logs[len(logs)-10:]
	}
	for _, line := range logs {
		fmt.Println(" -", line)
	}
}
