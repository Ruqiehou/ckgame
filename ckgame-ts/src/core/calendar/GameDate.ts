import { Season, seasonZh } from './Season.js';

const DAYS_IN_MONTH = [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];

export class GameDate implements Comparable<GameDate> {
  constructor(
    private _year: number,
    private _month: number,
    private _day: number,
  ) {}

  year(): number { return this._year; }
  month(): number { return this._month; }
  day(): number { return this._day; }

  season(): Season {
    const m = this._month;
    if (m >= 3 && m <= 5) return Season.SPRING;
    if (m >= 6 && m <= 8) return Season.SUMMER;
    if (m >= 9 && m <= 11) return Season.AUTUMN;
    return Season.WINTER;
  }

  isLeapYear(): boolean {
    return (this._year % 4 === 0 && this._year % 100 !== 0) || this._year % 400 === 0;
  }

  daysInMonth(): number {
    if (this._month === 2 && this.isLeapYear()) return 29;
    return DAYS_IN_MONTH[this._month - 1] ?? 30;
  }

  advanceOneDay(): GameDate {
    let d = this._day + 1;
    let m = this._month;
    let y = this._year;
    if (d > this.daysInMonth()) {
      d = 1;
      m++;
      if (m > 12) { m = 1; y++; }
    }
    return new GameDate(y, m, d);
  }

  advanceDays(days: number): GameDate {
    let cur: GameDate = this;
    for (let i = 0; i < days; i++) cur = cur.advanceOneDay();
    return cur;
  }

  isMonthStart(): boolean { return this._day === 1; }
  isYearStart(): boolean { return this._month === 1 && this._day === 1; }

  toOrdinal(): number {
    return this._year * 10000 + this._month * 100 + this._day;
  }

  daysUntil(other: GameDate): number {
    return other.toOrdinal() - this.toOrdinal();
  }

  compareTo(o: GameDate): number {
    return this.toOrdinal() - o.toOrdinal();
  }

  toString(): string {
    return `${this._year}-${String(this._month).padStart(2, '0')}-${String(this._day).padStart(2, '0')}`;
  }
}

interface Comparable<T> {
  compareTo(o: T): number;
}
