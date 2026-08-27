/**
 * Council positions.
 */
export var CouncilPosition;
(function (CouncilPosition) {
    CouncilPosition["CHANCELLOR"] = "CHANCELLOR";
    CouncilPosition["MARSHAL"] = "MARSHAL";
    CouncilPosition["STEWARD"] = "STEWARD";
    CouncilPosition["SPYMASTER"] = "SPYMASTER";
    CouncilPosition["COURT_CHAPLAIN"] = "COURT_CHAPLAIN";
})(CouncilPosition || (CouncilPosition = {}));
const councilPositionNames = {
    [CouncilPosition.CHANCELLOR]: '首相',
    [CouncilPosition.MARSHAL]: '元帅',
    [CouncilPosition.STEWARD]: '总管',
    [CouncilPosition.SPYMASTER]: '间谍总管',
    [CouncilPosition.COURT_CHAPLAIN]: '宫廷神甫',
};
export function councilPositionNameZh(pos) {
    return councilPositionNames[pos] ?? pos;
}
/** All council positions. */
export function allCouncilPositions() {
    return [
        CouncilPosition.CHANCELLOR,
        CouncilPosition.MARSHAL,
        CouncilPosition.STEWARD,
        CouncilPosition.SPYMASTER,
        CouncilPosition.COURT_CHAPLAIN,
    ];
}
//# sourceMappingURL=CouncilPosition.js.map