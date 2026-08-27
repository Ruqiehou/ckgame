import type { World } from '../world/World.js';
/**
 * Event effect: gold/prestige/piety/stress/health/trait/log.
 */
export declare class Effect {
    readonly kind: string;
    readonly amount: number;
    readonly text: string;
    constructor(kind: string, amount?: number, text?: string);
    /** Apply this effect to a character in the world. */
    apply(world: World, who: number): void;
}
