/**
 * Casus belli (war justification) types.
 */
export declare enum CasusBelli {
    CLAIM = "CLAIM",
    CONQUEST = "CONQUEST",
    INDEPENDENCE = "INDEPENDENCE",
    DEPOSE_LIEGE = "DEPOSE_LIEGE",
    HOLY_WAR = "HOLY_WAR",
    DE_JURE = "DE_JURE",
    RIVALRY = "RIVALRY",
    SUBJUGATION = "SUBJUGATION",
    TRADE_WAR = "TRADE_WAR"
}
export declare function cbNameZh(cb: CasusBelli): string;
export declare function cbWarscoreGoal(cb: CasusBelli): number;
export declare function cbPrestigeCost(cb: CasusBelli): number;
export declare function cbAttackerPrestigeOnWin(cb: CasusBelli): number;
