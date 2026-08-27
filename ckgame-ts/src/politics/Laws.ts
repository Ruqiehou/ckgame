import { Gender } from '../core/Gender.js';

/**
 * Political laws: succession, crown authority, gender law, and realm law.
 */

/** Succession law types. */
export enum SuccessionLaw {
  PRIMOGENITURE = 'primogeniture',
  CONFEDERATE_PARTITION = 'confederatePartition',
  ELECTIVE = 'elective',
  HOUSE_SENIORITY = 'houseSeniority',
  ULTIMOGENITURE = 'ultimogeniture',
}

const successionLawNames: Record<SuccessionLaw, string> = {
  [SuccessionLaw.PRIMOGENITURE]: '长子继承制',
  [SuccessionLaw.CONFEDERATE_PARTITION]: '联邦分割继承',
  [SuccessionLaw.ELECTIVE]: '选举君主制',
  [SuccessionLaw.HOUSE_SENIORITY]: '家族长老制',
  [SuccessionLaw.ULTIMOGENITURE]: '幼子继承制',
};

export function successionLawNameZh(law: SuccessionLaw): string {
  return successionLawNames[law] ?? law;
}

/** Crown authority levels. */
export enum CrownAuthority {
  AUTONOMOUS = 'autonomous',
  LIMITED = 'limited',
  HIGH = 'high',
  ABSOLUTE = 'absolute',
}

interface CrownAuthorityData {
  value: number;
  nameZh: string;
  vassalOpinionPenalty: number;
  taxBonus: number;
}

const crownAuthorityData: Record<CrownAuthority, CrownAuthorityData> = {
  [CrownAuthority.AUTONOMOUS]: { value: 0, nameZh: '自治王权', vassalOpinionPenalty: 0, taxBonus: 0.0 },
  [CrownAuthority.LIMITED]: { value: 1, nameZh: '有限王权', vassalOpinionPenalty: -5, taxBonus: 0.05 },
  [CrownAuthority.HIGH]: { value: 2, nameZh: '高度王权', vassalOpinionPenalty: -15, taxBonus: 0.15 },
  [CrownAuthority.ABSOLUTE]: { value: 3, nameZh: '绝对王权', vassalOpinionPenalty: -30, taxBonus: 0.25 },
};

export function crownAuthorityValue(ca: CrownAuthority): number {
  return crownAuthorityData[ca].value;
}

export function crownAuthorityNameZh(ca: CrownAuthority): string {
  return crownAuthorityData[ca].nameZh;
}

export function crownAuthorityVassalOpinionPenalty(ca: CrownAuthority): number {
  return crownAuthorityData[ca].vassalOpinionPenalty;
}

export function crownAuthorityTaxBonus(ca: CrownAuthority): number {
  return crownAuthorityData[ca].taxBonus;
}

/** Gender inheritance law. */
export enum GenderLaw {
  AGNATIC = 'agnatic',
  AGNATIC_COGNATIC = 'agnaticCognatic',
  ABSOLUTE_COGNATIC = 'absoluteCognatic',
  ENATIC = 'enatic',
}

const genderLawNames: Record<GenderLaw, string> = {
  [GenderLaw.AGNATIC]: '男系继承',
  [GenderLaw.AGNATIC_COGNATIC]: '男系优先',
  [GenderLaw.ABSOLUTE_COGNATIC]: '绝对双系',
  [GenderLaw.ENATIC]: '女系继承',
};

export function genderLawNameZh(law: GenderLaw): string {
  return genderLawNames[law] ?? law;
}

/** Whether this gender law allows the given gender to inherit. */
export function genderLawAllows(law: GenderLaw, gender: Gender, hasMaleHeir: boolean): boolean {
  switch (law) {
    case GenderLaw.AGNATIC:
      return gender === Gender.MALE;
    case GenderLaw.AGNATIC_COGNATIC:
      return gender === Gender.MALE || !hasMaleHeir;
    case GenderLaw.ENATIC:
      return gender === Gender.FEMALE;
    case GenderLaw.ABSOLUTE_COGNATIC:
      return true;
  }
}

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
export class RealmLaw {
  constructor(
    public readonly succession: SuccessionLaw = SuccessionLaw.PRIMOGENITURE,
    public readonly crownAuthority: CrownAuthority = CrownAuthority.LIMITED,
    public readonly genderLaw: GenderLaw = GenderLaw.AGNATIC_COGNATIC,
    public readonly partitionEnabled: boolean = false,
  ) {}

  static feudalDefault(): RealmLaw {
    return new RealmLaw();
  }

  /** Gender sort key: lower = higher priority. */
  private genderKey(g: Gender): number {
    switch (this.genderLaw) {
      case GenderLaw.AGNATIC:
      case GenderLaw.AGNATIC_COGNATIC:
        return g === Gender.MALE ? 0 : 1;
      case GenderLaw.ENATIC:
        return g === Gender.FEMALE ? 0 : 1;
      case GenderLaw.ABSOLUTE_COGNATIC:
        return 0;
    }
  }

  /**
   * Pick an heir from children and dynasty members.
   * Returns the heir candidate id, or null if no eligible heir.
   */
  pickHeir(children: HeirCandidate[], dynastyMembers: HeirCandidate[]): number | null {
    const livingChildren = children.filter(c => c.alive);
    const hasMale = livingChildren.some(c => c.gender === Gender.MALE);

    const eligible = (c: HeirCandidate): boolean =>
      genderLawAllows(this.genderLaw, c.gender, hasMale);

    // House seniority: oldest dynasty member first
    if (this.succession === SuccessionLaw.HOUSE_SENIORITY) {
      const pool = dynastyMembers
        .filter(c => c.alive && eligible(c))
        .sort((a, b) => a.birthOrdinal - b.birthOrdinal);
      return pool.length > 0 ? pool[0].id : null;
    }

    // Ultimogeniture: youngest child first
    if (this.succession === SuccessionLaw.ULTIMOGENITURE) {
      const pool = livingChildren
        .filter(eligible)
        .sort((a, b) => {
          const gk = this.genderKey(a.gender) - this.genderKey(b.gender);
          if (gk !== 0) return gk;
          return b.birthOrdinal - a.birthOrdinal; // reversed for youngest
        });
      return pool.length > 0 ? pool[0].id : null;
    }

    // Primogeniture / partition / elective: eldest first
    const pool = livingChildren
      .filter(eligible)
      .sort((a, b) => {
        const gk = this.genderKey(a.gender) - this.genderKey(b.gender);
        if (gk !== 0) return gk;
        return a.birthOrdinal - b.birthOrdinal;
      });
    if (pool.length > 0) return pool[0].id;

    // Fallback to dynasty members
    const fallback = dynastyMembers
      .filter(c => c.alive && eligible(c))
      .sort((a, b) => {
        const gk = this.genderKey(a.gender) - this.genderKey(b.gender);
        if (gk !== 0) return gk;
        return a.birthOrdinal - b.birthOrdinal;
      });
    return fallback.length > 0 ? fallback[0].id : null;
  }

  /**
   * Partition titles among heirs (confederate partition).
   */
  partitionTitles(titles: number[], heirs: number[]): TitledHeir[] {
    if (heirs.length === 0) return [];
    if (!this.partitionEnabled || this.succession !== SuccessionLaw.CONFEDERATE_PARTITION) {
      return [{ heirId: heirs[0], titles: [...titles] }];
    }
    const result: TitledHeir[] = heirs.map(h => ({ heirId: h, titles: [] }));
    for (let i = 0; i < titles.length; i++) {
      result[i % heirs.length].titles.push(titles[i]);
    }
    return result;
  }
}
