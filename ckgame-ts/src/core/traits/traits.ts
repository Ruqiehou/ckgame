import { AttributeSet } from '../stats/AttributeSet.js';
import { Trait } from './Trait.js';
import { TraitKind } from './TraitKind.js';

export function builtinTraits(): Trait[] {
  return [
    new Trait(1, '勇敢', TraitKind.PERSONALITY, new AttributeSet(0, 3, 0, -1, 0, 2), 5, 5, 0, 0.1, '天生勇敢果断'),
    new Trait(2, '怯懦', TraitKind.PERSONALITY, new AttributeSet(0, -2, 0, 1, 0, -1), -5, -5, 0, -0.05, '胆小怕事'),
    new Trait(3, '睿智', TraitKind.PERSONALITY, new AttributeSet(2, 0, 1, 0, 3, 0), 5, 5, 0, 0.05, '聪慧过人'),
    new Trait(4, '愚钝', TraitKind.PERSONALITY, new AttributeSet(-1, 0, -1, 0, -2, 0), -5, -5, 0, -0.05, '反应迟钝'),
    new Trait(5, '魅力非凡', TraitKind.PERSONALITY, new AttributeSet(3, 0, 0, 1, 0, 0), 10, 10, 0.1, 0, '极具人格魅力'),
    new Trait(6, '阴沉', TraitKind.PERSONALITY, new AttributeSet(-1, 0, 0, 2, 1, 0), -5, -10, 0, -0.05, '性格阴郁'),
    new Trait(7, '勤勉', TraitKind.PERSONALITY, new AttributeSet(0, 0, 3, 0, 1, 0), 5, 5, 0, 0.1, '勤勤恳恳'),
    new Trait(8, '懒惰', TraitKind.PERSONALITY, new AttributeSet(0, 0, -2, 0, 0, 0), -5, -5, 0, -0.1, '好吃懒做'),
    new Trait(9, '慷慨', TraitKind.PERSONALITY, new AttributeSet(2, 0, 0, 0, 0, 0), 10, 5, 0, 0, '出手阔绰'),
    new Trait(10, '吝啬', TraitKind.PERSONALITY, new AttributeSet(-1, 0, 1, 0, 0, 0), -5, -10, 0, 0, '爱财如命'),
    new Trait(11, '强壮', TraitKind.HEALTH, new AttributeSet(0, 1, 0, 0, 0, 3), 0, 5, 0.05, 0.2, '体魄强健'),
    new Trait(12, '体弱', TraitKind.HEALTH, new AttributeSet(0, -1, 0, 0, 0, -2), 0, -5, -0.05, -0.2, '体质虚弱'),
  ];
}
