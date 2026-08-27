/**
 * Treaty types between rulers.
 */
export enum TreatyKind {
  ALLIANCE = 'ALLIANCE',
  NON_AGGRESSION = 'NON_AGGRESSION',
  MARRIAGE_PACT = 'MARRIAGE_PACT',
  TRUCE = 'TRUCE',
  VASSALAGE = 'VASSALAGE',
  TRADE_AGREEMENT = 'TRADE_AGREEMENT',
  INTELLIGENCE_SHARING = 'INTELLIGENCE_SHARING',
}

const treatyKindNames: Record<TreatyKind, string> = {
  [TreatyKind.ALLIANCE]: '同盟',
  [TreatyKind.NON_AGGRESSION]: '互不侵犯',
  [TreatyKind.MARRIAGE_PACT]: '联姻协定',
  [TreatyKind.TRUCE]: '停战',
  [TreatyKind.VASSALAGE]: '附庸关系',
  [TreatyKind.TRADE_AGREEMENT]: '贸易协定',
  [TreatyKind.INTELLIGENCE_SHARING]: '情报共享',
};

export function treatyKindNameZh(k: TreatyKind): string {
  return treatyKindNames[k] ?? k;
}
