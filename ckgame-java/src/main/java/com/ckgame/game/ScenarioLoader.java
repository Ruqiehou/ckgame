package com.ckgame.game;

import com.ckgame.core.Gender;
import com.ckgame.core.TitleTier;
import com.ckgame.core.calendar.GameDate;
import com.ckgame.core.stats.AttributeSet;
import com.ckgame.world.Character;
import com.ckgame.world.County;
import com.ckgame.world.Dynasty;
import com.ckgame.world.Terrain;
import com.ckgame.world.Title;
import com.ckgame.world.TradeRoute;
import com.ckgame.world.World;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 从 JSON 数据构建场景世界。
 * 对应 Python 的 ck_engine.game.scenario_loader.load_scenario。
 */
public final class ScenarioLoader {

    /** 默认场景数据的 classpath 资源路径。 */
    public static final String DEFAULT_RESOURCE = "/data/scenarios/1066.json";

    private ScenarioLoader() {}

    /** JSON 数组 [year, month, day] 转 GameDate。 */
    private static GameDate date(JsonNode arr) {
        return new GameDate(arr.get(0).asInt(), arr.get(1).asInt(), arr.get(2).asInt());
    }

    /** 加载默认 1066 场景（从 classpath 资源读取）。 */
    public static World loadScenario() {
        return loadScenario(null);
    }

