import { NONE_ID } from '../core/Constants.js';
import { tierCreationCost, tierIsDestroyable } from '../core/TitleTier.js';
import { RealmLaw } from './Laws.js';
/** 头衔（领地）。 */
export class Title {
    id;
    name;
    tier;
    adjective = '';
    holder = NONE_ID;
    deJureLiege = NONE_ID;
    deFactoLiege = NONE_ID;
    deJureVassals = [];
    deFactoVassals = [];
    capital = NONE_ID;
    counties = [];
    creationCost = 0;
    destroyable = false;
    realmLaw = RealmLaw.feudalDefault();
    constructor(id, name, tier) {
        this.id = id;
        this.name = name;
        this.tier = tier;
    }
    static newTitle(titleId, name, tier) {
        const t = new Title(titleId, name, tier);
        t.adjective = name + '的';
        t.creationCost = tierCreationCost(tier);
        t.destroyable = tierIsDestroyable(tier);
        return t;
    }
    isHeld() {
        return this.holder !== NONE_ID;
    }
    setHolder(holder) {
        this.holder = holder;
    }
    clearHolder() {
        this.holder = NONE_ID;
    }
}
//# sourceMappingURL=Title.js.map