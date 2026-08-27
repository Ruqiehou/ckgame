import { FactionKind } from './FactionKind.js';
/**
 * Faction-related event.
 */
export interface FactionEvent {
    kind: string;
    factionId: number;
    liege: number;
    founder: number;
    who: number;
    factionKind?: FactionKind;
    members?: number[];
    reason?: string;
}
/**
 * Create a FactionEvent with defaults.
 */
export declare function createFactionEvent(kind: string, factionId: number, liege?: number, founder?: number, who?: number, factionKind?: FactionKind, members?: number[], reason?: string): FactionEvent;
