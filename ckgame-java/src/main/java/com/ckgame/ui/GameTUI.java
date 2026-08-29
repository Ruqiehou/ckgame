package com.ckgame.ui;

import com.ckgame.game.ScenarioLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

/**
 * CK 风格大战略引擎 -- 完整终端文字界面。
 * 通过 GameAPI 的 snapshot() / action() 驱动所有交互。
 */
public final class GameTUI {

    private final GameAPI api;
    private final Scanner scanner;

    /* ──────────────────── 常量映射 ──────────────────── */

    private static final Map<String, String> SEASONS = new HashMap<>();
    static {
        SEASONS.put("SPRING", "春");
        SEASONS.put("SUMMER", "夏");
        SEASONS.put("AUTUMN", "秋");
        SEASONS.put("WINTER", "冬");
    }

    private static final Map<String, String> TERRAINS = new HashMap<>();
    static {
        TERRAINS.put("PLAINS", "平原");
        TERRAINS.put("HILLS", "丘陵");
        TERRAINS.put("MOUNTAINS", "山地");
        TERRAINS.put("FOREST", "森林");
        TERRAINS.put("DESERT", "沙漠");
        TERRAINS.put("WETLAND", "湿地");
        TERRAINS.put("FARMLAND", "农田");
        TERRAINS.put("COASTAL", "沿海");
    }

    /* ──────────────────── 构造 ──────────────────── */

    public GameTUI() {
        this.api = new GameAPI();
        this.scanner = new Scanner(System.in);
    }

    /* ──────────────────── 主循环 ──────────────────── */

