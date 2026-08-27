import { TitleTier } from '../core/TitleTier.js';
import { RealmLaw } from './Laws.js';
/** 头衔（领地）。 */
export declare class Title {
    id: number;
    name: string;
    tier: TitleTier;
    adjective: string;
    holder: number;
    deJureLiege: number;
    deFactoLiege: number;
    deJureVassals: number[];
    deFactoVassals: number[];
    capital: number;
    counties: number[];
    creationCost: number;
    destroyable: boolean;
    realmLaw: RealmLaw;
    constructor(id: number, name: string, tier: TitleTier);
    static newTitle(titleId: number, name: string, tier: TitleTier): Title;
    isHeld(): boolean;
    setHolder(holder: number): void;
    clearHolder(): void;
}
