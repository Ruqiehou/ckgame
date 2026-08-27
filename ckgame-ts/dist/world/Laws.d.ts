import { Gender } from '../core/Gender.js';
/** 继承法类型。 */
export declare enum SuccessionLaw {
    PRIMOGENITURE = "primogeniture",
    CONFEDERATE_PARTITION = "confederatePartition",
    ELECTIVE = "elective",
    HOUSE_SENIORITY = "houseSeniority",
    ULTIMOGENITURE = "ultimogeniture"
}
export declare function successionNameZh(law: SuccessionLaw): string;
/** 王权等级。 */
export declare enum CrownAuthority {
    AUTONOMOUS = "autonomous",
    LIMITED = "limited",
    HIGH = "high",
    ABSOLUTE = "absolute"
}
export declare function crownAuthorityValue(ca: CrownAuthority): number;
export declare function crownAuthorityNameZh(ca: CrownAuthority): string;
export declare function crownAuthorityVassalOpinionPenalty(ca: CrownAuthority): number;
export declare function crownAuthorityTaxBonus(ca: CrownAuthority): number;
/** 性别继承法。 */
export declare enum GenderLaw {
    AGNATIC = "agnatic",
    AGNATIC_COGNATIC = "agnaticCognatic",
    ABSOLUTE_COGNATIC = "absoluteCognatic",
    ENATIC = "enatic"
}
export declare function genderLawNameZh(law: GenderLaw): string;
export declare function genderLawAllows(genderLaw: GenderLaw, gender: Gender, hasMaleHeir: boolean): boolean;
/** 继承人候选项。 */
export interface HeirCandidate {
    id: number;
    gender: Gender;
    birthOrdinal: number;
    alive: boolean;
}
/** 分割结果行：持有人 + 分得的头衔列表。 */
export interface TitledHeir {
    heirId: number;
    titles: number[];
}
/** 领地法统：继承法、王权、性别继承规则与分割继承。 */
export declare class RealmLaw {
    readonly succession: SuccessionLaw;
    readonly crownAuthority: CrownAuthority;
    readonly genderLaw: GenderLaw;
    readonly partitionEnabled: boolean;
    constructor(succession?: SuccessionLaw, crownAuthority?: CrownAuthority, genderLaw?: GenderLaw, partitionEnabled?: boolean);
    static feudalDefault(): RealmLaw;
    /** 性别排序键：男系继承时男性优先（0），女系继承时女性优先（0）。 */
    private genderKey;
    /**
     * 根据继承法挑选继承人。
     */
    pickHeir(children: HeirCandidate[], dynastyMembers: HeirCandidate[]): HeirCandidate | null;
    /**
     * 分割继承分配：将一组头衔按轮转方式分配给若干继承人。
     */
    partitionTitles(titles: number[], heirs: number[]): TitledHeir[];
}
