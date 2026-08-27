/** 王朝：家族实体，成员列表与家族威望。 */
export declare class Dynasty {
    id: number;
    name: string;
    head: number;
    founder: number;
    members: number[];
    colorR: number;
    colorG: number;
    colorB: number;
    motto: string;
    prestige: number;
    constructor(id: number, name: string);
    static newDynasty(dynastyId: number, name: string, head: number): Dynasty;
    addMember(who: number): void;
}
