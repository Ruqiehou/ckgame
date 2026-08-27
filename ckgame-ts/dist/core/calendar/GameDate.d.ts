import { Season } from './Season.js';
export declare class GameDate implements Comparable<GameDate> {
    private _year;
    private _month;
    private _day;
    constructor(_year: number, _month: number, _day: number);
    year(): number;
    month(): number;
    day(): number;
    season(): Season;
    isLeapYear(): boolean;
    daysInMonth(): number;
    advanceOneDay(): GameDate;
    advanceDays(days: number): GameDate;
    isMonthStart(): boolean;
    isYearStart(): boolean;
    toOrdinal(): number;
    daysUntil(other: GameDate): number;
    compareTo(o: GameDate): number;
    toString(): string;
}
interface Comparable<T> {
    compareTo(o: T): number;
}
export {};
