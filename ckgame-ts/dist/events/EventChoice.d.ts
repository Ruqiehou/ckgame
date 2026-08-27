import { Effect } from './Effect.js';
/**
 * An event choice.
 */
export declare class EventChoice {
    readonly id: number;
    readonly text: string;
    readonly effects: Effect[];
    readonly aiWeight: number;
    constructor(id: number, text: string, effects: Effect[], aiWeight?: number);
}
