import { GameDate } from '../core/calendar/GameDate.js';
import { TreatyKind } from './TreatyKind.js';
/**
 * A treaty between two rulers.
 */
export declare class Treaty {
    readonly a: number;
    readonly b: number;
    readonly kind: TreatyKind;
    readonly start: GameDate;
    readonly expiresYear: number;
    constructor(a: number, b: number, kind: TreatyKind, start: GameDate, expiresYear: number);
}
