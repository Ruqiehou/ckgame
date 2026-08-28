import { test } from 'node:test';
import assert from 'node:assert/strict';
import { GameSimulation } from '../game/GameSimulation.js';

test('按日推进更新日期与计数', () => {
  const sim = new GameSimulation();
  const start = sim.world.date;
  sim.runDays(70);
  assert.equal((sim.world as any).tick, 70);
  assert.ok(sim.world.date.daysUntil(start) < 0, '日期应已前进');
  const elapsedMonths =
    (sim.world.date.year() - start.year()) * 12 + (sim.world.date.month() - start.month());
  assert.ok(elapsedMonths >= 2);
});

test('一个季度的模拟保持稳定', () => {
  const sim = new GameSimulation();
  sim.runDays(92); // 覆盖至少 3 次月度结算
  const rulers = sim.world.rulers();
  assert.ok(rulers.length >= 1);
  for (const r of rulers) {
    assert.ok(sim.world.character(r.id), `统治者 ${r.id} 应仍存在`);
  }
});

test('月初触发月度结算', () => {
  const sim = new GameSimulation();
  const startMonth = sim.world.date.month();
  for (let i = 0; i < 40; i++) {
    if (sim.world.date.day() === 1 && (sim.world as any).tick > 0) break;
    sim.tickDay();
  }
  assert.equal(sim.world.date.day(), 1);
  assert.notEqual(sim.world.date.month(), startMonth);
});
