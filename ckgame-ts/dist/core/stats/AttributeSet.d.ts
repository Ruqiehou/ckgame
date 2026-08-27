export declare class AttributeSet {
    readonly diplomacy: number;
    readonly martial: number;
    readonly stewardship: number;
    readonly intrigue: number;
    readonly learning: number;
    readonly prowess: number;
    constructor(diplomacy?: number, martial?: number, stewardship?: number, intrigue?: number, learning?: number, prowess?: number);
    static zero(): AttributeSet;
    static defaults(): AttributeSet;
    add(other: AttributeSet): AttributeSet;
    clamp(lo: number, hi: number): AttributeSet;
    total(): number;
    toString(): string;
}
