import { NONE_ID } from '../core/Constants.js';
import { TitleTier, tierCreationCost, tierIsDestroyable } from '../core/TitleTier.js';
import { RealmLaw } from './Laws.js';

/** 头衔（领地）。 */
export class Title {
  id: number;
  name: string;
  tier: TitleTier;
  adjective: string = '';
  holder: number = NONE_ID;
  deJureLiege: number = NONE_ID;
  deFactoLiege: number = NONE_ID;
  deJureVassals: number[] = [];
  deFactoVassals: number[] = [];
  capital: number = NONE_ID;
  counties: number[] = [];
  creationCost: number = 0;
  destroyable: boolean = false;
  realmLaw: RealmLaw = RealmLaw.feudalDefault();

  constructor(id: number, name: string, tier: TitleTier) {
    this.id = id;
    this.name = name;
    this.tier = tier;
  }

  static newTitle(titleId: number, name: string, tier: TitleTier): Title {
    const t = new Title(titleId, name, tier);
    t.adjective = name + '的';
    t.creationCost = tierCreationCost(tier);
    t.destroyable = tierIsDestroyable(tier);
    return t;
  }

  isHeld(): boolean {
    return this.holder !== NONE_ID;
  }

  setHolder(holder: number): void {
    this.holder = holder;
  }

  clearHolder(): void {
    this.holder = NONE_ID;
  }
}
