export class AttributeSet {
  constructor(
    public readonly diplomacy: number = 8,
    public readonly martial: number = 8,
    public readonly stewardship: number = 8,
    public readonly intrigue: number = 8,
    public readonly learning: number = 8,
    public readonly prowess: number = 8,
  ) {}

  static zero(): AttributeSet {
    return new AttributeSet(0, 0, 0, 0, 0, 0);
  }

  static defaults(): AttributeSet {
    return new AttributeSet(8, 8, 8, 8, 8, 8);
  }

  add(other: AttributeSet): AttributeSet {
    return new AttributeSet(
      this.diplomacy + other.diplomacy,
      this.martial + other.martial,
      this.stewardship + other.stewardship,
      this.intrigue + other.intrigue,
      this.learning + other.learning,
      this.prowess + other.prowess,
    );
  }

  clamp(lo: number, hi: number): AttributeSet {
    const c = (v: number) => Math.max(lo, Math.min(hi, v));
    return new AttributeSet(c(this.diplomacy), c(this.martial), c(this.stewardship),
      c(this.intrigue), c(this.learning), c(this.prowess));
  }

  total(): number {
    return this.diplomacy + this.martial + this.stewardship + this.intrigue + this.learning + this.prowess;
  }

  toString(): string {
    return `外交${this.diplomacy} 军事${this.martial} 管理${this.stewardship} 谋略${this.intrigue} 学识${this.learning} 勇武${this.prowess}`;
  }
}
