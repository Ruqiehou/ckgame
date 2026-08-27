import { Gender } from '../core/Gender.js';
import { TitleTier } from '../core/TitleTier.js';
import { GameDate } from '../core/calendar/GameDate.js';
import { AttributeSet } from '../core/stats/AttributeSet.js';
import { Trait } from '../core/traits/Trait.js';
import { Character } from './Character.js';
import { Dynasty } from './Dynasty.js';
import { RealmLaw } from './Laws.js';
import { MapGraph } from './MapGraph.js';
import { Title } from './Title.js';
import { Terrain } from './Terrain.js';
import { TradeRoute } from './TradeRoute.js';
/** 贸易事件记录。 */
export interface TradeEvent {
    date: string;
    route: TradeRoute;
    type: string;
    description: string;
}
/** 世界状态：所有角色、王朝、头衔、地图与贸易的经济逻辑。 */
export declare class World {
    date: GameDate;
    tick: number;
    characters: Map<number, Character>;
    dynasties: Map<number, Dynasty>;
    titles: Map<number, Title>;
    map: MapGraph;
    traits: Trait[];
    log: string[];
    nextChar: number;
    nextDynasty: number;
    nextTitle: number;
    nextCounty: number;
    tradeRoutes: TradeRoute[];
    exchangeRates: Map<string, number>;
    tradeEvents: TradeEvent[];
    private rngState;
    constructor(dateOrNull?: GameDate | null, seed?: number);
    /** Simple mulberry32 PRNG for deterministic results. */
    private rng;
    nextDouble(): number;
    nextInt(bound: number): number;
    nextInt(origin: number, bound: number): number;
    pushLog(msg: string): void;
    createDynasty(name: string): number;
    createCharacter(name: string, dynasty: number, gender: Gender, birth: GameDate): number;
    createTitle(name: string, tier: TitleTier): number;
    createCounty(name: string, terrain: Terrain): number;
    character(cid: number): Character | undefined;
    title(tid: number): Title | undefined;
    aliveCharacters(): Character[];
    rulers(): Character[];
    traitBonus(traitIds: number[]): AttributeSet;
    effectiveAttrs(cid: number): AttributeSet | null;
    setParents(child: number, father: number, mother: number): void;
    marry(a: number, b: number): boolean;
    grantTitle(titleId: number, holder: number): boolean;
    setVassal(vassalTitle: number, liegeTitle: number): boolean;
    clearVassalLink(titleId: number): void;
    attachCountyToTitle(countyId: number, titleId: number): boolean;
    private traitById;
    opinion(from: number, to: number): number;
    modifyOpinion(from: number, to: number, delta: number): void;
    private heirRows;
    findHeir(ruler: number, law: RealmLaw | null): number | null;
    onDeath(who: number, law: RealmLaw | null): void;
    occupyCounty(countyId: number, newHolder: number): boolean;
    loadTradeRoutes(routes: TradeRoute[]): void;
    calculateTradeIncome(ruler: number): number;
    monthlyIncomeOf(ruler: number): number;
    processMonthlyEconomy(): void;
    private triggerTradeEvent;
    private adjustExchangeRate;
    updateExchangeRates(): void;
    processHealth(): void;
    processFertility(): void;
}
