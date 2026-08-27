import { Gender } from '../core/Gender.js';
/** 继承法类型。 */
export var SuccessionLaw;
(function (SuccessionLaw) {
    SuccessionLaw["PRIMOGENITURE"] = "primogeniture";
    SuccessionLaw["CONFEDERATE_PARTITION"] = "confederatePartition";
    SuccessionLaw["ELECTIVE"] = "elective";
    SuccessionLaw["HOUSE_SENIORITY"] = "houseSeniority";
    SuccessionLaw["ULTIMOGENITURE"] = "ultimogeniture";
})(SuccessionLaw || (SuccessionLaw = {}));
const SUCCESSION_NAMES = {
    [SuccessionLaw.PRIMOGENITURE]: '长子继承制',
    [SuccessionLaw.CONFEDERATE_PARTITION]: '联邦分割继承',
    [SuccessionLaw.ELECTIVE]: '选举君主制',
    [SuccessionLaw.HOUSE_SENIORITY]: '家族长老制',
    [SuccessionLaw.ULTIMOGENITURE]: '幼子继承制',
};
export function successionNameZh(law) {
    return SUCCESSION_NAMES[law] ?? law;
}
/** 王权等级。 */
export var CrownAuthority;
(function (CrownAuthority) {
    CrownAuthority["AUTONOMOUS"] = "autonomous";
    CrownAuthority["LIMITED"] = "limited";
    CrownAuthority["HIGH"] = "high";
    CrownAuthority["ABSOLUTE"] = "absolute";
})(CrownAuthority || (CrownAuthority = {}));
const CROWN_DATA = {
    [CrownAuthority.AUTONOMOUS]: { value: 0, nameZh: '自治王权', vassalOpinionPenalty: 0, taxBonus: 0.0 },
    [CrownAuthority.LIMITED]: { value: 1, nameZh: '有限王权', vassalOpinionPenalty: -5, taxBonus: 0.05 },
    [CrownAuthority.HIGH]: { value: 2, nameZh: '高度王权', vassalOpinionPenalty: -15, taxBonus: 0.15 },
    [CrownAuthority.ABSOLUTE]: { value: 3, nameZh: '绝对王权', vassalOpinionPenalty: -30, taxBonus: 0.25 },
};
export function crownAuthorityValue(ca) {
    return CROWN_DATA[ca].value;
}
export function crownAuthorityNameZh(ca) {
    return CROWN_DATA[ca].nameZh;
}
export function crownAuthorityVassalOpinionPenalty(ca) {
    return CROWN_DATA[ca].vassalOpinionPenalty;
}
export function crownAuthorityTaxBonus(ca) {
    return CROWN_DATA[ca].taxBonus;
}
/** 性别继承法。 */
export var GenderLaw;
(function (GenderLaw) {
    GenderLaw["AGNATIC"] = "agnatic";
    GenderLaw["AGNATIC_COGNATIC"] = "agnaticCognatic";
    GenderLaw["ABSOLUTE_COGNATIC"] = "absoluteCognatic";
    GenderLaw["ENATIC"] = "enatic";
})(GenderLaw || (GenderLaw = {}));
const GENDER_LAW_NAMES = {
    [GenderLaw.AGNATIC]: '男系继承',
    [GenderLaw.AGNATIC_COGNATIC]: '男系优先',
    [GenderLaw.ABSOLUTE_COGNATIC]: '绝对双系',
    [GenderLaw.ENATIC]: '女系继承',
};
export function genderLawNameZh(law) {
    return GENDER_LAW_NAMES[law] ?? law;
}
export function genderLawAllows(genderLaw, gender, hasMaleHeir) {
    switch (genderLaw) {
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
/** 领地法统：继承法、王权、性别继承规则与分割继承。 */
export class RealmLaw {
    succession;
    crownAuthority;
    genderLaw;
    partitionEnabled;
    constructor(succession = SuccessionLaw.PRIMOGENITURE, crownAuthority = CrownAuthority.LIMITED, genderLaw = GenderLaw.AGNATIC_COGNATIC, partitionEnabled = false) {
        this.succession = succession;
        this.crownAuthority = crownAuthority;
        this.genderLaw = genderLaw;
        this.partitionEnabled = partitionEnabled;
    }
    static feudalDefault() {
        return new RealmLaw();
    }
    /** 性别排序键：男系继承时男性优先（0），女系继承时女性优先（0）。 */
    genderKey(g) {
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
     * 根据继承法挑选继承人。
     */
    pickHeir(children, dynastyMembers) {
        const livingChildren = children.filter(c => c.alive);
        const hasMale = livingChildren.some(c => c.gender === Gender.MALE);
        const eligible = (c) => genderLawAllows(this.genderLaw, c.gender, hasMale);
        // 家族长老制：整个王朝中年长优先
        if (this.succession === SuccessionLaw.HOUSE_SENIORITY) {
            const pool = dynastyMembers
                .filter(c => c.alive && eligible(c))
                .sort((a, b) => a.birthOrdinal - b.birthOrdinal);
            return pool.length > 0 ? pool[0] : null;
        }
        // 幼子继承制：按性别键与倒序出生序取最幼
        if (this.succession === SuccessionLaw.ULTIMOGENITURE) {
            const pool = livingChildren
                .filter(eligible)
                .sort((a, b) => {
                const gk = this.genderKey(a.gender) - this.genderKey(b.gender);
                if (gk !== 0)
                    return gk;
                return b.birthOrdinal - a.birthOrdinal;
            });
            return pool.length > 0 ? pool[0] : null;
        }
        // 长子 / 分割 / 选举：简化为长嗣
        const pool = livingChildren
            .filter(eligible)
            .sort((a, b) => {
            const gk = this.genderKey(a.gender) - this.genderKey(b.gender);
            if (gk !== 0)
                return gk;
            return a.birthOrdinal - b.birthOrdinal;
        });
        if (pool.length > 0) {
            return pool[0];
        }
        // 无子女则取王朝成员
        const fallback = dynastyMembers
            .filter(c => c.alive && eligible(c))
            .sort((a, b) => {
            const gk = this.genderKey(a.gender) - this.genderKey(b.gender);
            if (gk !== 0)
                return gk;
            return a.birthOrdinal - b.birthOrdinal;
        });
        return fallback.length > 0 ? fallback[0] : null;
    }
    /**
     * 分割继承分配：将一组头衔按轮转方式分配给若干继承人。
     */
    partitionTitles(titles, heirs) {
        if (heirs.length === 0) {
            return [];
        }
        if (!this.partitionEnabled || this.succession !== SuccessionLaw.CONFEDERATE_PARTITION) {
            return [{ heirId: heirs[0], titles: [...titles] }];
        }
        const result = [];
        for (const h of heirs) {
            result.push({ heirId: h, titles: [] });
        }
        for (let i = 0; i < titles.length; i++) {
            result[i % heirs.length].titles.push(titles[i]);
        }
        return result;
    }
}
//# sourceMappingURL=Laws.js.map