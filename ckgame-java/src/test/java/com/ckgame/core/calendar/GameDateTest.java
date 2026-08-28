package com.ckgame.core.calendar;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 日期算术测试，与 Python/Go/TS 版同口径。 */
class GameDateTest {

    @Test
    void advanceDaysCrossesMonths() {
        assertEquals(new GameDate(1066, 2, 2), new GameDate(1066, 1, 30).advanceDays(3));
        assertEquals(new GameDate(1067, 1, 1), new GameDate(1066, 12, 31).advanceDays(1));
        assertEquals(new GameDate(1066, 3, 1), new GameDate(1066, 2, 28).advanceDays(1));
    }

    @Test
    void leapYearFebruary() {
        assertTrue(new GameDate(1064, 1, 1).isLeapYear());
        assertFalse(new GameDate(1066, 1, 1).isLeapYear());
        assertEquals(new GameDate(1064, 2, 29), new GameDate(1064, 2, 28).advanceDays(1));
    }

    @Test
    void seasonBoundaries() {
        assertEquals(Season.WINTER, new GameDate(1066, 1, 15).season());
        assertEquals(Season.SPRING, new GameDate(1066, 3, 15).season());
        assertEquals(Season.SUMMER, new GameDate(1066, 7, 15).season());
        assertEquals(Season.AUTUMN, new GameDate(1066, 10, 15).season());
        assertEquals(Season.WINTER, new GameDate(1066, 12, 15).season());
    }

    @Test
    void monthAndYearStart() {
        assertTrue(new GameDate(1066, 1, 1).isMonthStart());
        assertFalse(new GameDate(1066, 1, 2).isMonthStart());
        assertTrue(new GameDate(1066, 1, 1).isYearStart());
    }

    @Test
    void daysUntilWithinSameMonth() {
        assertEquals(1, new GameDate(1066, 1, 1).daysUntil(new GameDate(1066, 1, 2)));
    }
}
