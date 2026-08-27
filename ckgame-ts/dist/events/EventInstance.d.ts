import { EventChoice } from './EventChoice.js';
/**
 * A pending event instance for a specific character.
 */
export declare class EventInstance {
    readonly eventId: number;
    readonly character: number;
    readonly title: string;
    readonly description: string;
    readonly choices: EventChoice[];
    constructor(eventId: number, character: number, title: string, description: string, choices: EventChoice[]);
}
