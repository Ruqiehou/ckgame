/**
 * Casus belli (war justification) types.
 */
export var CasusBelli;
(function (CasusBelli) {
    CasusBelli["CLAIM"] = "CLAIM";
    CasusBelli["CONQUEST"] = "CONQUEST";
    CasusBelli["INDEPENDENCE"] = "INDEPENDENCE";
    CasusBelli["DEPOSE_LIEGE"] = "DEPOSE_LIEGE";
    CasusBelli["HOLY_WAR"] = "HOLY_WAR";
    CasusBelli["DE_JURE"] = "DE_JURE";
    CasusBelli["RIVALRY"] = "RIVALRY";
    CasusBelli["SUBJUGATION"] = "SUBJUGATION";
    CasusBelli["TRADE_WAR"] = "TRADE_WAR";
})(CasusBelli || (CasusBelli = {}));
const _nameZh = {
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
const _warscoreGoal = {
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
const _prestigeCost = {
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
const _attackerPrestigeOnWin = {
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
export function cbNameZh(cb) {
    return _nameZh[cb] ?? cb;
}
export function cbWarscoreGoal(cb) {
    return _warscoreGoal[cb] ?? 100;
}
export function cbPrestigeCost(cb) {
    return _prestigeCost[cb] ?? 0;
}
export function cbAttackerPrestigeOnWin(cb) {
    return _attackerPrestigeOnWin[cb] ?? 0;
}
//# sourceMappingURL=CasusBelli.js.map