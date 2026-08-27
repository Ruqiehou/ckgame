import { NONE_ID } from '../core/Constants.js';

/** 王朝：家族实体，成员列表与家族威望。 */
export class Dynasty {
  id: number;
  name: string;
  head: number = NONE_ID;
  founder: number = NONE_ID;
  members: number[] = [];
  colorR: number = 128;
  colorG: number = 128;
  colorB: number = 128;
  motto: string = '';
  prestige: number = 0;

  constructor(id: number, name: string) {
    this.id = id;
    this.name = name;
  }

  static newDynasty(dynastyId: number, name: string, head: number): Dynasty {
    const d = new Dynasty(dynastyId, name);
    d.head = head;
    d.founder = head;
    return d;
  }

  addMember(who: number): void {
    if (!this.members.includes(who)) {
      this.members.push(who);
    }
  }
}
