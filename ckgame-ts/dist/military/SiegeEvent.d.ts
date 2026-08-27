/**
 * Siege event (progress, capture, or lift).
 */
export declare class SiegeEvent {
    readonly kind: string;
    readonly siegeId: number;
    readonly county: number;
    readonly attacker: number;
    readonly defender: number;
    readonly progress: number;
    readonly required: number;
    readonly reason: string;
    constructor(kind: string, siegeId: number, county: number, attacker?: number, defender?: number, progress?: number, required?: number, reason?: string);
}
