import { NONE_ID } from '../core/Constants.js';
import { Gender } from '../core/Gender.js';
import { GameDate } from '../core/calendar/GameDate.js';
import { AttributeSet } from '../core/stats/AttributeSet.js';
import { LifeState } from './LifeState.js';

/**
 * 角色。字段保持可变，贴近 Java dataclass 语义。
 */
export class Character {
  id: number;
  name: string;
  dynasty: number = NONE_ID;
  gender: Gender = Gender.MALE;
  birth: GameDate;
  death: GameDate | null = null;
  life: LifeState = LifeState.ALIVE;
  baseAttrs: AttributeSet = AttributeSet.defaults();
  traits: number[] = [];
  culture: number = 0;
  faith: number = 0;
  gold: number = 50;
  prestige: number = 50;
  piety: number = 50;
  stress: number = 0;
  health: number = 5;
  fertility: number = 0.5;
  father: number = NONE_ID;
  mother: number = NONE_ID;
  spouses: number[] = [];
  children: number[] = [];
  heldTitles: number[] = [];
  primaryTitle: number = NONE_ID;
  isRuler: boolean = false;
  employer: number = NONE_ID;
  opinionCache: Map<number, number> = new Map();
  level: number = 1;
  xp: number = 0;

  constructor(id: number, name: string, dynasty: number, gender: Gender, birth: GameDate) {
    this.id = id;
    this.name = name;
    this.dynasty = dynasty;
    this.gender = gender;
    this.birth = birth;
  }

  ageAt(date: GameDate): number {
    let age = date.year() - this.birth.year();
    if (date.month() < this.birth.month() ||
        (date.month() === this.birth.month() && date.day() < this.birth.day())) {
      age -= 1;
    }
    return Math.max(0, age);
  }

  isAdult(date: GameDate): boolean {
    return this.ageAt(date) >= 16;
  }

  isAlive(): boolean {
    return this.life === LifeState.ALIVE;
  }

  isMarried(): boolean {
    return this.spouses.length > 0;
  }

  effectiveAttrs(traitBonus: AttributeSet): AttributeSet {
    return this.baseAttrs.add(traitBonus);
  }

  kill(date: GameDate): void {
    this.life = LifeState.DEAD;
    this.death = date;
    this.isRuler = false;
  }

  addGold(amount: number): void {
    this.gold = Math.max(0, this.gold + amount);
  }

  addPrestige(amount: number): void {
    this.prestige = Math.max(0, this.prestige + amount);
  }

  addStress(amount: number): void {
    this.stress = Math.max(0, Math.min(400, this.stress + amount));
  }

  /** 获得经验值，返回是否升级。 */
  gainXp(amount: number): boolean {
    this.xp += amount;
    let leveled = false;
    while (this.xp >= this.xpToNextLevel()) {
      this.xp -= this.xpToNextLevel();
      this.level += 1;
      leveled = true;
    }
    return leveled;
  }

  /** 升级所需经验值，随等级递增。 */
  xpToNextLevel(): number {
    return 100 + (this.level - 1) * 50;
  }
}
