import { EventChoice } from './EventChoice.js';
/**
 * Event definition.
 */
export declare class EventDef {
    readonly id: number;
    readonly title: string;
    readonly description: string;
    readonly weight: number;
    readonly cooldownDays: number;
    readonly major: boolean;
    readonly choices: EventChoice[];
    readonly requiresRuler: boolean;
    readonly requiresAdult: boolean;
    readonly minGold: number;
    readonly requiresMarried: boolean;
    constructor(id: number, title: string, description: string, weight: number, cooldownDays: number, major: boolean, choices: EventChoice[], requiresRuler?: boolean, requiresAdult?: boolean, minGold?: number, requiresMarried?: boolean);
}
