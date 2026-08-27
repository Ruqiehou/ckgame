import { Gender } from '../core/Gender.js';
import { GameDate } from '../core/calendar/GameDate.js';
import { AttributeSet } from '../core/stats/AttributeSet.js';
import { LifeState } from './LifeState.js';
/**
 * 角色。字段保持可变，贴近 Java dataclass 语义。
 */
export declare class Character {
    id: number;
    name: string;
    dynasty: number;
    gender: Gender;
    birth: GameDate;
    death: GameDate | null;
    life: LifeState;
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
    constructor(id: number, name: string, dynasty: number, gender: Gender, birth: GameDate);
    ageAt(date: GameDate): number;
    isAdult(date: GameDate): boolean;
    isAlive(): boolean;
    isMarried(): boolean;
    effectiveAttrs(traitBonus: AttributeSet): AttributeSet;
    kill(date: GameDate): void;
    addGold(amount: number): void;
    addPrestige(amount: number): void;
    addStress(amount: number): void;
    /** 获得经验值，返回是否升级。 */
    gainXp(amount: number): boolean;
    /** 升级所需经验值，随等级递增。 */
    xpToNextLevel(): number;
}
