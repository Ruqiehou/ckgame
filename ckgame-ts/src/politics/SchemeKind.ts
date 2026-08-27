/**
 * Scheme (conspiracy) types.
 */
export enum SchemeKind {
  MURDER = 'MURDER',
  ABDUCT = 'ABDUCT',
  FABRICATE_HOOK = 'FABRICATE_HOOK',
  SWAY = 'SWAY',
  SEDUCE = 'SEDUCE',
  CLAIM_FABRICATION = 'CLAIM_FABRICATION',
}

const _nameZh: Record<SchemeKind, string> = {
  [SchemeKind.MURDER]: '谋杀',
  [SchemeKind.ABDUCT]: '绑架',
  [SchemeKind.FABRICATE_HOOK]: '伪造把柄',
  [SchemeKind.SWAY]: '拉拢',
  [SchemeKind.SEDUCE]: '引诱',
  [SchemeKind.CLAIM_FABRICATION]: '伪造宣称',
};

const _baseMonthlyProgress: Record<SchemeKind, number> = {
  [SchemeKind.MURDER]: 5.0,
  [SchemeKind.ABDUCT]: 4.0,
  [SchemeKind.FABRICATE_HOOK]: 6.0,
  [SchemeKind.SWAY]: 8.0,
  [SchemeKind.SEDUCE]: 7.0,
  [SchemeKind.CLAIM_FABRICATION]: 3.0,
};

const _secrecyBase: Record<SchemeKind, number> = {
  [SchemeKind.MURDER]: 40.0,
  [SchemeKind.ABDUCT]: 35.0,
  [SchemeKind.FABRICATE_HOOK]: 50.0,
  [SchemeKind.SWAY]: 80.0,
  [SchemeKind.SEDUCE]: 60.0,
  [SchemeKind.CLAIM_FABRICATION]: 70.0,
};

export function schemeKindNameZh(kind: SchemeKind): string {
  return _nameZh[kind] ?? kind;
}

export function schemeKindBaseMonthlyProgress(kind: SchemeKind): number {
  return _baseMonthlyProgress[kind] ?? 5.0;
}

export function schemeKindSecrecyBase(kind: SchemeKind): number {
  return _secrecyBase[kind] ?? 50.0;
}
