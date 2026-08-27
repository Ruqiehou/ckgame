import { NONE_ID } from '../core/Constants.js';
import { Gender } from '../core/Gender.js';
import { AttributeSet } from '../core/stats/AttributeSet.js';
import { LifeState } from './LifeState.js';
/**
 * 角色。字段保持可变，贴近 Java dataclass 语义。
 */
export class Character {
    id;
    name;
    dynasty = NONE_ID;
    gender = Gender.MALE;
    birth;
    death = null;
    life = LifeState.ALIVE;
    baseAttrs = AttributeSet.defaults();
    traits = [];
    culture = 0;
    faith = 0;
    gold = 50;
    prestige = 50;
    piety = 50;
    stress = 0;
    health = 5;
    fertility = 0.5;
    father = NONE_ID;
    mother = NONE_ID;
    spouses = [];
    children = [];
    heldTitles = [];
    primaryTitle = NONE_ID;
    isRuler = false;
    employer = NONE_ID;
    opinionCache = new Map();
    level = 1;
    xp = 0;
    constructor(id, name, dynasty, gender, birth) {
        this.id = id;
        this.name = name;
        this.dynasty = dynasty;
        this.gender = gender;
        this.birth = birth;
    }
    ageAt(date) {
        let age = date.year() - this.birth.year();
        if (date.month() < this.birth.month() ||
            (date.month() === this.birth.month() && date.day() < this.birth.day())) {
            age -= 1;
        }
        return Math.max(0, age);
    }
    isAdult(date) {
        return this.ageAt(date) >= 16;
    }
    isAlive() {
        return this.life === LifeState.ALIVE;
    }
    isMarried() {
        return this.spouses.length > 0;
    }
    effectiveAttrs(traitBonus) {
        return this.baseAttrs.add(traitBonus);
    }
    kill(date) {
        this.life = LifeState.DEAD;
        this.death = date;
        this.isRuler = false;
    }
    addGold(amount) {
        this.gold = Math.max(0, this.gold + amount);
    }
    addPrestige(amount) {
        this.prestige = Math.max(0, this.prestige + amount);
    }
    addStress(amount) {
        this.stress = Math.max(0, Math.min(400, this.stress + amount));
    }
    /** 获得经验值，返回是否升级。 */
    gainXp(amount) {
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
    xpToNextLevel() {
        return 100 + (this.level - 1) * 50;
    }
}
//# sourceMappingURL=Character.js.map