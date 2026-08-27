import { NONE_ID } from '../core/Constants.js';
/** 王朝：家族实体，成员列表与家族威望。 */
export class Dynasty {
    id;
    name;
    head = NONE_ID;
    founder = NONE_ID;
    members = [];
    colorR = 128;
    colorG = 128;
    colorB = 128;
    motto = '';
    prestige = 0;
    constructor(id, name) {
        this.id = id;
        this.name = name;
    }
    static newDynasty(dynastyId, name, head) {
        const d = new Dynasty(dynastyId, name);
        d.head = head;
        d.founder = head;
        return d;
    }
    addMember(who) {
        if (!this.members.includes(who)) {
            this.members.push(who);
        }
    }
}
//# sourceMappingURL=Dynasty.js.map