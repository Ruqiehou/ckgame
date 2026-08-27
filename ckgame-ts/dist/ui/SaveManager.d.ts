import { GameSimulation } from '../game/GameSimulation.js';
export interface SaveInfo {
    file: string;
    name: string;
    date: string;
    playerId: number;
    mtime: number;
    size: number;
}
export interface LoadedGame {
    sim: GameSimulation;
    playerId: number;
}
export declare class SaveManager {
    static save(sim: GameSimulation, playerId: number, slotName?: string): string;
    static load(slotName: string): LoadedGame | null;
    static listSaves(): SaveInfo[];
    static deleteSave(slotName: string): boolean;
}
