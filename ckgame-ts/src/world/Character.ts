import { Gender } from '../core/Gender';
import { TitleTier } from '../core/TitleTier';
import { GameDate } from '../core/calendar/GameDate';
import { RealmLaw, SuccessionLaw, CrownAuthority, GenderLaw } from './entities';

export interface Character {
  id: number;
  name: string;
  dynasty: number;
  gender: Gender;
  birth: GameDate;
  death?: GameDate;
  life: 'ALIVE' | 'DEAD';
  baseAttrs: AttributeSet;
  traits: number[];
  culture: number;
  faith: number;
  gold: number;
  prestige: number;
  piety: number;
  stress: number;
  health: number;
  fertility: number;
  father: number;
  mother: number;
  spouses: number[];
  children: number[];
  heldTitles: number[];
  primaryTitle: number;
  isRuler: boolean;
  employer: number;
  opinionCache: Map<number, number>;
  level: number;
  xp: number;
}

export interface Dynasty {
  id: number;
  name: string;
  head: number;
  founder: number;
  members: number[];
  colorR: number;
  colorG: number;
  colorB: number;
  motto: string;
  prestige: number;
}

export interface AttributeSet {
  diplomacy: number;
  martial: number;
  stewardship: number;
  intrigue: number;
  learning: number;
  prowess: number;

  add(other: AttributeSet): AttributeSet;
  zero(): AttributeSet;
}

export interface Trait {
  id: number;
  name: string;
  description: string;
  attrBonus: () => AttributeSet;
  opinionSelf: () => number;
  opinionOthers: () => number;
  healthMod: () => number;
}

export class AttributeSet implements AttributeSet {
  constructor(
    public diplomacy = 0,
    public martial = 0,
    public stewardship = 0,
    public intrigue = 0,
    public learning = 0,
    public prowess = 0
  ) {}

  add(other: AttributeSet): AttributeSet {
    return new AttributeSet(
      this.diplomacy + other.diplomacy,
      this.martial + other.martial,
      this.stewardship + other.stewardship,
      this.intrigue + other.intrigue,
      this.learning + other.learning,
      this.prowess + other.prowess
    );
  }

  static zero(): AttributeSet {
    return new AttributeSet(0, 0, 0, 0, 0, 0);
  }
}

export class Trait implements Trait {
  constructor(
    public id: number,
    public name: string,
    public description: string,
    public attrBonus: () => AttributeSet,
    public opinionSelf: () => number,
    public opinionOthers: () => number,
    public healthMod: () => number
  ) {}
}
