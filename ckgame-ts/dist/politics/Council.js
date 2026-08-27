import { NONE_ID } from '../core/Constants.js';
import { AttributeSet } from '../core/stats/AttributeSet.js';
import { CouncilPosition, allCouncilPositions } from './CouncilPosition.js';
import { CouncilTask } from './CouncilTask.js';
/**
 * Monthly council output result.
 */
export class CouncilMonthlyResult {
    gold = 0;
    prestige = 0;
    piety = 0;
    controlGain = 0;
    developmentChance = 0;
    claimProgress = 0;
    logs = [];
}
/**
 * A single ruler's council.
 */
export class Council {
    ruler;
    chancellor = NONE_ID;
    marshal = NONE_ID;
    steward = NONE_ID;
    spymaster = NONE_ID;
    chaplain = NONE_ID;
    tasks = new Map();
    constructor(ruler) {
        this.ruler = ruler;
    }
    /** Create an empty council with default tasks. */
    static empty(ruler) {
        const c = new Council(ruler);
        c.tasks.set(CouncilPosition.CHANCELLOR, CouncilTask.DOMESTIC_RELATIONS);
        c.tasks.set(CouncilPosition.MARSHAL, CouncilTask.TRAIN_COMMANDERS);
        c.tasks.set(CouncilPosition.STEWARD, CouncilTask.COLLECT_TAXES);
        c.tasks.set(CouncilPosition.SPYMASTER, CouncilTask.DISRUPT_SCHEMES);
        c.tasks.set(CouncilPosition.COURT_CHAPLAIN, CouncilTask.CONVERT_FAITH);
        return c;
    }
    /** Get the current holder of a position. */
    get(pos) {
        switch (pos) {
            case CouncilPosition.CHANCELLOR: return this.chancellor;
            case CouncilPosition.MARSHAL: return this.marshal;
            case CouncilPosition.STEWARD: return this.steward;
            case CouncilPosition.SPYMASTER: return this.spymaster;
            case CouncilPosition.COURT_CHAPLAIN: return this.chaplain;
        }
    }
    /** Appoint someone to a position. */
    set(pos, who) {
        switch (pos) {
            case CouncilPosition.CHANCELLOR:
                this.chancellor = who;
                break;
            case CouncilPosition.MARSHAL:
                this.marshal = who;
                break;
            case CouncilPosition.STEWARD:
                this.steward = who;
                break;
            case CouncilPosition.SPYMASTER:
                this.spymaster = who;
                break;
            case CouncilPosition.COURT_CHAPLAIN:
                this.chaplain = who;
                break;
        }
    }
    /** Return all appointed positions with their holder ids. */
    members() {
        const out = [];
        for (const p of allCouncilPositions()) {
            const who = this.get(p);
            if (who !== NONE_ID) {
                out.push([p, who]);
            }
        }
        return out;
    }
    /** Get the current task for a position. */
    taskOf(pos) {
        return this.tasks.get(pos) ?? CouncilTask.DOMESTIC_RELATIONS;
    }
    /**
     * Auto-appoint council members from candidates.
     * Each candidate is: [id, diplomacy, martial, stewardship, intrigue, learning].
     */
    autoAppoint(candidates) {
        const used = new Set();
        if (this.ruler !== NONE_ID)
            used.add(this.ruler);
        const pick = (scoreIdx) => {
            let best = NONE_ID;
            let bestS = -999;
            for (const row of candidates) {
                if (used.has(row[0]))
                    continue;
                if (row[scoreIdx] > bestS) {
                    bestS = row[scoreIdx];
                    best = row[0];
                }
            }
            if (best !== NONE_ID)
                used.add(best);
            return best;
        };
        this.chancellor = pick(1);
        this.marshal = pick(2);
        this.steward = pick(3);
        this.spymaster = pick(4);
        this.chaplain = pick(5);
    }
    /**
     * Compute monthly council effects.
     * @param skillOf Map from character id to their AttributeSet; defaults to AttributeSet.defaults()
     */
    monthlyEffect(skillOf) {
        const r = new CouncilMonthlyResult();
        const defaultAttrs = AttributeSet.defaults();
        for (const [pos, who] of this.members()) {
            const skills = skillOf.get(who) ?? defaultAttrs;
            const task = this.taskOf(pos);
            switch (task) {
                case CouncilTask.DOMESTIC_RELATIONS:
                    r.prestige += skills.diplomacy * 0.3;
                    break;
                case CouncilTask.FABRICATE_CLAIM:
                    r.claimProgress += skills.diplomacy * 0.8;
                    break;
                case CouncilTask.TRAIN_COMMANDERS:
                    r.prestige += skills.martial * 0.15;
                    r.controlGain += skills.martial * 0.1;
                    break;
                case CouncilTask.INCREASE_CONTROL:
                    r.controlGain += skills.martial * 0.4;
                    break;
                case CouncilTask.COLLECT_TAXES:
                    r.gold += skills.stewardship * 0.6;
                    break;
                case CouncilTask.DEVELOP_COUNTY:
                    r.developmentChance += skills.stewardship * 0.5;
                    break;
                case CouncilTask.DISRUPT_SCHEMES:
                    r.prestige += skills.intrigue * 0.1;
                    r.logs.push('间谍总管破坏敌对阴谋');
                    break;
                case CouncilTask.SUPPORT_MURDER:
                    r.claimProgress += skills.intrigue * 0.3;
                    break;
                case CouncilTask.CONVERT_FAITH:
                    r.piety += skills.learning * 0.5;
                    break;
                case CouncilTask.FABRICATE_HOOK:
                    r.piety += skills.learning * 0.2;
                    r.claimProgress += skills.learning * 0.25;
                    break;
                case CouncilTask.RECRUIT_KNIGHTS:
                    r.gold -= 15;
                    r.prestige += 5;
                    r.logs.push('骑士招募中');
                    break;
                case CouncilTask.IMPROVE_DIPLOMACY:
                    r.prestige += skills.diplomacy * 0.4;
                    r.logs.push('外交关系改善');
                    break;
                case CouncilTask.SPREAD_CULTURE:
                    r.controlGain += skills.learning * 0.3;
                    r.piety += skills.learning * 0.2;
                    r.logs.push('文化传播中');
                    break;
                case CouncilTask.ESTABLISH_TRADE:
                    r.gold += skills.diplomacy * 0.5;
                    r.prestige += skills.stewardship * 0.2;
                    r.logs.push('商路建立中');
                    break;
                case CouncilTask.MAINTAIN_BUILDINGS:
                    r.gold -= 5;
                    r.controlGain += skills.stewardship * 0.2;
                    r.logs.push('建筑维护中');
                    break;
                case CouncilTask.TRAIN_TROOPS:
                    r.gold -= 10;
                    r.controlGain += skills.martial * 0.3;
                    r.logs.push('部队训练中');
                    break;
                case CouncilTask.GATHER_INTEL:
                    r.prestige += skills.intrigue * 0.3;
                    r.claimProgress += skills.intrigue * 0.2;
                    r.logs.push('情报收集中');
                    break;
                case CouncilTask.PROMOTE_CULTURE:
                    r.controlGain += skills.learning * 0.4;
                    r.piety += skills.learning * 0.1;
                    r.logs.push('文化推广中');
                    break;
                default: {
                    // Default output based on position
                    switch (pos) {
                        case CouncilPosition.CHANCELLOR:
                            r.prestige += skills.diplomacy * 0.15;
                            break;
                        case CouncilPosition.MARSHAL:
                            r.controlGain += skills.martial * 0.1;
                            break;
                        case CouncilPosition.STEWARD:
                            r.gold += skills.stewardship * 0.2;
                            break;
                        case CouncilPosition.SPYMASTER:
                            r.prestige += skills.intrigue * 0.1;
                            break;
                        case CouncilPosition.COURT_CHAPLAIN:
                            r.piety += skills.learning * 0.2;
                            break;
                    }
                    break;
                }
            }
        }
        return r;
    }
}
//# sourceMappingURL=Council.js.map