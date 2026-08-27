export enum TitleTier {
  BARONY = 'BARONY',
  COUNTY = 'COUNTY',
  DUCHY = 'DUCHY',
  KINGDOM = 'KINGDOM',
  EMPIRE = 'EMPIRE',
}

const rankNames: Record<TitleTier, string> = {
  [TitleTier.BARONY]: '男爵',
  [TitleTier.COUNTY]: '伯爵',
  [TitleTier.DUCHY]: '公爵',
  [TitleTier.KINGDOM]: '国王',
  [TitleTier.EMPIRE]: '皇帝',
};

const creationCosts: Record<TitleTier, number> = {
  [TitleTier.BARONY]: 0,
  [TitleTier.COUNTY]: 50,
  [TitleTier.DUCHY]: 200,
  [TitleTier.KINGDOM]: 500,
  [TitleTier.EMPIRE]: 1000,
};

export function tierRankName(tier: TitleTier): string {
  return rankNames[tier] ?? tier;
}

export function tierCreationCost(tier: TitleTier): number {
  return creationCosts[tier] ?? 0;
}

export function tierIsDestroyable(tier: TitleTier): boolean {
  return tier === TitleTier.DUCHY || tier === TitleTier.KINGDOM || tier === TitleTier.EMPIRE;
}
