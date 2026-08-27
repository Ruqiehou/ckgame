/**
 * Scheme (conspiracy) types.
 */
export declare enum SchemeKind {
    MURDER = "MURDER",
    ABDUCT = "ABDUCT",
    FABRICATE_HOOK = "FABRICATE_HOOK",
    SWAY = "SWAY",
    SEDUCE = "SEDUCE",
    CLAIM_FABRICATION = "CLAIM_FABRICATION"
}
export declare function schemeKindNameZh(kind: SchemeKind): string;
export declare function schemeKindBaseMonthlyProgress(kind: SchemeKind): number;
export declare function schemeKindSecrecyBase(kind: SchemeKind): number;