    public void run() {
        System.out.println("====================================================");
        System.out.println("  CK 风格大战略引擎 -- 命令行文字对话模式");
        System.out.println("  十字军之王 Java 版  1066 年诺曼征服模拟器");
        System.out.println("====================================================");
        Map<String, Object> snap = api.snapshot();
        Map<String, Object> p = playerMap(snap);
        System.out.println("  当前玩家: " + str(p, "name", "?"));
        System.out.println("  输入 0 或 quit 退出，输入 help 查看帮助");
        pause();

        while (true) {
            handleEvents();

            render("主菜单");
            showMainMenu();
            String cmd = input("> ").toLowerCase(Locale.ROOT).trim();

            if (cmd.isEmpty()) {
                continue;
            }
            if (cmd.equals("0") || cmd.equals("quit") || cmd.equals("exit")) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("action", "save");
                api.action(payload);
                System.out.println("  已自动存档。再见！");
                break;
            }
            switch (cmd) {
                case "help":              showHelp(); break;
                case "1":               showStatus(); break;
                case "2":               showCharDetail(); break;
                case "3":               showCounties(); break;
                case "4":               showCountyDetail(); break;
                case "5":               showArmies(); break;
                case "6":               showWars(); break;
                case "7":               showClaims(); break;
                case "8":               showTreaties(); break;
                case "9":               showRulers(); break;
                case "10":              showSieges(); break;
                case "11":              showLog(); break;
                case "12":              showMap(); break;
                case "a":               actionRaise(); break;
                case "b":               actionMove(); break;
                case "c":               actionDisband(); break;
                case "d":               actionWar(); break;
                case "e":               actionPeace(); break;
                case "f":               actionImprove(); break;
                case "g":               actionFeast(); break;
                case "h":               actionBuild(); break;
                case "i":               actionDevelop(); break;
                case "j":               actionClaim(); break;
                case "k":               actionGrant(); break;
                case "l":               actionKnights(); break;
                case "m":               menuCouncil(); break;
                case "n":               menuSchemes(); break;
                case "o":               menuDiplomacy(); break;
                case "p":               menuLaws(); break;
                case "q":               menuSaveLoad(); break;
                case "r":               actionAdvance(); break;
                case "s":               actionSwitchPlayer(); break;
                case "t":               toggleCheat(); break;
                default:
                    System.out.println("  未知指令，输入 help 查看帮助");
                    pause();
                    break;
            }
        }
    }

    /* ──────────────────── 主菜单 ──────────────────── */

    private void showMainMenu() {
        System.out.println();
        System.out.println("  [1]  状态概览      [2]  角色详情      [3]  伯爵领列表");
        System.out.println("  [4]  伯爵领详情    [5]  军队列表      [6]  战争列表");
        System.out.println("  [7]  宣称列表      [8]  条约列表      [9]  统治者列表");
        System.out.println("  [10] 围城列表      [11] 日志          [12] 地图");
        System.out.println("  [A]  征召军队      [B]  移动军队      [C]  解散军队");
        System.out.println("  [D]  宣战          [E]  议和          [F]  改善关系");
        System.out.println("  [G]  举办宴会      [H]  建造建筑      [I]  发展领地");
        System.out.println("  [J]  伪造宣称      [K]  授予头衔      [L]  招募骑士");
        System.out.println("  [M]  内阁管理      [N]  阴谋          [O]  外交");
        System.out.println("  [P]  法律          [Q]  存档管理      [R]  推进时间");
        System.out.println("  [S]  切换角色      [T]  作弊          [0]  退出");
    }

    /* ──────────────────── 渲染 / 头部 ──────────────────── */

    private void render(String title) {
        clear();
        Map<String, Object> snap = api.snapshot();
        Map<String, Object> p = playerMap(snap);
        String season = SEASONS.getOrDefault(str(snap, "season", ""), "?");
        String date = str(snap, "date", "????-??-??");

        List<Map<String, Object>> wars = listMap(snap, "wars");
        int activeWarCount = 0;
        for (Map<String, Object> w : wars) {
            if (bool(w, "active") && bool(w, "involves_player")) {
                activeWarCount++;
            }
        }
        List<Map<String, Object>> armies = listMap(snap, "armies");
        int playerArmyCount = 0;
        int playerArmyMen = 0;
        for (Map<String, Object> a : armies) {
            if (bool(a, "is_player")) {
                playerArmyCount++;
                playerArmyMen += intVal(a, "men");
            }
        }
        List<Map<String, Object>> counties = listMap(snap, "counties");
        int siegeCount = 0;
        for (Map<String, Object> c : counties) {
            if (c.get("siege") != null) {
                siegeCount++;
            }
        }
        double warExhaustion = dbl(snap, "player_war_exhaustion");

        System.out.println("============================================================");
        if (title != null && !title.isEmpty()) {
            System.out.println("  [" + title + "]");
        }
        System.out.println("  日期: " + date + " [" + season + "]  |  统治者: "
                + str(p, "name", "?") + " (" + str(p, "title", "无") + ")");
        System.out.printf("  金: %.0f  威望: %.0f  虔诚: %.0f  压力: %d  健康: %.1f%n",
                dbl(p, "gold"), dbl(p, "prestige"), dbl(p, "piety"),
                intVal(p, "stress"), dbl(p, "health"));

        Map<String, Object> attrs = mapVal(p, "attrs");
        if (attrs != null && !attrs.isEmpty()) {
            System.out.printf("  外交:%d 军事:%d 管理:%d 谋略:%d 学识:%d 勇武:%d%n",
                    intVal(attrs, "diplomacy"), intVal(attrs, "martial"),
                    intVal(attrs, "stewardship"), intVal(attrs, "intrigue"),
                    intVal(attrs, "learning"), intVal(attrs, "prowess"));
        }

        StringBuilder summary = new StringBuilder();
        if (activeWarCount > 0) summary.append("战争:").append(activeWarCount);
        if (playerArmyCount > 0) {
            if (summary.length() > 0) summary.append("  ");
            summary.append("军队:").append(playerArmyCount).append("(").append(playerArmyMen).append("人)");
        }
        if (siegeCount > 0) {
            if (summary.length() > 0) summary.append("  ");
            summary.append("围城:").append(siegeCount);
        }
        if (warExhaustion > 0.1) {
            if (summary.length() > 0) summary.append("  ");
            summary.append("战争疲劳:").append(String.format("%.1f", warExhaustion));
        }
        if (summary.length() > 0) {
            System.out.println("  [" + summary + "]");
        }
        System.out.println("============================================================");
    }

    /* ──────────────────── 状态概览 ──────────────────── */

    private void showStatus() {
        render("状态概览");
        Map<String, Object> snap = api.snapshot();
        Map<String, Object> p = playerMap(snap);
        System.out.printf("%n  金币: %.0f%n", dbl(p, "gold"));
        System.out.printf("  威望: %.0f%n", dbl(p, "prestige"));
        System.out.printf("  虔诚: %.0f%n", dbl(p, "piety"));
        System.out.println("  压力: " + intVal(p, "stress"));
        System.out.printf("  健康: %.1f%n", dbl(p, "health"));
        System.out.println("  等级: " + intVal(p, "level") + "  XP: "
                + intVal(p, "xp") + "/" + intVal(p, "xpToNext"));
        System.out.printf("  月收入: %.1f%n", dbl(p, "income"));
        System.out.println("  野战军: " + intVal(p, "men"));
        System.out.printf("  战争疲劳: %.1f%n", dbl(snap, "player_war_exhaustion"));

        Map<String, Object> laws = mapVal(p, "laws");
        if (laws != null && !laws.isEmpty()) {
            System.out.println("  继承法: " + str(laws, "succession", "无"));
            System.out.println("  王权: " + str(laws, "crown_authority", "无"));
            System.out.println("  性别法: " + str(laws, "gender_law", "无"));
        }

        List<String> log = listStr(snap, "log");
        System.out.println("\n  -- 近期日志 --");
        int start = Math.max(0, log.size() - 8);
        for (int i = start; i < log.size(); i++) {
            System.out.println("  " + log.get(i));
        }
        pause();
    }

    /* ──────────────────── 角色详情 ──────────────────── */

    private void showCharDetail() {
        render("角色详情");
        Map<String, Object> snap = api.snapshot();
        List<Map<String, Object>> chars = listMap(snap, "characters");
        int pid = api.playerId();
        System.out.println("\n  选择角色:");
        for (int i = 0; i < chars.size(); i++) {
            Map<String, Object> c = chars.get(i);
            String tag = intVal(c, "id") == pid ? " * " : "   ";
            StringBuilder rel = new StringBuilder();
            if (bool(c, "relation_allied")) rel.append("[盟]");
            if (bool(c, "relation_rival")) rel.append("[敌]");
            if (bool(c, "relation_at_war")) rel.append("[战]");
            if (bool(c, "relation_marriage")) rel.append("[姻]");
            System.out.printf("  %s%d. %s  %d岁  %s  %s%n",
                    tag, i + 1, str(c, "name", "?"),
                    intVal(c, "age"), str(c, "title", "无"), rel);
        }
        System.out.println("    0. 返回");

        int choice = inputInt("选择", 0);
        if (choice <= 0 || choice > chars.size()) return;
        Map<String, Object> c = chars.get(choice - 1);
        renderCharDetail(c);
    }

    private void renderCharDetail(Map<String, Object> c) {
        render("角色详情 -- " + str(c, "name", "?"));
        Map<String, Object> attrs = mapVal(c, "attrs");
        System.out.printf("%n  姓名: %s  性别: %s  年龄: %d%n",
                str(c, "name", "?"), str(c, "gender", "?"), intVal(c, "age"));
        System.out.println("  家族: " + str(c, "dynasty_name", "无")
                + "  头衔: " + str(c, "title", "无"));
        System.out.printf("  金币: %.0f  威望: %.0f  等级: %d%n",
                dbl(c, "gold"), dbl(c, "prestige"), intVal(c, "level"));
        if (attrs != null && !attrs.isEmpty()) {
            System.out.printf("  外交:%d 军事:%d 管理:%d 谋略:%d 学识:%d 勇武:%d%n",
                    intVal(attrs, "diplomacy"), intVal(attrs, "martial"),
                    intVal(attrs, "stewardship"), intVal(attrs, "intrigue"),
                    intVal(attrs, "learning"), intVal(attrs, "prowess"));
        }
        System.out.println("\n  对你的好感: " + intVal(c, "opinion_of_player")
                + "  你对其好感: " + intVal(c, "player_opinion"));

        List<String> rels = new ArrayList<>();
        if (bool(c, "relation_allied")) rels.add("同盟");
        if (bool(c, "relation_rival")) rels.add("宿敌");
        if (bool(c, "relation_at_war")) rels.add("交战中");
        if (bool(c, "relation_marriage")) rels.add("联姻");
        if (bool(c, "relation_vassalage")) rels.add("附庸");
        if (bool(c, "relation_trade_agreement")) rels.add("贸易");
        if (bool(c, "relation_intelligence_sharing")) rels.add("情报共享");
        System.out.println("  关系: " + (rels.isEmpty() ? "无特殊关系" : String.join("  ", rels)));

        List<Object> spouseIds = (List<Object>) c.get("spouse_ids");
        if (spouseIds != null && !spouseIds.isEmpty()) {
            System.out.println("  配偶: " + spouseIds.size() + "人");
        } else {
            System.out.println("  配偶: 无");
        }

        List<Object> heldTitleIds = (List<Object>) c.get("held_title_ids");
        if (heldTitleIds != null && !heldTitleIds.isEmpty()) {
            System.out.println("  持有头衔数: " + heldTitleIds.size());
        }

        List<Object> traitNames = (List<Object>) c.get("trait_names");
        if (traitNames != null && !traitNames.isEmpty()) {
            StringBuilder sb = new StringBuilder("  特质: ");
            for (int i = 0; i < traitNames.size(); i++) {
                if (i > 0) sb.append("、");
                sb.append(traitNames.get(i));
            }
            System.out.println(sb);
        }
        System.out.println();
        pause();
    }

    /* ──────────────────── 伯爵领列表 ──────────────────── */

    private void showCounties() {
        render("伯爵领列表");
        Map<String, Object> snap = api.snapshot();
        List<Map<String, Object>> counties = listMap(snap, "counties");
        System.out.printf("%n  %-4s %-10s %-8s %-6s %-8s %-12s %-8s %-8s%n",
                "ID", "名称", "地形", "发展", "控制", "领主", "税收", "征召");
        System.out.println("  " + "-".repeat(72));
        for (Map<String, Object> c : counties) {
            String marker = bool(c, "is_player") ? "*" : " ";
            String terrainZh = TERRAINS.getOrDefault(str(c, "terrain", ""), str(c, "terrain", "?"));
            System.out.printf("  %s%-3d %-10s %-8s %-6d %-8.1f %-12s %-8.1f %-8d%n",
                    marker, intVal(c, "id"), str(c, "name", "?"),
                    terrainZh, intVal(c, "development"),
                    dbl(c, "control"), str(c, "holder_name", "无主"),
                    dbl(c, "tax"), intVal(c, "levies"));
        }
        System.out.println("\n  * 表示己方领地");
        pause();
    }

    /* ──────────────────── 伯爵领详情 ──────────────────── */

    private void showCountyDetail() {
        render("伯爵领详情");
        Map<String, Object> snap = api.snapshot();
        List<Map<String, Object>> counties = listMap(snap, "counties");
        System.out.println("\n  选择省份:");
        for (int i = 0; i < counties.size(); i++) {
            Map<String, Object> c = counties.get(i);
            String marker = bool(c, "is_player") ? "*" : " ";
            String siegeTag = c.get("siege") != null ? " [围城中]" : "";
            System.out.printf("  %s%d. %s (%s)%s%n",
                    marker, i + 1, str(c, "name", "?"),
                    str(c, "holder_name", "?"), siegeTag);
        }
        System.out.println("    0. 返回");

        int choice = inputInt("选择", 0);
        if (choice <= 0 || choice > counties.size()) return;
        renderCountyDetail(counties.get(choice - 1));
    }

    private void renderCountyDetail(Map<String, Object> c) {
        render("伯爵领详情 -- " + str(c, "name", "?"));
        String terrainZh = TERRAINS.getOrDefault(str(c, "terrain", ""), str(c, "terrain", "?"));
        System.out.printf("%n  省份: %s  地形: %s%n", str(c, "name", "?"), terrainZh);
        System.out.println("  领主: " + str(c, "holder_name", "无主")
                + (bool(c, "is_player") ? "  * 己方领地" : ""));
        System.out.printf("  发展: %d/%s  控制: %.1f%n",
                intVal(c, "development"),
                c.get("dev_cap") != null ? String.valueOf(intVal(c, "dev_cap")) : "?",
                dbl(c, "control"));
        System.out.printf("  税收: %.1f  征召: %d  堡垒: %d%n",
                dbl(c, "tax"), intVal(c, "levies"), intVal(c, "fort"));

        List<Map<String, Object>> buildings = listMap(c, "buildings");
        if (buildings.isEmpty()) {
            System.out.println("\n  建筑: 无");
        } else {
            System.out.println("\n  建筑:");
            for (Map<String, Object> b : buildings) {
                String tag = "";
                if (b.get("max_level") != null) {
                    tag = " Lv." + intVal(b, "level") + "/" + intVal(b, "max_level");
                }
                System.out.println("    " + str(b, "name", "?") + tag
                        + " -- " + str(b, "description", ""));
            }
        }

        List<Object> neighbors = (List<Object>) c.get("neighbors");
        if (neighbors != null && !neighbors.isEmpty()) {
            System.out.println("  邻接省份数: " + neighbors.size());
        }

        Map<String, Object> siege = mapVal(c, "siege");
        if (siege != null && !siege.isEmpty()) {
            System.out.println("\n  -- 围城进行中 --");
            System.out.printf("  进度: %d/%d%n",
                    intVal(siege, "progress"), intVal(siege, "required"));
            System.out.println("  攻方: " + str(siege, "attacker_name", "?"));
        }
        System.out.println();
        pause();
    }

    /* ──────────────────── 军队列表 ──────────────────── */

    private void showArmies() {
        render("军队列表");
        Map<String, Object> snap = api.snapshot();
        List<Map<String, Object>> armies = listMap(snap, "armies");
        System.out.printf("%n  %-4s %-16s %-10s %-10s %-8s %-8s %-10s%n",
                "ID", "名称", "状态", "位置", "兵力", "补给", "指挥官");
        System.out.println("  " + "-".repeat(72));
        for (Map<String, Object> a : armies) {
            String marker = bool(a, "is_player") ? "*" : " ";
            System.out.printf("  %s%-3d %-16s %-10s %-10s %-8d %-8.1f %-10s%n",
                    marker, intVal(a, "id"), str(a, "name", "?"),
                    str(a, "status", "?"), str(a, "location_name", "?"),
                    intVal(a, "men"), dbl(a, "supply"),
                    str(a, "owner_name", "?"));
        }
        System.out.println("\n  * 表示己方军团");
        pause();
    }

    /* ──────────────────── 战争列表 ──────────────────── */

    private void showWars() {
        render("战争列表");
        Map<String, Object> snap = api.snapshot();
        List<Map<String, Object>> wars = listMap(snap, "wars");
        if (wars.isEmpty()) {
            System.out.println("\n  当前无战争");
        } else {
            for (Map<String, Object> w : wars) {
                String status = bool(w, "active") ? "进行中" : "已结束";
                System.out.printf("  [%s] %s | %s vs %s | 分数:%.0f | %s%n",
                        status, str(w, "name", "?"),
                        str(w, "attacker", "?"), str(w, "defender", "?"),
                        dbl(w, "warscore"), str(w, "cb", "?"));
            }
        }
        pause();
    }

    /* ──────────────────── 宣称列表 ──────────────────── */

    private void showClaims() {
        render("宣称列表");
        Map<String, Object> snap = api.snapshot();
        List<Map<String, Object>> claims = listMap(snap, "player_claims");
        if (claims.isEmpty()) {
            System.out.println("\n  你没有任何宣称");
        } else {
            System.out.println("\n  你的宣称:");
            for (Map<String, Object> cl : claims) {
                String target = str(cl, "title_name", "");
                if (target.isEmpty()) target = str(cl, "county_name", "?");
                String pressed = bool(cl, "pressed") ? "已压制" : "未压制";
                System.out.printf("    %s (强度:%.0f %s)%n",
                        target, dbl(cl, "strength"), pressed);
            }
        }
        pause();
    }

    /* ──────────────────── 条约列表 ──────────────────── */

    private void showTreaties() {
        render("条约列表");
        Map<String, Object> snap = api.snapshot();
        List<Map<String, Object>> treaties = listMap(snap, "treaties");
        if (treaties.isEmpty()) {
            System.out.println("\n  没有生效的条约");
        } else {
            System.out.println("\n  生效的条约:");
            for (Map<String, Object> t : treaties) {
                System.out.printf("    %s <-> %s (至 %s 年)%n",
                        str(t, "kind_zh", str(t, "kind", "?")),
                        str(t, "other_name", "?"),
                        str(t, "expires_year", "?"));
            }
        }
        pause();
    }

    /* ──────────────────── 统治者列表 ──────────────────── */

    private void showRulers() {
        render("统治者列表");
        Map<String, Object> snap = api.snapshot();
        List<Map<String, Object>> rulers = listMap(snap, "rulers");
        System.out.printf("%n  %-12s %-12s %-7s %-7s %-5s %-7s %-7s %s%n",
                "名字", "头衔", "金", "威望", "军略", "收入", "兵力", "性格");
        System.out.println("  " + "-".repeat(75));
        for (Map<String, Object> r : rulers) {
            String marker = bool(r, "is_player") ? "*" : " ";
            System.out.printf("  %s%-11s %-12s %-7.0f %-7.0f %-5d %-7.1f %-7d %s%n",
                    marker, str(r, "name", "?"), str(r, "title", "无"),
                    dbl(r, "gold"), dbl(r, "prestige"),
                    intVal(r, "martial"), dbl(r, "income"),
                    intVal(r, "men"), str(r, "persona", ""));
        }
        System.out.println("\n  * 表示当前玩家");
        pause();
    }

    /* ──────────────────── 围城列表 ──────────────────── */

    private void showSieges() {
        render("围城列表");
        Map<String, Object> snap = api.snapshot();
        List<Map<String, Object>> counties = listMap(snap, "counties");
        List<Map<String, Object>> sieged = new ArrayList<>();
        for (Map<String, Object> c : counties) {
            if (c.get("siege") != null) {
                sieged.add(c);
            }
        }
        if (sieged.isEmpty()) {
            System.out.println("\n  当前无围城");
        } else {
            for (Map<String, Object> c : sieged) {
                Map<String, Object> s = mapVal(c, "siege");
                if (s == null) continue;
                int progress = intVal(s, "progress");
                int required = Math.max(1, intVal(s, "required"));
                int barLen = 20;
                int filled = (int) ((long) barLen * progress / required);
                filled = Math.max(0, Math.min(barLen, filled));
                StringBuilder bar = new StringBuilder();
                for (int i = 0; i < filled; i++) bar.append('#');
                for (int i = filled; i < barLen; i++) bar.append('.');
                String involved = bool(c, "is_player") ? "*" : " ";
                System.out.printf("%n  %s%s (守方:%s)%n",
                        involved, str(c, "name", "?"), str(c, "holder_name", "?"));
                System.out.println("  攻方: " + str(s, "attacker_name", "?"));
                System.out.printf("  [%s] %d/%d%n", bar, progress, required);
            }
        }
        pause();
    }

    /* ──────────────────── 日志 ──────────────────── */

    private void showLog() {
        render("事件日志");
        Map<String, Object> snap = api.snapshot();
        List<String> log = listStr(snap, "log");
        if (log.isEmpty()) {
            System.out.println("\n  暂无日志");
        } else {
            System.out.printf("%n  最近 %d 条事件:%n%n", log.size());
            for (String line : log) {
                System.out.println("  " + line);
            }
        }
        pause();
    }

    /* ──────────────────── ASCII 地图 ──────────────────── */

    private void showMap() {
        render("文字地图");
        Map<String, Object> snap = api.snapshot();
        List<Map<String, Object>> counties = listMap(snap, "counties");
        if (counties.isEmpty()) {
            System.out.println("\n  无地图数据");
            pause();
            return;
        }

        int cols = 56;
        int rows = 24;
        char[][] grid = new char[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c] = ' ';
            }
        }

        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (Map<String, Object> c : counties) {
            double cx = dbl(c, "cx");
            double cy = dbl(c, "cy");
            if (cx < minX) minX = cx;
            if (cx > maxX) maxX = cx;
            if (cy < minY) minY = cy;
            if (cy > maxY) maxY = cy;
        }
        double rangeX = Math.max(1, maxX - minX);
        double rangeY = Math.max(1, maxY - minY);

        Map<Integer, Map<String, Object>> byId = new HashMap<>();
        for (Map<String, Object> c : counties) {
            byId.put(intVal(c, "id"), c);
        }

        // 绘制邻接线
        Map<Long, Boolean> drawn = new HashMap<>();
        for (Map<String, Object> c : counties) {
            int gx1 = toGridX(dbl(c, "cx"), minX, rangeX, cols);
            int gy1 = toGridY(dbl(c, "cy"), minY, rangeY, rows);
            List<Object> neighbors = (List<Object>) c.get("neighbors");
            if (neighbors == null) continue;
            for (Object nidObj : neighbors) {
                int nid = ((Number) nidObj).intValue();
                long key = Math.min(intVal(c, "id"), nid) * 100000L + Math.max(intVal(c, "id"), nid);
                if (drawn.containsKey(key)) continue;
                drawn.put(key, true);
                Map<String, Object> nc = byId.get(nid);
                if (nc == null) continue;
                int gx2 = toGridX(dbl(nc, "cx"), minX, rangeX, cols);
                int gy2 = toGridY(dbl(nc, "cy"), minY, rangeY, rows);
                int steps = Math.max(Math.abs(gx2 - gx1), Math.abs(gy2 - gy1));
                if (steps < 1) steps = 1;
                for (int s = 0; s <= steps; s++) {
                    int px = gx1 + (gx2 - gx1) * s / steps;
                    int py = gy1 + (gy2 - gy1) * s / steps;
                    if (py >= 0 && py < rows && px >= 0 && px < cols && grid[py][px] == ' ') {
                        grid[py][px] = '.';
                    }
                }
            }
        }

        // 绘制省份标记
        for (Map<String, Object> c : counties) {
            int gx = toGridX(dbl(c, "cx"), minX, rangeX, cols);
            int gy = toGridY(dbl(c, "cy"), minY, rangeY, rows);
            if (gy >= 0 && gy < rows && gx >= 0 && gx < cols) {
                if (bool(c, "is_player")) {
                    grid[gy][gx] = '*';
                } else if (c.get("siege") != null) {
                    grid[gy][gx] = 'X';
                } else {
                    String holder = str(c, "holder_name", "");
                    grid[gy][gx] = holder.isEmpty() ? '?' : holder.charAt(0);
                }
            }
        }

        // 输出地图
        System.out.println();
        String border = "=".repeat(cols);
        System.out.println("  +" + border + "+");
        for (int r = 0; r < rows; r++) {
            System.out.println("  |" + new String(grid[r]) + "|");
        }
        System.out.println("  +" + border + "+");

        System.out.println("\n  * 己方  X 围城中  . 邻接  [其他为领主名首字母]");
        System.out.println("\n  省份图例:");
        for (Map<String, Object> c : counties) {
            String holder = str(c, "holder_name", "?");
            String ch;
            if (bool(c, "is_player")) {
                ch = "*";
            } else if (c.get("siege") != null) {
                ch = "X";
            } else {
                ch = holder.isEmpty() ? "?" : String.valueOf(holder.charAt(0));
            }
            System.out.printf("    %s %s (%s)%n", ch, str(c, "name", "?"), holder);
        }
        pause();
    }

    private static int toGridX(double cx, double minX, double rangeX, int cols) {
        return (int) ((cx - minX) / rangeX * (cols - 8) + 3);
    }

    private static int toGridY(double cy, double minY, double rangeY, int rows) {
        return (int) ((cy - minY) / rangeY * (rows - 4) + 1);
    }

    /* ──────────────────── 帮助 ──────────────────── */

    private void showHelp() {
        render("帮助");
        System.out.println("\n  这是一个 CK 风格的大战略模拟引擎。");
        System.out.println("  你扮演一位中世纪统治者，管理领地、组建军队、进行外交和阴谋。");
        System.out.println("  使用数字或字母选择菜单项。");
        System.out.println("  时间推进时会自动处理月度事件、战斗、围城等。");
        System.out.println("  如果有事件弹出，请选择对应数字。");
        System.out.println("  输入 0 或 q 退出游戏。");
        pause();
    }

    /* ──────────────────── 事件处理 ──────────────────── */

    private void handleEvents() {
        Map<String, Object> snap = api.snapshot();
        List<Map<String, Object>> events = listMap(snap, "pending_events");
        if (events.isEmpty()) return;

        for (Map<String, Object> ev : events) {
            clear();
            System.out.println("============================================================");
            System.out.println("  >> 事件: " + str(ev, "title", "未知事件"));
            System.out.println("============================================================");
            System.out.println();
            System.out.println("  " + str(ev, "description", ""));
            System.out.println();

            List<Map<String, Object>> choices = listMap(ev, "choices");
            for (int i = 0; i < choices.size(); i++) {
                System.out.printf("  %d. %s%n", i + 1, str(choices.get(i), "text", "?"));
            }
            System.out.println("  0. 跳过");

            int choice = inputInt("选择", 0);
            if (choice <= 0 || choice > choices.size()) continue;

            Map<String, Object> chosen = choices.get(choice - 1);
            Map<String, Object> payload = new HashMap<>();
            payload.put("action", "resolve_event");
            payload.put("event_id", str(ev, "eventId", ""));
            payload.put("choice_id", str(chosen, "id", ""));
            Map<String, Object> res = api.action(payload);
            System.out.println("\n  你选择了: " + str(chosen, "text", ""));
            String msg = lastMessage(res);
            if (msg != null && !msg.isEmpty()) {
                System.out.println("  " + msg);
            }
            pause();
        }
    }

    /* ──────────────────── 征召军队 ──────────────────── */

    private void actionRaise() {
        render("征召军队");
        Map<String, Object> snap = api.snapshot();
        List<Map<String, Object>> owned = filterPlayerCounties(snap);
        if (owned.isEmpty()) {
            System.out.println("\n  你没有领地，无法征召");
            pause();
            return;
        }

        System.out.println("\n  选择征召省份:");
        for (int i = 0; i < owned.size(); i++) {
            Map<String, Object> c = owned.get(i);
            System.out.printf("    %d. %s (征召:%d 税收:%.1f)%n",
                    i + 1, str(c, "name", "?"), intVal(c, "levies"), dbl(c, "tax"));
        }
        System.out.println("    0. 返回");

        int choice = inputInt("选择", 0);
        if (choice <= 0 || choice > owned.size()) return;
        Map<String, Object> county = owned.get(choice - 1);

        List<Map<String, Object>> armies = listMap(snap, "armies");
        for (Map<String, Object> a : armies) {
            if (bool(a, "is_player") && !"DISBANDED".equals(str(a, "status", ""))) {
                System.out.println("\n  已有军团，请先解散");
                pause();
                return;
            }
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "raise_army");
        payload.put("county_id", intVal(county, "id"));
        Map<String, Object> res = api.action(payload);
        System.out.println("\n  " + (lastMessage(res).isEmpty() ? "征召完成" : lastMessage(res)));
        pause();
    }

    /* ──────────────────── 移动军队 ──────────────────── */

    private void actionMove() {
        render("移动军队");
        Map<String, Object> snap = api.snapshot();
        List<Map<String, Object>> armies = filterPlayerArmies(snap);
        if (armies.isEmpty()) {
            System.out.println("\n  没有可指挥的军团");
            pause();
            return;
        }

        System.out.println("\n  选择军团:");
        for (int i = 0; i < armies.size(); i++) {
            Map<String, Object> a = armies.get(i);
            System.out.printf("    %d. %s (兵力:%d 位置:%s)%n",
                    i + 1, str(a, "name", "?"), intVal(a, "men"),
                    str(a, "location_name", "?"));
        }
        System.out.println("    0. 返回");

        int choice = inputInt("选择", 0);
        if (choice <= 0 || choice > armies.size()) return;
        Map<String, Object> army = armies.get(choice - 1);

        List<Map<String, Object>> counties = listMap(snap, "counties");
        System.out.println("\n  选择目标省份:");
        for (Map<String, Object> c : counties) {
            System.out.printf("    %d. %s%n", intVal(c, "id"), str(c, "name", "?"));
        }
        System.out.println("    0. 返回");

        int target = inputInt("目标ID", 0);
        if (target <= 0) return;

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "move_army");
        payload.put("army_id", intVal(army, "id"));
        payload.put("county_id", target);
        Map<String, Object> res = api.action(payload);
        System.out.println("\n  " + (lastMessage(res).isEmpty() ? "移动完成" : lastMessage(res)));
        pause();
    }

    /* ──────────────────── 解散军队 ──────────────────── */

    private void actionDisband() {
        render("解散军队");
        Map<String, Object> snap = api.snapshot();
        List<Map<String, Object>> armies = filterPlayerArmies(snap);
        if (armies.isEmpty()) {
            System.out.println("\n  没有可解散的军团");
            pause();
            return;
        }

        System.out.println("\n  选择解散的军团:");
        for (int i = 0; i < armies.size(); i++) {
            Map<String, Object> a = armies.get(i);
            System.out.printf("    %d. %s (兵力:%d)%n",
                    i + 1, str(a, "name", "?"), intVal(a, "men"));
        }
        System.out.println("    0. 返回");

        int choice = inputInt("选择", 0);
        if (choice <= 0 || choice > armies.size()) return;
        Map<String, Object> army = armies.get(choice - 1);

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "disband_army");
        payload.put("army_id", intVal(army, "id"));
        api.action(payload);
        System.out.println("\n  已解散 " + str(army, "name", "?"));
        pause();
    }

    /* ──────────────────── 宣战 ──────────────────── */

    private void actionWar() {
        render("宣战");
        Map<String, Object> snap = api.snapshot();
        List<Map<String, Object>> rulers = listMap(snap, "rulers");
        List<Map<String, Object>> treaties = listMap(snap, "treaties");
        int pid = api.playerId();

        List<Map<String, Object>> targets = new ArrayList<>();
        for (Map<String, Object> r : rulers) {
            if (bool(r, "is_player") || intVal(r, "id") == pid) continue;
            boolean allied = false;
            for (Map<String, Object> tr : treaties) {
                if ("ALLIANCE".equals(str(tr, "kind", ""))) {
                    int a = intVal(tr, "a");
                    int b = intVal(tr, "b");
                    int rid = intVal(r, "id");
                    if ((pid == a && rid == b) || (pid == b && rid == a)) {
                        allied = true;
                        break;
                    }
                }
            }
            if (!allied) targets.add(r);
        }

        if (targets.isEmpty()) {
            System.out.println("\n  没有可宣战的目标");
            pause();
            return;
        }

        System.out.println("\n  可选目标:");
        for (int i = 0; i < targets.size(); i++) {
            Map<String, Object> t = targets.get(i);
            System.out.printf("    %d. %s (%s)%n",
                    i + 1, str(t, "name", "?"), str(t, "title", "无"));
        }
        System.out.println("    0. 返回");

        int choice = inputInt("选择", 0);
        if (choice <= 0 || choice > targets.size()) return;
        Map<String, Object> target = targets.get(choice - 1);

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "declare_war");
        payload.put("target_id", intVal(target, "id"));
        Map<String, Object> res = api.action(payload);
        System.out.println("\n  " + (lastMessage(res).isEmpty() ? "宣战完成" : lastMessage(res)));
        pause();
    }

    /* ──────────────────── 议和 ──────────────────── */

    private void actionPeace() {
        render("议和");
        Map<String, Object> snap = api.snapshot();
        List<Map<String, Object>> wars = listMap(snap, "wars");
        List<Map<String, Object>> active = new ArrayList<>();
        for (Map<String, Object> w : wars) {
            if (bool(w, "active") && bool(w, "involves_player")) {
                active.add(w);
            }
        }
        if (active.isEmpty()) {
            System.out.println("\n  你没有进行中的战争");
            pause();
            return;
        }

        System.out.println("\n  你的战争:");
        for (int i = 0; i < active.size(); i++) {
            Map<String, Object> w = active.get(i);
            String tag = bool(w, "can_white_peace") ? "可白和" : "条件不足";
            System.out.printf("    %d. %s | 分数:%.0f 已持续:%d月 [%s]%n",
                    i + 1, str(w, "name", "?"), dbl(w, "warscore"),
                    intVal(w, "months"), tag);
        }
        System.out.println("    0. 返回");

        int choice = inputInt("选择", 0);
        if (choice <= 0 || choice > active.size()) return;
        Map<String, Object> war = active.get(choice - 1);

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "white_peace");
        payload.put("war_id", intVal(war, "id"));
        Map<String, Object> res = api.action(payload);
        System.out.println("\n  " + (lastMessage(res).isEmpty() ? "操作完成" : lastMessage(res)));
        pause();
    }

    /* ──────────────────── 改善关系 ──────────────────── */

    private void actionImprove() {
        render("改善关系");
        Map<String, Object> snap = api.snapshot();
        List<Map<String, Object>> chars = listMap(snap, "characters");
        int pid = api.playerId();
        List<Map<String, Object>> others = new ArrayList<>();
        for (Map<String, Object> c : chars) {
            if (intVal(c, "id") != pid) others.add(c);
        }

        System.out.println("\n  选择角色:");
        for (int i = 0; i < others.size(); i++) {
            Map<String, Object> c = others.get(i);
            System.out.printf("    %d. %s (%s)%n",
                    i + 1, str(c, "name", "?"), str(c, "title", "无"));
        }
        System.out.println("    0. 返回");

        int choice = inputInt("选择", 0);
        if (choice <= 0 || choice > others.size()) return;
        Map<String, Object> target = others.get(choice - 1);

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "improve_relations");
        payload.put("target_id", intVal(target, "id"));
        Map<String, Object> res = api.action(payload);
        System.out.println("\n  " + (lastMessage(res).isEmpty() ? "操作完成" : lastMessage(res)));
        pause();
    }

    /* ──────────────────── 举办宴会 ──────────────────── */

    private void actionFeast() {
        render("举办宴会");
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "hold_feast");
        Map<String, Object> res = api.action(payload);
        System.out.println("\n  " + (lastMessage(res).isEmpty() ? "宴会完成" : lastMessage(res)));
        pause();
    }

    /* ──────────────────── 建造建筑 ──────────────────── */

    private void actionBuild() {
        render("建造/升级建筑");
        Map<String, Object> snap = api.snapshot();
        List<Map<String, Object>> owned = filterPlayerCounties(snap);
        if (owned.isEmpty()) {
            System.out.println("\n  你没有领地，无法建造");
            pause();
            return;
        }

        System.out.println("\n  选择省份:");
        for (int i = 0; i < owned.size(); i++) {
            Map<String, Object> c = owned.get(i);
            List<Map<String, Object>> blds = listMap(c, "buildings");
            System.out.printf("    %d. %s (发展:%d 已有建筑:%d)%n",
                    i + 1, str(c, "name", "?"), intVal(c, "development"), blds.size());
        }
        System.out.println("    0. 返回");

        int choice = inputInt("选择", 0);
        if (choice <= 0 || choice > owned.size()) return;
        Map<String, Object> county = owned.get(choice - 1);

        List<Map<String, Object>> existing = listMap(county, "buildings");
        System.out.printf("%n  %s 现有建筑:%n", str(county, "name", "?"));
        if (existing.isEmpty()) {
            System.out.println("    (无)");
        } else {
            for (Map<String, Object> b : existing) {
                String costStr = bool(b, "can_upgrade")
                        ? String.format("%.0f", dbl(b, "upgrade_cost"))
                        : "已满级";
                System.out.printf("    %s Lv.%d/%d 下一级费用:%s%n",
                        str(b, "name", "?"), intVal(b, "level"),
                        intVal(b, "max_level"), costStr);
            }
        }

        List<Map<String, Object>> kinds = listMap(snap, "building_types");
        if (kinds.isEmpty()) {
            System.out.println("\n  无可用建筑类型");
            pause();
            return;
        }
        System.out.println("\n  选择要建造/升级的建筑:");
        for (int i = 0; i < kinds.size(); i++) {
            Map<String, Object> k = kinds.get(i);
            System.out.printf("    %d. %s -- %s%n",
                    i + 1, str(k, "name", "?"), str(k, "description", ""));
        }
        System.out.println("    0. 返回");

        int kchoice = inputInt("选择", 0);
        if (kchoice <= 0 || kchoice > kinds.size()) return;
        Map<String, Object> kind = kinds.get(kchoice - 1);

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "upgrade_building");
        payload.put("county_id", intVal(county, "id"));
        payload.put("building_kind", str(kind, "value", ""));
        Map<String, Object> res = api.action(payload);
        System.out.println("\n  " + (lastMessage(res).isEmpty() ? "操作完成" : lastMessage(res)));
        pause();
    }

    /* ──────────────────── 发展领地 ──────────────────── */

    private void actionDevelop() {
        render("发展领地");
        Map<String, Object> snap = api.snapshot();
        List<Map<String, Object>> owned = filterPlayerCounties(snap);
        if (owned.isEmpty()) {
            System.out.println("\n  你没有领地");
            pause();
            return;
        }

        System.out.println("\n  选择省份 (每级花费 10 金):");
        for (int i = 0; i < owned.size(); i++) {
            Map<String, Object> c = owned.get(i);
            String capStr = c.get("dev_cap") != null
                    ? "/" + intVal(c, "dev_cap") : "";
            System.out.printf("    %d. %s (发展:%d%s 税收:%.1f)%n",
                    i + 1, str(c, "name", "?"),
                    intVal(c, "development"), capStr, dbl(c, "tax"));
        }
        System.out.println("    0. 返回");

        int choice = inputInt("选择", 0);
        if (choice <= 0 || choice > owned.size()) return;
        Map<String, Object> county = owned.get(choice - 1);

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "develop_county");
        payload.put("county_id", intVal(county, "id"));
        Map<String, Object> res = api.action(payload);
        System.out.println("\n  " + (lastMessage(res).isEmpty() ? "操作完成" : lastMessage(res)));
        pause();
    }

    /* ──────────────────── 伪造宣称 ──────────────────── */

    private void actionClaim() {
        render("伪造宣称");
        Map<String, Object> snap = api.snapshot();
        List<Map<String, Object>> counties = listMap(snap, "counties");
        List<Map<String, Object>> foreign = new ArrayList<>();
        for (Map<String, Object> c : counties) {
            if (!bool(c, "is_player")) foreign.add(c);
        }
        if (foreign.isEmpty()) {
            System.out.println("\n  没有可伪造宣称的省份");
            pause();
            return;
        }

        System.out.println("\n  选择省份 (花费 50 金):");
        for (int i = 0; i < foreign.size(); i++) {
            Map<String, Object> c = foreign.get(i);
            System.out.printf("    %d. %s (领主:%s)%n",
                    i + 1, str(c, "name", "?"), str(c, "holder_name", "?"));
        }
        System.out.println("    0. 返回");

        int choice = inputInt("选择", 0);
        if (choice <= 0 || choice > foreign.size()) return;
        Map<String, Object> county = foreign.get(choice - 1);

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "fabricate_claim");
        payload.put("county_id", intVal(county, "id"));
        Map<String, Object> res = api.action(payload);
        System.out.println("\n  " + (lastMessage(res).isEmpty() ? "操作完成" : lastMessage(res)));
        pause();
    }

    /* ──────────────────── 授予头衔 ──────────────────── */

    private void actionGrant() {
        render("授予头衔");
        Map<String, Object> snap = api.snapshot();
        List<Map<String, Object>> chars = listMap(snap, "characters");
        int pid = api.playerId();

        List<Map<String, Object>> titles = listMap(snap, "player_titles");
        if (titles.isEmpty()) {
            System.out.println("\n  没有可授予的头衔 (主头衔不可授予)");
            pause();
            return;
        }

        System.out.println("\n  你的头衔:");
        for (int i = 0; i < titles.size(); i++) {
            Map<String, Object> t = titles.get(i);
            System.out.printf("    %d. %s%n", i + 1, str(t, "name", "?"));
        }
        System.out.println("    0. 返回");

        int choice = inputInt("选择头衔", 0);
        if (choice <= 0 || choice > titles.size()) return;
        Map<String, Object> title = titles.get(choice - 1);

        List<Map<String, Object>> others = new ArrayList<>();
        for (Map<String, Object> c : chars) {
            if (intVal(c, "id") != pid && bool(c, "is_alive")) {
                others.add(c);
            }
        }
        if (others.isEmpty()) {
            System.out.println("\n  没有可授予的对象");
            pause();
            return;
        }

        System.out.printf("%n  授予给谁 (%s):%n", str(title, "name", "?"));
        for (int i = 0; i < others.size(); i++) {
            Map<String, Object> c = others.get(i);
            System.out.printf("    %d. %s (%s)%n",
                    i + 1, str(c, "name", "?"),
                    str(c, "title", "无头衔"));
        }
        System.out.println("    0. 返回");

        int cchoice = inputInt("选择", 0);
        if (cchoice <= 0 || cchoice > others.size()) return;
        Map<String, Object> target = others.get(cchoice - 1);

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "grant_title");
        payload.put("title_id", intVal(title, "id"));
        payload.put("target_id", intVal(target, "id"));
        Map<String, Object> res = api.action(payload);
        System.out.println("\n  " + (lastMessage(res).isEmpty() ? "已授予" : lastMessage(res)));
        pause();
    }

    /* ──────────────────── 招募骑士 ──────────────────── */

    private void actionKnights() {
        render("招募骑士");
        System.out.println("\n  招募精锐部队 (花费 25 金): 重骑兵+40 重步兵+80");
        System.out.println("  需要已有野战军。确认招募? (y/n)");
        String confirm = input("> ").trim().toLowerCase(Locale.ROOT);
        if (!confirm.equals("y") && !confirm.equals("yes") && !confirm.equals("是")) {
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "recruit_knights");
        Map<String, Object> res = api.action(payload);
        System.out.println("\n  " + (lastMessage(res).isEmpty() ? "操作完成" : lastMessage(res)));
        pause();
    }

    /* ──────────────────── 任命指挥官 ──────────────────── */

    private void actionCommander() {
        render("任命指挥官");
        Map<String, Object> snap = api.snapshot();
        List<Map<String, Object>> armies = filterPlayerArmies(snap);
        if (armies.isEmpty()) {
            System.out.println("\n  没有可指挥的军团");
            pause();
            return;
        }

        System.out.println("\n  选择军团:");
        for (int i = 0; i < armies.size(); i++) {
            Map<String, Object> a = armies.get(i);
            System.out.printf("    %d. %s (兵力:%d 位置:%s)%n",
                    i + 1, str(a, "name", "?"), intVal(a, "men"),
                    str(a, "location_name", "?"));
        }
        System.out.println("    0. 返回");

        int choice = inputInt("选择", 0);
        if (choice <= 0 || choice > armies.size()) return;
        Map<String, Object> army = armies.get(choice - 1);

        List<Map<String, Object>> chars = listMap(snap, "characters");
        int pid = api.playerId();
        List<Map<String, Object>> candidates = new ArrayList<>();
        for (Map<String, Object> c : chars) {
            if (intVal(c, "id") != pid && intVal(c, "age") >= 16) {
                candidates.add(c);
            }
        }
        if (candidates.isEmpty()) {
            System.out.println("\n  没有可任命的人选");
            pause();
            return;
        }

        System.out.println("\n  选择指挥官:");
        for (int i = 0; i < candidates.size(); i++) {
            Map<String, Object> c = candidates.get(i);
            Map<String, Object> attrs = mapVal(c, "attrs");
            int martial = attrs != null ? intVal(attrs, "martial") : 0;
            int prowess = attrs != null ? intVal(attrs, "prowess") : 0;
            System.out.printf("    %d. %s (军略:%d 勇武:%d)%n",
                    i + 1, str(c, "name", "?"), martial, prowess);
        }
        System.out.println("    0. 返回");

        int cchoice = inputInt("选择", 0);
        if (cchoice <= 0 || cchoice > candidates.size()) return;
        Map<String, Object> target = candidates.get(cchoice - 1);

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "set_commander");
        payload.put("army_id", intVal(army, "id"));
        payload.put("character_id", intVal(target, "id"));
        Map<String, Object> res = api.action(payload);
        System.out.println("\n  " + (lastMessage(res).isEmpty() ? "已任命" : lastMessage(res)));
        pause();
    }

    /* ──────────────────── 推进时间 ──────────────────── */

    private void actionAdvance() {
        render("推进时间");
        System.out.println("\n  推进天数 (建议 1/7/30/90/365):");
        System.out.println("    1. 1 天");
        System.out.println("    2. 1 周 (7天)");
        System.out.println("    3. 1 月 (30天)");
        System.out.println("    4. 1 季 (90天)");
        System.out.println("    5. 1 年 (365天)");
        System.out.println("    6. 自定义");
        System.out.println("    0. 返回");

        int choice = inputInt("选择", 0);
        int days;
        switch (choice) {
            case 0: return;
            case 1: days = 1; break;
            case 2: days = 7; break;
            case 3: days = 30; break;
            case 4: days = 90; break;
            case 5: days = 365; break;
            case 6:
                days = inputInt("天数", 1);
                days = Math.max(1, Math.min(365, days));
                break;
            default:
                System.out.println("  无效选择");
                pause();
                return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "advance");
        payload.put("days", days);
        Map<String, Object> res = api.action(payload);
        Map<String, Object> newSnap = api.snapshot();
        System.out.printf("%n  时间推进 %d 天 -> %s%n", days, str(newSnap, "date", "?"));
        String msg = lastMessage(res);
        if (!msg.isEmpty()) {
            System.out.println("  " + msg);
        }
        pause();
    }

    /* ──────────────────── 切换角色 ──────────────────── */

    private void actionSwitchPlayer() {
        render("切换角色");
        Map<String, Object> snap = api.snapshot();
        List<Map<String, Object>> playable = listMap(snap, "playable");
        int pid = api.playerId();

        System.out.println("\n  可选统治者:");
        for (int i = 0; i < playable.size(); i++) {
            Map<String, Object> p = playable.get(i);
            String marker = intVal(p, "id") == pid ? "*" : " ";
            System.out.printf("  %s%d. %s (%s)%n",
                    marker, i + 1, str(p, "name", "?"), str(p, "title", "无"));
        }
        System.out.println("    0. 返回");

        int choice = inputInt("选择", 0);
        if (choice <= 0 || choice > playable.size()) return;
        Map<String, Object> target = playable.get(choice - 1);

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "set_player");
        payload.put("character_id", intVal(target, "id"));
        api.action(payload);
        System.out.println("\n  已切换为 " + str(target, "name", "?"));
        pause();
    }

    /* ──────────────────── 新游戏 ──────────────────── */

    private String pickScenario() {
        List<ScenarioLoader.ScenarioInfo> scenarios = ScenarioLoader.listScenarios();
        System.out.println("\n  可选场景：");
        for (int i = 0; i < scenarios.size(); i++) {
            ScenarioLoader.ScenarioInfo s = scenarios.get(i);
            String desc = s.description.isEmpty() ? "" : " — " + s.description;
            System.out.println("    " + (i + 1) + ". " + s.name + " (" + s.id + ")" + desc);
        }
        String cur = api.simulation().scenarioId;
        String raw = input("  选择 [回车保持当前: " + cur + "] > ").trim();
        if (raw.isEmpty()) {
            return cur;
        }
        try {
            int idx = Integer.parseInt(raw);
            if (idx >= 1 && idx <= scenarios.size()) {
                return scenarios.get(idx - 1).id;
            }
        } catch (NumberFormatException ignored) {
            for (ScenarioLoader.ScenarioInfo s : scenarios) {
                if (s.id.equals(raw)) {
                    return raw;
                }
            }
        }
        System.out.println("  无效选择，保持当前场景");
        return cur;
    }

    private void actionNewGame() {
        render("新游戏");
        String scenario = pickScenario();
        System.out.println("\n  开始新游戏将丢失当前进度 (已有存档不受影响)。确认? (y/n)");
        String confirm = input("> ").trim().toLowerCase(Locale.ROOT);
        if (!confirm.equals("y") && !confirm.equals("yes") && !confirm.equals("是")) {
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "new_game");
        payload.put("scenario", scenario);
        api.action(payload);
        System.out.println("\n  新局开始（场景：" + scenario + "）");
        pause();
    }

    /* ──────────────────── 作弊开关 ──────────────────── */

    private void toggleCheat() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "toggle_cheat");
        Map<String, Object> res = api.action(payload);
        Map<String, Object> snap = api.snapshot();
        boolean cheat = bool(snap, "cheat_mode");
        boolean infiniteGold = bool(snap, "infinite_gold_mode");
        System.out.println("  作弊模式: " + (cheat ? "开启" : "关闭"));
        System.out.println("  无限金币: " + (infiniteGold ? "开启" : "关闭"));
        String msg = lastMessage(res);
        if (!msg.isEmpty()) {
            System.out.println("  " + msg);
        }
        pause();
    }

    /* ════════════════════ 子菜单 ════════════════════ */

    /* ──────────────────── 内阁管理 ──────────────────── */

    private void menuCouncil() {
        while (true) {
            render("内阁管理");
            Map<String, Object> snap = api.snapshot();
            Map<String, Object> council = mapVal(snap, "player_council");
            if (council == null || council.isEmpty()) {
                System.out.println("\n  暂无内阁");
                pause();
                return;
            }

            List<Map<String, Object>> members = listMap(council, "members");
            System.out.println("\n  内阁成员:");
            for (Map<String, Object> m : members) {
                System.out.printf("  %s: %s | 任务: %s%n",
                        str(m, "position_zh", "?"),
                        str(m, "holder_name", "?"),
                        str(m, "task_zh", "无"));
            }

            System.out.println("\n  1. 任命官员");
            System.out.println("  2. 分配任务");
            System.out.println("  0. 返回");
            String cmd = input("选择").trim();

            if (cmd.equals("0")) break;
            else if (cmd.equals("1")) menuAppoint();
            else if (cmd.equals("2")) menuTask();
        }
    }

    private void menuAppoint() {
        render("任命官员");
        Map<String, Object> snap = api.snapshot();
        String[][] positions = {
            {"CHANCELLOR", "首相"},
            {"MARSHAL", "元帅"},
            {"STEWARD", "总管"},
            {"SPYMASTER", "间谍总管"},
            {"COURT_CHAPLAIN", "宫廷神甫"},
        };

        System.out.println("\n  选择职位:");
        for (int i = 0; i < positions.length; i++) {
            System.out.printf("    %d. %s%n", i + 1, positions[i][1]);
        }
        System.out.println("    0. 返回");

        int choice = inputInt("选择", 0);
        if (choice <= 0 || choice > positions.length) return;
        String posName = positions[choice - 1][0];

        List<Map<String, Object>> chars = listMap(snap, "characters");
        int pid = api.playerId();
        List<Map<String, Object>> candidates = new ArrayList<>();
        for (Map<String, Object> c : chars) {
            if (intVal(c, "id") != pid && intVal(c, "age") >= 16) {
                candidates.add(c);
            }
        }
        if (candidates.isEmpty()) {
            System.out.println("\n  没有可选人选");
            pause();
            return;
        }

        System.out.println("\n  选择人选:");
        for (int i = 0; i < candidates.size(); i++) {
            Map<String, Object> c = candidates.get(i);
            System.out.printf("    %d. %s (%s, %d岁)%n",
                    i + 1, str(c, "name", "?"),
                    str(c, "title", "无头衔"), intVal(c, "age"));
        }
        System.out.println("    0. 返回");

        int cid = inputInt("选择", 0);
        if (cid <= 0 || cid > candidates.size()) return;

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "appoint_council");
        payload.put("position", posName);
        payload.put("character_id", intVal(candidates.get(cid - 1), "id"));
        api.action(payload);
        System.out.println("\n  已任命");
        pause();
    }

    private void menuTask() {
        render("分配任务");
        String[][] positions = {
            {"CHANCELLOR", "首相"},
            {"MARSHAL", "元帅"},
            {"STEWARD", "总管"},
            {"SPYMASTER", "间谍总管"},
            {"COURT_CHAPLAIN", "宫廷神甫"},
        };
        String[][] tasks = {
            {"DOMESTIC_RELATIONS", "内政外交"},
            {"FABRICATE_CLAIM", "伪造宣称"},
            {"TRAIN_COMMANDERS", "训练将领"},
            {"INCREASE_CONTROL", "强化控制"},
            {"COLLECT_TAXES", "催收税赋"},
            {"DEVELOP_COUNTY", "发展领地"},
            {"DISRUPT_SCHEMES", "破坏阴谋"},
            {"SUPPORT_MURDER", "协助密谋"},
            {"CONVERT_FAITH", "传播信仰"},
            {"RECRUIT_KNIGHTS", "招募骑士"},
            {"IMPROVE_DIPLOMACY", "改善外交"},
            {"SPREAD_CULTURE", "传播文化"},
            {"ESTABLISH_TRADE", "建立商路"},
            {"MAINTAIN_BUILDINGS", "维护建筑"},
            {"TRAIN_TROOPS", "训练部队"},
            {"GATHER_INTEL", "收集情报"},
            {"PROMOTE_CULTURE", "推广文化"},
        };

        System.out.println("\n  选择职位:");
        for (int i = 0; i < positions.length; i++) {
            System.out.printf("    %d. %s%n", i + 1, positions[i][1]);
        }
        System.out.println("    0. 返回");

        int choice = inputInt("选择", 0);
        if (choice <= 0 || choice > positions.length) return;
        String posName = positions[choice - 1][0];

        System.out.println("\n  选择任务:");
        for (int i = 0; i < tasks.length; i++) {
            System.out.printf("    %d. %s%n", i + 1, tasks[i][1]);
        }
        System.out.println("    0. 返回");

        int tchoice = inputInt("选择", 0);
        if (tchoice <= 0 || tchoice > tasks.length) return;
        String taskName = tasks[tchoice - 1][0];

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "assign_council_task");
        payload.put("position", posName);
        payload.put("task", taskName);
        api.action(payload);
        System.out.println("\n  任务已分配");
        pause();
    }

    /* ──────────────────── 阴谋 ──────────────────── */

    private void menuSchemes() {
        while (true) {
            render("阴谋活动");
            Map<String, Object> snap = api.snapshot();
            List<Map<String, Object>> schemes = listMap(snap, "player_schemes");
            if (schemes.isEmpty()) {
                System.out.println("\n  没有进行中的阴谋");
            } else {
                System.out.println("\n  进行中阴谋:");
                for (Map<String, Object> s : schemes) {
                    System.out.printf("    %s -> %s 进度:%.0f%%%n",
                            str(s, "kind_zh", "?"),
                            str(s, "target_name", "?"),
                            dbl(s, "progress"));
                }
            }

            System.out.println("\n  1. 发起阴谋");
            System.out.println("  0. 返回");
            String cmd = input("选择").trim();

            if (cmd.equals("0")) break;
            else if (cmd.equals("1")) actionStartScheme();
        }
    }

    private void actionStartScheme() {
        render("发起阴谋");
        Map<String, Object> snap = api.snapshot();
        List<Map<String, Object>> chars = listMap(snap, "characters");
        int pid = api.playerId();
        List<Map<String, Object>> targets = new ArrayList<>();
        for (Map<String, Object> c : chars) {
            if (intVal(c, "id") != pid && bool(c, "is_alive")) {
                targets.add(c);
            }
        }
        if (targets.isEmpty()) {
            System.out.println("\n  没有可选目标");
            pause();
            return;
        }

        System.out.println("\n  选择目标:");
        for (int i = 0; i < targets.size(); i++) {
            System.out.printf("    %d. %s%n", i + 1, str(targets.get(i), "name", "?"));
        }
        System.out.println("    0. 返回");

        int choice = inputInt("选择", 0);
        if (choice <= 0 || choice > targets.size()) return;
        Map<String, Object> target = targets.get(choice - 1);

        String[][] kinds = {
            {"MURDER", "谋杀"},
            {"ABDUCT", "绑架"},
            {"FABRICATE_HOOK", "伪造把柄"},
            {"SWAY", "拉拢"},
            {"SEDUCE", "引诱"},
            {"CLAIM_FABRICATION", "伪造宣称"},
        };

        System.out.println("\n  选择阴谋类型:");
        for (int i = 0; i < kinds.length; i++) {
            System.out.printf("    %d. %s%n", i + 1, kinds[i][1]);
        }
        System.out.println("    0. 返回");

        int kchoice = inputInt("选择", 0);
        if (kchoice <= 0 || kchoice > kinds.length) return;
        String kindName = kinds[kchoice - 1][0];

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "start_scheme");
        payload.put("scheme_kind", kindName);
        payload.put("target_id", intVal(target, "id"));
        Map<String, Object> res = api.action(payload);
        System.out.println("\n  " + (lastMessage(res).isEmpty() ? "阴谋已发起" : lastMessage(res)));
        pause();
    }

    /* ──────────────────── 外交 ──────────────────── */

    private void menuDiplomacy() {
        while (true) {
            render("外交行动");
            Map<String, Object> snap = api.snapshot();
            List<Map<String, Object>> chars = listMap(snap, "characters");
            int pid = api.playerId();
            List<Map<String, Object>> others = new ArrayList<>();
            for (Map<String, Object> c : chars) {
                if (intVal(c, "id") != pid && bool(c, "is_alive")) {
                    others.add(c);
                }
            }

            System.out.println("\n  可选角色:");
            for (int i = 0; i < others.size(); i++) {
                Map<String, Object> c = others.get(i);
                System.out.printf("    %d. %s (%s)%n",
                        i + 1, str(c, "name", "?"), str(c, "title", "无"));
            }
            System.out.println("    0. 返回主菜单");

            int choice = inputInt("选择", 0);
            if (choice <= 0 || choice > others.size()) break;
            Map<String, Object> target = others.get(choice - 1);
            int tid = intVal(target, "id");

            System.out.printf("%n  对 %s 的外交行动:%n", str(target, "name", "?"));
            System.out.println("    1.  结盟");
            System.out.println("    2.  签订互不侵犯");
            System.out.println("    3.  成为附庸");
            System.out.println("    4.  贸易协定");
            System.out.println("    5.  情报共享");
            System.out.println("    6.  联姻");
            System.out.println("    7.  赠送礼物");
            System.out.println("    8.  设为宿敌");
            System.out.println("    9.  邀请入宫廷 (30金)");
            System.out.println("   10. 为其举办宴会 (40金)");
            System.out.println("   11. 决斗");
            System.out.println("    0.  返回");

            String act = input("选择").trim();
            if (act.equals("0")) continue;

            Map<String, Object> payload = new HashMap<>();
            payload.put("target_id", tid);

            switch (act) {
                case "1":  payload.put("action", "form_alliance"); break;
                case "2":  payload.put("action", "form_non_aggression"); break;
                case "3":  payload.put("action", "form_vassalage"); break;
                case "4":  payload.put("action", "form_trade_agreement"); break;
                case "5":  payload.put("action", "form_intelligence_sharing"); break;
                case "6":  payload.put("action", "arrange_marriage"); break;
                case "7": {
                    double amount = inputDouble("金额 (1-500)", 50.0);
                    amount = Math.max(1, Math.min(500, amount));
                    payload.put("action", "send_gift");
                    payload.put("amount", amount);
                    break;
                }
                case "8":  payload.put("action", "set_rival"); break;
                case "9":  payload.put("action", "invite_to_court"); break;
                case "10": payload.put("action", "host_feast_for"); break;
                case "11": payload.put("action", "duel"); break;
                default:   System.out.println("  无效选择"); pause(); continue;
            }

            Map<String, Object> res = api.action(payload);
            System.out.println("\n  " + (lastMessage(res).isEmpty() ? "操作完成" : lastMessage(res)));
            pause();
        }
    }

    /* ──────────────────── 法律 ──────────────────── */

    private void menuLaws() {
        while (true) {
            render("法律管理");
            Map<String, Object> snap = api.snapshot();
            Map<String, Object> p = playerMap(snap);
            Map<String, Object> laws = mapVal(p, "laws");
            String succession = laws != null ? str(laws, "succession", "无") : "无";
            String crownAuth = laws != null ? str(laws, "crown_authority", "无") : "无";
            String genderLaw = laws != null ? str(laws, "gender_law", "无") : "无";

            System.out.printf("%n  当前继承法: %s%n", succession);
            System.out.printf("  当前王权:   %s%n", crownAuth);
            System.out.printf("  当前性别法: %s%n", genderLaw);
            System.out.println("\n  1. 更改继承法");
            System.out.println("  2. 更改王权");
            System.out.println("  3. 更改性别法");
            System.out.println("  0. 返回");

            String cmd = input("选择").trim();
            if (cmd.equals("0")) break;
            else if (cmd.equals("1")) {
                pickLaw("set_succession_law", "继承法", new String[][]{
                    {"PRIMOGENITURE", "长子继承制"},
                    {"ULTIMOGENITURE", "幼子继承制"},
                    {"ELECTIVE", "选举继承制"},
                    {"OPEN_ELECTIVE", "公开选举制"},
                }, false);
            } else if (cmd.equals("2")) {
                pickLaw("set_crown_authority", "王权", new String[][]{
                    {"0", "低王权"},
                    {"1", "中王权"},
                    {"2", "高王权"},
                }, true);
            } else if (cmd.equals("3")) {
                pickLaw("set_gender_law", "性别法", new String[][]{
                    {"AGNATIC", "男系继承"},
                    {"COGNATIC", "男女皆可继承"},
                    {"ABSOLUTE", "绝对平等继承"},
                }, false);
            }
        }
    }

    private void pickLaw(String actionName, String label, String[][] options, boolean isLevel) {
        System.out.printf("%n  选择%s:%n", label);
        for (int i = 0; i < options.length; i++) {
            System.out.printf("    %d. %s%n", i + 1, options[i][1]);
        }
        System.out.println("    0. 返回");

        int choice = inputInt("选择", 0);
        if (choice <= 0 || choice > options.length) return;

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", actionName);
        if (isLevel) {
            payload.put("level", Integer.parseInt(options[choice - 1][0]));
        } else {
            payload.put("law", options[choice - 1][0]);
        }
        Map<String, Object> res = api.action(payload);
        System.out.println("\n  " + (lastMessage(res).isEmpty() ? "已更改" : lastMessage(res)));
        pause();
    }

    /* ──────────────────── 存档管理 ──────────────────── */

    private void menuSaveLoad() {
        while (true) {
            render("存档管理");
            Map<String, Object> snap = api.snapshot();
            List<Map<String, Object>> saves = listMap(snap, "saves");

            System.out.println("\n  1. 保存游戏");
            System.out.println("  2. 读取存档");
            System.out.println("  3. 删除存档");
            System.out.println("  4. 新游戏");
            System.out.println("  0. 返回");

            if (!saves.isEmpty()) {
                System.out.println("\n  可用存档:");
                for (int i = 0; i < saves.size(); i++) {
                    Map<String, Object> s = saves.get(i);
                    System.out.printf("    %s (%s)%n",
                            str(s, "name", "?"), str(s, "date", "未知"));
                }
            }

            String cmd = input("选择").trim();
            if (cmd.equals("0")) break;
            else if (cmd.equals("1")) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("action", "save");
                api.action(payload);
                System.out.println("\n  已存档");
                pause();
            } else if (cmd.equals("2")) {
                menuLoad(saves);
            } else if (cmd.equals("3")) {
                menuDeleteSave(saves);
            } else if (cmd.equals("4")) {
                actionNewGame();
            }
        }
    }

    private void menuLoad(List<Map<String, Object>> saves) {
        if (saves.isEmpty()) {
            System.out.println("\n  没有存档");
            pause();
            return;
        }
        System.out.println("\n  选择存档:");
        for (int i = 0; i < saves.size(); i++) {
            Map<String, Object> s = saves.get(i);
            System.out.printf("    %d. %s (%s)%n",
                    i + 1, str(s, "name", "?"), str(s, "date", "未知"));
        }
        System.out.println("    0. 返回");

        int choice = inputInt("选择", 0);
        if (choice <= 0 || choice > saves.size()) return;
        String name = str(saves.get(choice - 1), "name", "");

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "load");
        payload.put("name", name);
        Map<String, Object> res = api.action(payload);
        System.out.println("\n  " + (lastMessage(res).isEmpty() ? "读档完成" : lastMessage(res)));
        pause();
    }

    private void menuDeleteSave(List<Map<String, Object>> saves) {
        if (saves.isEmpty()) {
            System.out.println("\n  没有存档");
            pause();
            return;
        }
        System.out.println("\n  选择要删除的存档:");
        for (int i = 0; i < saves.size(); i++) {
            Map<String, Object> s = saves.get(i);
            System.out.printf("    %d. %s (%s)%n",
                    i + 1, str(s, "name", "?"), str(s, "date", "未知"));
        }
        System.out.println("    0. 返回");

        int choice = inputInt("选择", 0);
        if (choice <= 0 || choice > saves.size()) return;
        String name = str(saves.get(choice - 1), "name", "");

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "delete_save");
        payload.put("name", name);
        Map<String, Object> res = api.action(payload);
        System.out.println("\n  " + (lastMessage(res).isEmpty() ? "已删除" : lastMessage(res)));
        pause();
    }

    /* ════════════════════ 工具方法 ════════════════════ */

    private String input(String prompt) {
        System.out.print("  " + prompt + " > ");
        System.out.flush();
        if (!scanner.hasNextLine()) return "";
        return scanner.nextLine();
    }

    private int inputInt(String prompt, int fallback) {
        String s = input(prompt).trim();
        Integer v = tryParse(s);
        return v != null ? v : fallback;
    }

    private double inputDouble(String prompt, double fallback) {
        String s = input(prompt).trim();
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void pause() {
        System.out.print("\n  按回车继续...");
        System.out.flush();
        if (scanner.hasNextLine()) {
            scanner.nextLine();
        }
    }

    private void clear() {
        if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
            try {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } catch (Exception e) {
                // fallback: print blank lines
                for (int i = 0; i < 50; i++) System.out.println();
            }
        } else {
            System.out.print("\033[H\033[2J");
            System.out.flush();
        }
    }

    /* ────── 安全类型转换 ────── */

    @SuppressWarnings("unchecked")
    private static Map<String, Object> playerMap(Map<String, Object> snap) {
        Object v = snap.get("player");
        return v instanceof Map ? (Map<String, Object>) v : new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapVal(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v instanceof Map ? (Map<String, Object>) v : null;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> listMap(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v instanceof List) {
            List<?> list = (List<?>) v;
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map) {
                    result.add((Map<String, Object>) item);
                }
            }
            return result;
        }
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    private static List<String> listStr(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v instanceof List) {
            List<?> list = (List<?>) v;
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                if (item != null) result.add(item.toString());
            }
            return result;
        }
        return new ArrayList<>();
    }

    private static String str(Map<String, Object> m, String key, String def) {
        Object v = m.get(key);
        return v != null ? v.toString() : def;
    }

    private static int intVal(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v instanceof Number) return ((Number) v).intValue();
        return 0;
    }

    private static double dbl(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v instanceof Number) return ((Number) v).doubleValue();
        return 0.0;
    }

    private static boolean bool(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v instanceof Boolean) return (Boolean) v;
        return false;
    }

    private static String lastMessage(Map<String, Object> res) {
        if (res == null) return "";
        Object msg = res.get("message");
        if (msg != null) return msg.toString();
        Object msgs = res.get("messages");
        if (msgs instanceof List) {
            List<?> list = (List<?>) msgs;
            if (!list.isEmpty()) {
                Object last = list.get(list.size() - 1);
                return last != null ? last.toString() : "";
            }
        }
        return "";
    }

    private static List<Map<String, Object>> filterPlayerCounties(Map<String, Object> snap) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> c : listMap(snap, "counties")) {
            if (bool(c, "is_player")) result.add(c);
        }
        return result;
    }

    private static List<Map<String, Object>> filterPlayerArmies(Map<String, Object> snap) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> a : listMap(snap, "armies")) {
            if (bool(a, "is_player") && !"DISBANDED".equals(str(a, "status", ""))) {
                result.add(a);
            }
        }
        return result;
    }

    private static Integer tryParse(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String f1(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    private static String f0(double v) {
        return String.format(Locale.ROOT, "%.0f", v);
    }
}
