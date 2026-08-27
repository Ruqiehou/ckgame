import { AttributeSet } from '../stats/AttributeSet.js';
export class Trait {
    id;
    name;
    kind;
    attrBonus;
    opinionSelf;
    opinionOthers;
    fertilityMod;
    healthMod;
    description;
    constructor(id, name, kind, attrBonus = AttributeSet.zero(), opinionSelf = 0, opinionOthers = 0, fertilityMod = 0, healthMod = 0, description = '') {
        this.id = id;
        this.name = name;
        this.kind = kind;
        this.attrBonus = attrBonus;
        this.opinionSelf = opinionSelf;
        this.opinionOthers = opinionOthers;
        this.fertilityMod = fertilityMod;
        this.healthMod = healthMod;
        this.description = description;
    }
}
//# sourceMappingURL=Trait.js.map