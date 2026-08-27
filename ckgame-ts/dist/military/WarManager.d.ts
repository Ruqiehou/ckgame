import { GameDate } from '../core/calendar/GameDate.js';
import { CasusBelli } from '../politics/CasusBelli.js';
import type { Diplomacy } from '../politics/Diplomacy.js';
import type { World } from '../world/World.js';
import { Army } from './Army.js';
import { War } from './War.js';
import { WarResult } from './WarResult.js';
/**
 * War manager: manages all wars and armies.
 */
export declare class WarManager {
    wars: Map<number, War>;
    armies: Map<number, Army>;
    nextWar: number;
    nextArmy: number;
    private world;
    private diplomacy;
    constructor(world?: World | null, diplomacy?: Diplomacy | null);
    getWorld(): World | null;
    getDiplomacy(): Diplomacy | null;
    /** Declare war and create a war entry. */
    declareWar(cb: CasusBelli, attacker: number, defender: number, date: GameDate, name: string, targetTitle?: number): number;
    declareWar(cb: CasusBelli, attacker: number, defender: number, date: GameDate, name: string): number;
    /** Get a war by id. */
    war(wid: number): War | undefined;
    /** All active wars. */
    activeWars(): War[];
    /** End a war with the given result. */
    endWar(wid: number, result: WarResult): void;
    /** Raise a new army. */
    raiseArmy(owner: number, location: number, levies: number, name?: string): number;
    raiseArmy(owner: number, location: number, levies: number): number;
    /** Get an army by id. */
    army(aid: number): Army | undefined;
    /** All active army ids for a given owner. */
    armiesOf(owner: number): number[];
    /** Total men across all active armies for a given owner. */
    totalMenOf(owner: number): number;
    /** Tick movement for all moving armies. */
    tickMovement(moveChanceOf?: (army: Army) => number): void;
    /** Disband empty armies. */
    disbandEmpty(): void;
    /** Set path for an army. */
    move(armyId: number, path: number[]): void;
    /** Disband an army. */
    disband(armyId: number): void;
    /** Daily tick: movement + cleanup. */
    tick(): void;
}
