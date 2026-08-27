/**
 * A character's claim on a title or county.
 */
export declare class Claim {
    readonly claimant: number;
    readonly title: number;
    readonly county: number | null;
    pressed: boolean;
    strength: number;
    constructor(claimant: number, title: number, county: number | null, strength?: number);
}
