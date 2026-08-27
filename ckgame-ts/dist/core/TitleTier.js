export var TitleTier;
(function (TitleTier) {
    TitleTier["BARONY"] = "BARONY";
    TitleTier["COUNTY"] = "COUNTY";
    TitleTier["DUCHY"] = "DUCHY";
    TitleTier["KINGDOM"] = "KINGDOM";
    TitleTier["EMPIRE"] = "EMPIRE";
})(TitleTier || (TitleTier = {}));
const rankNames = {
    [TitleTier.BARONY]: '男爵',
    [TitleTier.COUNTY]: '伯爵',
    [TitleTier.DUCHY]: '公爵',
    [TitleTier.KINGDOM]: '国王',
    [TitleTier.EMPIRE]: '皇帝',
};
const creationCosts = {
    [TitleTier.BARONY]: 0,
    [TitleTier.COUNTY]: 50,
    [TitleTier.DUCHY]: 200,
    [TitleTier.KINGDOM]: 500,
    [TitleTier.EMPIRE]: 1000,
};
export function tierRankName(tier) {
    return rankNames[tier] ?? tier;
}
export function tierCreationCost(tier) {
    return creationCosts[tier] ?? 0;
}
export function tierIsDestroyable(tier) {
    return tier === TitleTier.DUCHY || tier === TitleTier.KINGDOM || tier === TitleTier.EMPIRE;
}
//# sourceMappingURL=TitleTier.js.map