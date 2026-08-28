package com.ckgame.game;

import com.ckgame.world.Character;
import com.ckgame.world.World;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 主循环稳定性测试：按日/按月推进不崩溃，状态保持一致。 */
class GameSimulationTest {

    @Test
    void runDaysAdvancesDateAndTick() {
        GameSimulation sim = new GameSimulation();
        World w = sim.world;
        int startYear = w.date.year();
        int startMonth = w.date.month();
        sim.runDays(70);
        assertEquals(70, w.tick);
        int elapsedMonths = (w.date.year() - startYear) * 12 + (w.date.month() - startMonth);
        assertTrue(elapsedMonths >= 2, "70 天应跨过至少两个月度结算");
    }

    @Test
    void quarterOfStableSimulation() {
        GameSimulation sim = new GameSimulation();
        sim.runDays(92);
        List<Character> rulers = sim.world.rulers();
        assertTrue(rulers.size() >= 1);
        for (Character r : rulers) {
            assertNotNull(sim.world.character(r.id));
        }
    }

    @Test
    void monthStartTriggersMonthlySettlement() {
        GameSimulation sim = new GameSimulation();
        World w = sim.world;
        int startMonth = w.date.month();
        for (int i = 0; i < 40; i++) {
            if (w.date.day() == 1 && w.tick > 0) break;
            sim.tickDay();
        }
        assertEquals(1, w.date.day());
        assertNotEquals(startMonth, w.date.month());
    }
}
