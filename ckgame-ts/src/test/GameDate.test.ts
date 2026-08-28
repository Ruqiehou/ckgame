import { test } from 'node:test';
import assert from 'node:assert/strict';
import { GameDate } from '../core/calendar/GameDate.js';
import { Season } from '../core/calendar/Season.js';

function ymd(d: GameDate): [number, number, number] {
  return [d.year(), d.month(), d.day()];
}

test('跨月推进日期', () => {
  assert.deepEqual(ymd(new GameDate(1066, 1, 30).advanceDays(3)), [1066, 2, 2]);
  assert.deepEqual(ymd(new GameDate(1066, 12, 31).advanceDays(1)), [1067, 1, 1]);
  assert.deepEqual(ymd(new GameDate(1066, 2, 28).advanceDays(1)), [1066, 3, 1]);
});

test('闰年二月', () => {
  assert.ok(new GameDate(1064, 1, 1).isLeapYear());
  assert.ok(!new GameDate(1066, 1, 1).isLeapYear());
  assert.deepEqual(ymd(new GameDate(1064, 2, 28).advanceDays(1)), [1064, 2, 29]);
});

test('季节划分', () => {
  assert.equal(new GameDate(1066, 1, 15).season(), Season.WINTER);
  assert.equal(new GameDate(1066, 3, 15).season(), Season.SPRING);
  assert.equal(new GameDate(1066, 7, 15).season(), Season.SUMMER);
  assert.equal(new GameDate(1066, 10, 15).season(), Season.AUTUMN);
  assert.equal(new GameDate(1066, 12, 15).season(), Season.WINTER);
});

test('月初/年初判断', () => {
  assert.ok(new GameDate(1066, 1, 1).isMonthStart());
  assert.ok(!new GameDate(1066, 1, 2).isMonthStart());
  assert.ok(new GameDate(1066, 1, 1).isYearStart());
});

test('同龄人年龄序（当月当天）', () => {
  // daysUntil 基于序数，仅在同月内为天数差
  assert.equal(new GameDate(1066, 1, 1).daysUntil(new GameDate(1066, 1, 2)), 1);
});
