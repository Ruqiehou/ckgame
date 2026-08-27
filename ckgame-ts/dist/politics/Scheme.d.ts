import { GameDate } from '../core/calendar/GameDate.js';
import { SchemeKind } from './SchemeKind.js';
/**
 * A scheme (conspiracy) object.
 */
export declare class Scheme {
    readonly id: number;
    readonly kind: SchemeKind;
    readonly owner: number;
    readonly target: number;
    progress: number;
    secrecy: number;
    agents: number[];
    started: GameDate | null;
    exposed: boolean;
    constructor(id: number, kind: SchemeKind, owner: number, target: number);
    /** Whether the scheme has completed. */
    isComplete(): boolean;
}
