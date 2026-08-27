import { Gender } from '../core/Gender.js';
/**
 * Political laws: succession, crown authority, gender law, and realm law.
 */
/** Succession law types. */
export declare enum SuccessionLaw {
    PRIMOGENITURE = "primogeniture",
    CONFEDERATE_PARTITION = "confederatePartition",
    ELECTIVE = "elective",
    HOUSE_SENIORITY = "houseSeniority",
    ULTIMOGENITURE = "ultimogeniture"
}
export declare function successionLawNameZh(law: SuccessionLaw): string;
/** Crown authority levels. */
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
/** Gender inheritance law. */
export declare enum GenderLaw {
    AGNATIC = "agnatic",
    AGNATIC_COGNATIC = "agnaticCognatic",
    ABSOLUTE_COGNATIC = "absoluteCognatic",
    ENATIC = "enatic"
}
export declare function genderLawNameZh(law: GenderLaw): string;
/** Whether this gender law allows the given gender to inherit. */
export declare function genderLawAllows(law: GenderLaw, gender: Gender, hasMaleHeir: boolean): boolean;
/** Heir candidate for succession resolution. */
export interface HeirCandidate {
    id: number;
    gender: Gender;
    birthOrdinal: number;
    alive: boolean;
}
/** Titled heir: heir id + list of title ids received. */
export interface TitledHeir {
    heirId: number;
    titles: number[];
}
/**
 * Realm law: succession, crown authority, gender law, and partition.
 */
export declare class RealmLaw {
    readonly succession: SuccessionLaw;
    readonly crownAuthority: CrownAuthority;
    readonly genderLaw: GenderLaw;
    readonly partitionEnabled: boolean;
    constructor(succession?: SuccessionLaw, crownAuthority?: CrownAuthority, genderLaw?: GenderLaw, partitionEnabled?: boolean);
    static feudalDefault(): RealmLaw;
    /** Gender sort key: lower = higher priority. */
    private genderKey;
    /**
     * Pick an heir from children and dynasty members.
     * Returns the heir candidate id, or null if no eligible heir.
     */
    pickHeir(children: HeirCandidate[], dynastyMembers: HeirCandidate[]): number | null;
    /**
     * Partition titles among heirs (confederate partition).
     */
    partitionTitles(titles: number[], heirs: number[]): TitledHeir[];
}
