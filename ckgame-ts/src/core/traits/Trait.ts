import { AttributeSet } from '../stats/AttributeSet.js';
import { TraitKind } from './TraitKind.js';

export class Trait {
  constructor(
    public readonly id: number,
    public readonly name: string,
    public readonly kind: TraitKind,
    public readonly attrBonus: AttributeSet = AttributeSet.zero(),
    public readonly opinionSelf: number = 0,
    public readonly opinionOthers: number = 0,
    public readonly fertilityMod: number = 0,
    public readonly healthMod: number = 0,
    public readonly description: string = '',
  ) {}
}
