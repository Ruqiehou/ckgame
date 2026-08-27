export class AttributeSet {
    diplomacy;
    martial;
    stewardship;
    intrigue;
    learning;
    prowess;
    constructor(diplomacy = 8, martial = 8, stewardship = 8, intrigue = 8, learning = 8, prowess = 8) {
        this.diplomacy = diplomacy;
        this.martial = martial;
        this.stewardship = stewardship;
        this.intrigue = intrigue;
        this.learning = learning;
        this.prowess = prowess;
    }
    static zero() {
        return new AttributeSet(0, 0, 0, 0, 0, 0);
    }
    static defaults() {
        return new AttributeSet(8, 8, 8, 8, 8, 8);
    }
    add(other) {
        return new AttributeSet(this.diplomacy + other.diplomacy, this.martial + other.martial, this.stewardship + other.stewardship, this.intrigue + other.intrigue, this.learning + other.learning, this.prowess + other.prowess);
    }
    clamp(lo, hi) {
        const c = (v) => Math.max(lo, Math.min(hi, v));
        return new AttributeSet(c(this.diplomacy), c(this.martial), c(this.stewardship), c(this.intrigue), c(this.learning), c(this.prowess));
    }
    total() {
        return this.diplomacy + this.martial + this.stewardship + this.intrigue + this.learning + this.prowess;
    }
    toString() {
        return `外交${this.diplomacy} 军事${this.martial} 管理${this.stewardship} 谋略${this.intrigue} 学识${this.learning} 勇武${this.prowess}`;
    }
}
//# sourceMappingURL=AttributeSet.js.map