    /**
     * 加载场景。
     *
     * @param path 文件系统路径；为 null/空白时优先从 classpath 资源 {@link #DEFAULT_RESOURCE} 读取
     */
    public static World loadScenario(String path) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode data;
        try {
            if (path != null && !path.isBlank()) {
                // 支持直接传入文件系统路径
                try (InputStream in = Files.newInputStream(Path.of(path))) {
                    data = mapper.readTree(in);
                }
            } else {
                InputStream in = ScenarioLoader.class.getResourceAsStream(DEFAULT_RESOURCE);
                if (in == null) {
                    throw new IllegalStateException("找不到场景资源：" + DEFAULT_RESOURCE);
                }
                try (in) {
                    data = mapper.readTree(in);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("加载场景失败", e);
        }

        // 起始日期
        JsonNode startNode = data.path("start_date");
        GameDate start = (startNode.isArray() && startNode.size() == 3)
                ? date(startNode)
                : new GameDate(1066, 1, 1);
        World world = new World(start);

        // 加载贸易路线数据
        if (data.has("trade_routes")) {
            List<TradeRoute> routes = new ArrayList<>();
            for (JsonNode row : data.get("trade_routes")) {
                routes.add(new TradeRoute(
                        row.path("from").asText(),
                        row.path("to").asText(),
                        row.path("exchange_rate").asDouble(1.0),
                        row.path("trade_volume").asDouble(0.0)));
            }
            world.loadTradeRoutes(routes);
        }

        // 王朝
        Map<String, Integer> dyn = new HashMap<>();
        for (JsonNode row : data.path("dynasties")) {
            int did = world.createDynasty(row.get("name").asText());
            dyn.put(row.get("key").asText(), did);
            Dynasty d = world.dynasties.get(did);
            if (d == null) {
                continue;
            }
            if (row.has("color")) {
                JsonNode color = row.get("color");
                d.colorR = color.get(0).asInt();
                d.colorG = color.get(1).asInt();
                d.colorB = color.get(2).asInt();
            }
            if (row.has("motto")) {
                d.motto = row.get("motto").asText();
            }
        }

        // 省份
        Map<String, Integer> counties = new HashMap<>();
        for (JsonNode row : data.path("counties")) {
            int cid = world.createCounty(row.get("name").asText(),
                    Terrain.valueOf(row.get("terrain").asText()));
            counties.put(row.get("key").asText(), cid);
            County c = world.map.get(cid);
            if (c == null) {
                continue;
            }
            c.key = row.get("key").asText();
            c.development = row.path("development").asInt(c.development);
            c.levies = row.path("levies").asInt(c.levies);
            c.tax = row.path("tax").asDouble(c.tax);
            c.fortLevel = row.path("fort").asInt(c.fortLevel);
            List<String> buildings = new ArrayList<>();
            buildings.add("庄园");
            buildings.add("市场");
            if (c.fortLevel >= 2) {
                buildings.add("城堡");
            }
            c.buildings = buildings;
            c.tradeRouteProtected = row.path("trade_route_protected").asBoolean(false);
            c.tradeRouteProtectionLevel = row.path("trade_route_protection_level").asInt(0);
            c.hasPort = row.path("has_port").asBoolean(false);
            c.portLevel = row.path("port_level").asInt(0);
            c.tradeRouteMaintenanceLevel = row.path("trade_route_maintenance_level").asInt(0);
        }

        // 省份邻接关系
        for (JsonNode pair : data.path("connections")) {
            world.map.connect(counties.get(pair.get(0).asText()), counties.get(pair.get(1).asText()));
        }

        // 头衔
        Map<String, Integer> titles = new HashMap<>();
        for (JsonNode row : data.path("titles")) {
            int tid = world.createTitle(row.get("name").asText(),
                    TitleTier.valueOf(row.get("tier").asText()));
            titles.put(row.get("key").asText(), tid);
        }

        // 头衔-省份绑定：伯爵领头衔与省份绑定，高阶头衔仅记录
        JsonNode titleCounties = data.path("title_counties");
        Iterator<Map.Entry<String, JsonNode>> fields = titleCounties.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            Integer tid = titles.get(entry.getKey());
            if (tid == null) {
                continue;
            }
            for (JsonNode ckNode : entry.getValue()) {
                Integer cid = counties.get(ckNode.asText());
                if (cid == null) {
                    continue;
                }
                Title t = world.title(tid);
                if (t != null && t.tier == TitleTier.COUNTY) {
                    world.attachCountyToTitle(cid, tid);
                } else if (t != null && !t.counties.contains(cid)) {
                    t.counties.add(cid);
                }
            }
        }

        // 封臣关系
        for (JsonNode pair : data.path("vassals")) {
            world.setVassal(titles.get(pair.get(0).asText()), titles.get(pair.get(1).asText()));
        }

        // 人物
        Map<String, Integer> chars = new HashMap<>();
        for (JsonNode row : data.path("characters")) {
            int cid = world.createCharacter(
                    row.get("name").asText(),
                    dyn.get(row.get("dynasty").asText()),
                    Gender.valueOf(row.get("gender").asText()),
                    date(row.get("birth")));
            chars.put(row.get("key").asText(), cid);
            Character ch = world.character(cid);
            if (ch == null) {
                continue;
            }
            ch.culture = row.path("culture").asInt(0);
            ch.faith = row.path("faith").asInt(0);
            if (row.has("attrs")) {
                JsonNode a = row.get("attrs");
                ch.baseAttrs = new AttributeSet(
                        a.get(0).asInt(), a.get(1).asInt(), a.get(2).asInt(),
                        a.get(3).asInt(), a.get(4).asInt(), a.get(5).asInt());
            }
            if (row.has("gold")) {
                ch.gold = row.get("gold").asDouble();
            }
            if (row.has("prestige")) {
                ch.prestige = row.get("prestige").asDouble();
            }
            for (JsonNode t : row.path("traits")) {
                int tid = t.asInt();
                if (!ch.traits.contains(tid)) {
                    ch.traits.add(tid);
                }
            }
        }

        // 父母
        for (JsonNode triple : data.path("parents")) {
            world.setParents(
                    chars.get(triple.get(0).asText()),
                    chars.get(triple.get(1).asText()),
                    chars.get(triple.get(2).asText()));
        }

        // 婚姻
        for (JsonNode pair : data.path("marriages")) {
            world.marry(chars.get(pair.get(0).asText()), chars.get(pair.get(1).asText()));
        }

        // 头衔授予
        for (JsonNode pair : data.path("grants")) {
            world.grantTitle(titles.get(pair.get(0).asText()), chars.get(pair.get(1).asText()));
        }

        // 授予后的封臣关系
        for (JsonNode pair : data.path("post_grants_vassals")) {
            world.setVassal(titles.get(pair.get(0).asText()), titles.get(pair.get(1).asText()));
        }

        // 再次填充高阶头衔的 counties 列表（与旧场景一致）
        fields = titleCounties.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            Integer tid = titles.get(entry.getKey());
            if (tid == null) {
                continue;
            }
            Title t = world.title(tid);
            if (t == null) {
                continue;
            }
            for (JsonNode ckNode : entry.getValue()) {
                Integer cid = counties.get(ckNode.asText());
                if (cid != null && !t.counties.contains(cid)) {
                    t.counties.add(cid);
                }
            }
        }

        // 好感度
        for (JsonNode triple : data.path("opinions")) {
            world.modifyOpinion(
                    chars.get(triple.get(0).asText()),
                    chars.get(triple.get(1).asText()),
                    triple.get(2).asInt());
        }

        // 场景日志
        for (JsonNode line : data.path("logs")) {
            world.pushLog(line.asText());
        }

        return world;
    }
}
