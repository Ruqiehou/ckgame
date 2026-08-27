import { SchemeKind } from './SchemeKind.js';
/**
 * Scheme monthly tick outcome.
 */
export declare class SchemeOutcome {
    kind: string;
    schemeId: number;
    schemeKind: SchemeKind | null;
    owner: number;
    target: number;
    progress: number;
    constructor(kind: string, schemeId: number);
}
