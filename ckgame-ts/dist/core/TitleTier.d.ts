export declare enum TitleTier {
    BARONY = "BARONY",
    COUNTY = "COUNTY",
    DUCHY = "DUCHY",
    KINGDOM = "KINGDOM",
    EMPIRE = "EMPIRE"
}
export declare function tierRankName(tier: TitleTier): string;
export declare function tierCreationCost(tier: TitleTier): number;
export declare function tierIsDestroyable(tier: TitleTier): boolean;
