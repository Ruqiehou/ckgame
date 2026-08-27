import { AttributeSet } from '../stats/AttributeSet.js';
import { TraitKind } from './TraitKind.js';
export declare class Trait {
    readonly id: number;
    readonly name: string;
    readonly kind: TraitKind;
    readonly attrBonus: AttributeSet;
    readonly opinionSelf: number;
    readonly opinionOthers: number;
    readonly fertilityMod: number;
    readonly healthMod: number;
    readonly description: string;
    constructor(id: number, name: string, kind: TraitKind, attrBonus?: AttributeSet, opinionSelf?: number, opinionOthers?: number, fertilityMod?: number, healthMod?: number, description?: string);
}
