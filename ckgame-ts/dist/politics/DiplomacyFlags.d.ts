/**
 * Two rulers' diplomatic relationship flags.
 */
export declare class DiplomacyFlags {
    allied: boolean;
    atWar: boolean;
    nonAggression: boolean;
    rival: boolean;
    marriagePact: boolean;
    vassalage: boolean;
    tradeAgreement: boolean;
    intelligenceSharing: boolean;
    /** Whether the current relationship blocks war declaration. */
    blocksWar(): boolean;
}
