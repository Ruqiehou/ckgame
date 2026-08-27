/**
 * Faction types.
 */
export enum FactionKind {
  INDEPENDENCE = 'INDEPENDENCE',
  LOWER_CROWN_AUTHORITY = 'LOWER_CROWN_AUTHORITY',
  CLAIMANT = 'CLAIMANT',
  LIBERTY = 'LIBERTY',
  POPULAR = 'POPULAR',
}

const _nameZh: Record<FactionKind, string> = {
  [FactionKind.INDEPENDENCE]: '独立派系',
  [FactionKind.LOWER_CROWN_AUTHORITY]: '降低王权派系',
  [FactionKind.CLAIMANT]: '拥立派系',
  [FactionKind.LIBERTY]: '自由派系',
  [FactionKind.POPULAR]: '民变派系',
};

const _ultimatumText: Record<FactionKind, string> = {
  [FactionKind.INDEPENDENCE]: '要求独立，建立自己的政权',
  [FactionKind.LOWER_CROWN_AUTHORITY]: '要求限制王权，扩大贵族议会权力',
  [FactionKind.CLAIMANT]: '要求废黜当前君主，拥立新君',
  [FactionKind.LIBERTY]: '要求恢复自由权利，减免赋税',
  [FactionKind.POPULAR]: '要求改革苛政，减轻百姓负担',
};

export function factionKindNameZh(kind: FactionKind): string {
  return _nameZh[kind] ?? kind;
}

export function factionKindUltimatumText(kind: FactionKind): string {
  return _ultimatumText[kind] ?? '';
}
