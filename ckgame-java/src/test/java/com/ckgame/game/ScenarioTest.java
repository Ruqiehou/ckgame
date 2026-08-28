package com.ckgame.game;

import com.ckgame.core.Constants;
import com.ckgame.world.County;
import com.ckgame.world.World;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 1066 场景加载不变量，与 Python/Go/TS 版同口径。 */
class ScenarioTest {

    @Test
    void scenarioCounts() {
        World w = ScenarioLoader.loadScenario();
        assertEquals(25, w.characters.size());
        assertEquals(14, w.map.counties().size());
        assertEquals(19, w.titles.size());
        assertEquals(7, w.dynasties.size());
    }

    @Test
    void startDateIs10660101() {
        World w = ScenarioLoader.loadScenario();
        assertEquals(1066, w.date.year());
        assertEquals(1, w.date.month());
        assertEquals(1, w.date.day());
    }

    @Test
    void everyCountyHasHolderAndTitle() {
        World w = ScenarioLoader.loadScenario();
        for (Map.Entry<Integer, County> e : w.map.counties().entrySet()) {
            County c = e.getValue();
            assertNotEquals(Constants.NONE_ID, c.holder, c.name + " 无领主");
            assertNotEquals(Constants.NONE_ID, c.ownerTitle, c.name + " 无所属头衔");
            assertTrue(w.titles.containsKey(c.ownerTitle), c.name + " 的头衔不存在");
        }
    }
}
