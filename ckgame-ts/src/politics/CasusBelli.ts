/**
 * Casus belli (war justification) types.
 */
export enum CasusBelli {
  CLAIM = 'CLAIM',
  CONQUEST = 'CONQUEST',
  INDEPENDENCE = 'INDEPENDENCE',
  DEPOSE_LIEGE = 'DEPOSE_LIEGE',
  HOLY_WAR = 'HOLY_WAR',
  DE_JURE = 'DE_JURE',
  RIVALRY = 'RIVALRY',
  SUBJUGATION = 'SUBJUGATION',
  TRADE_WAR = 'TRADE_WAR',
}

const _nameZh: Record<CasusBelli, string> = {
  [CasusBelli.CLAIM]: '宣称战争',
  [CasusBelli.CONQUEST]: '征服',
  [CasusBelli.INDEPENDENCE]: '独立战争',
  [CasusBelli.DEPOSE_LIEGE]: '废黜领主',
  [CasusBelli.HOLY_WAR]: '圣战',
  [CasusBelli.DE_JURE]: '法理战争',
  [CasusBelli.RIVALRY]: '世仇战争',
  [CasusBelli.SUBJUGATION]: '臣服战争',
  [CasusBelli.TRADE_WAR]: '贸易战争',
};

const _warscoreGoal: Record<CasusBelli, number> = {
  [CasusBelli.CLAIM]: 80,
  [CasusBelli.CONQUEST]: 100,
  [CasusBelli.INDEPENDENCE]: 80,
  [CasusBelli.DEPOSE_LIEGE]: 100,
  [CasusBelli.HOLY_WAR]: 100,
  [CasusBelli.DE_JURE]: 80,
  [CasusBelli.RIVALRY]: 60,
  [CasusBelli.SUBJUGATION]: 100,
  [CasusBelli.TRADE_WAR]: 60,
};

const _prestigeCost: Record<CasusBelli, number> = {
  [CasusBelli.CLAIM]: 50,
  [CasusBelli.CONQUEST]: 100,
  [CasusBelli.INDEPENDENCE]: 0,
  [CasusBelli.DEPOSE_LIEGE]: 150,
  [CasusBelli.HOLY_WAR]: 0,
  [CasusBelli.DE_JURE]: 75,
  [CasusBelli.RIVALRY]: 25,
  [CasusBelli.SUBJUGATION]: 200,
  [CasusBelli.TRADE_WAR]: 50,
};

const _attackerPrestigeOnWin: Record<CasusBelli, number> = {
  [CasusBelli.CLAIM]: 50,
  [CasusBelli.CONQUEST]: 80,
  [CasusBelli.INDEPENDENCE]: 100,
  [CasusBelli.DEPOSE_LIEGE]: 120,
  [CasusBelli.HOLY_WAR]: 150,
  [CasusBelli.DE_JURE]: 60,
  [CasusBelli.RIVALRY]: 40,
  [CasusBelli.SUBJUGATION]: 200,
  [CasusBelli.TRADE_WAR]: 60,
};

export function cbNameZh(cb: CasusBelli): string {
  return _nameZh[cb] ?? cb;
}

export function cbWarscoreGoal(cb: CasusBelli): number {
  return _warscoreGoal[cb] ?? 100;
}

export function cbPrestigeCost(cb: CasusBelli): number {
  return _prestigeCost[cb] ?? 0;
}

export function cbAttackerPrestigeOnWin(cb: CasusBelli): number {
  return _attackerPrestigeOnWin[cb] ?? 0;
}
