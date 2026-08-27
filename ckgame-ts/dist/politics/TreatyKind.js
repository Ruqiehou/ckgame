/**
 * Treaty types between rulers.
 */
export var TreatyKind;
(function (TreatyKind) {
    TreatyKind["ALLIANCE"] = "ALLIANCE";
    TreatyKind["NON_AGGRESSION"] = "NON_AGGRESSION";
    TreatyKind["MARRIAGE_PACT"] = "MARRIAGE_PACT";
    TreatyKind["TRUCE"] = "TRUCE";
    TreatyKind["VASSALAGE"] = "VASSALAGE";
    TreatyKind["TRADE_AGREEMENT"] = "TRADE_AGREEMENT";
    TreatyKind["INTELLIGENCE_SHARING"] = "INTELLIGENCE_SHARING";
})(TreatyKind || (TreatyKind = {}));
const treatyKindNames = {
    [TreatyKind.ALLIANCE]: '同盟',
    [TreatyKind.NON_AGGRESSION]: '互不侵犯',
    [TreatyKind.MARRIAGE_PACT]: '联姻协定',
    [TreatyKind.TRUCE]: '停战',
    [TreatyKind.VASSALAGE]: '附庸关系',
    [TreatyKind.TRADE_AGREEMENT]: '贸易协定',
    [TreatyKind.INTELLIGENCE_SHARING]: '情报共享',
};
export function treatyKindNameZh(k) {
    return treatyKindNames[k] ?? k;
}
//# sourceMappingURL=TreatyKind.js.map