import type { World } from '../world/World.js';
import { EventDef } from './EventDef.js';
import { EventInstance } from './EventInstance.js';
/**
 * Event engine: manages event catalog, cooldowns, pending queue, and history.
 */
export declare class EventEngine {
    catalog: EventDef[];
    pending: EventInstance[];
    history: string[];
    private cooldowns;
    private rng;
    constructor(catalog?: EventDef[], rng?: () => number);
    private cooldownKey;
    /** Decrement all cooldowns by one day; remove expired entries. */
    tickCooldowns(): void;
    private eligible;
    /** Daily check: may generate events for each character. */
    dailyCheck(world: World, characters: number[]): void;
    /** Resolve a choice for a pending event. */
    resolveChoice(world: World, instance: EventInstance, choiceId: number): void;
    /** AI auto-resolves all pending events by highest weight. */
    autoResolveAll(world: World): void;
    /** All 30 builtin event definitions. */
    static builtinEvents(): EventDef[];
}
