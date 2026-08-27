/**
 * Faction types.
 */
export var FactionKind;
(function (FactionKind) {
    FactionKind["INDEPENDENCE"] = "INDEPENDENCE";
    FactionKind["LOWER_CROWN_AUTHORITY"] = "LOWER_CROWN_AUTHORITY";
    FactionKind["CLAIMANT"] = "CLAIMANT";
    FactionKind["LIBERTY"] = "LIBERTY";
    FactionKind["POPULAR"] = "POPULAR";
})(FactionKind || (FactionKind = {}));
const _nameZh = {
    [FactionKind.INDEPENDENCE]: '独立派系',
    [FactionKind.LOWER_CROWN_AUTHORITY]: '降低王权派系',
    [FactionKind.CLAIMANT]: '拥立派系',
    [FactionKind.LIBERTY]: '自由派系',
    [FactionKind.POPULAR]: '民变派系',
};
const _ultimatumText = {
    [FactionKind.INDEPENDENCE]: '要求独立，建立自己的政权',
    [FactionKind.LOWER_CROWN_AUTHORITY]: '要求限制王权，扩大贵族议会权力',
    [FactionKind.CLAIMANT]: '要求废黜当前君主，拥立新君',
    [FactionKind.LIBERTY]: '要求恢复自由权利，减免赋税',
    [FactionKind.POPULAR]: '要求改革苛政，减轻百姓负担',
};
export function factionKindNameZh(kind) {
    return _nameZh[kind] ?? kind;
}
export function factionKindUltimatumText(kind) {
    return _ultimatumText[kind] ?? '';
}
//# sourceMappingURL=FactionKind.js.map