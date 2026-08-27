/**
 * Council positions.
 */
export declare enum CouncilPosition {
    CHANCELLOR = "CHANCELLOR",
    MARSHAL = "MARSHAL",
    STEWARD = "STEWARD",
    SPYMASTER = "SPYMASTER",
    COURT_CHAPLAIN = "COURT_CHAPLAIN"
}
export declare function councilPositionNameZh(pos: CouncilPosition): string;
/** All council positions. */
export declare function allCouncilPositions(): CouncilPosition[];
