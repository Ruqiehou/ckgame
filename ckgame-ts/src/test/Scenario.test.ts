import { test } from 'node:test';
import assert from 'node:assert/strict';
import { ScenarioLoader } from '../game/ScenarioLoader.js';

const world = ScenarioLoader.loadScenario();

test('1066 场景数据规模', () => {
  assert.equal(world.characters.size, 25);
  assert.equal(world.map.counties.size, 14);
  assert.equal(world.titles.size, 19);
  assert.equal(world.dynasties.size, 7);
});

test('起始日期为 1066-01-01', () => {
  assert.equal(world.date.year(), 1066);
  assert.equal(world.date.month(), 1);
  assert.equal(world.date.day(), 1);
});

test('每个省份都有领主与所属头衔', () => {
  for (const county of world.map.counties.values()) {
    assert.notEqual(county.holder, 0, `${county.name} 无领主`);
    assert.ok(county.ownerTitle, `${county.name} 无所属头衔`);
  }
});

test('存在贸易路线', () => {
  const routes = (world as any).tradeRoutes;
  assert.ok(Array.isArray(routes) && routes.length >= 1);
});
