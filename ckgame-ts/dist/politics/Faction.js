import { NONE_ID } from '../core/Constants.js';
/**
 * A faction object.
 */
export class Faction {
    id;
    kind;
    targetLiege;
    members = [];
    power = 0;
    discontent = 0;
    ultimatumSent = false;
    claimant = NONE_ID;
    constructor(id, kind, targetLiege, claimant) {
        this.id = id;
        this.kind = kind;
        this.targetLiege = targetLiege;
        if (claimant !== undefined && claimant !== null) {
            this.claimant = claimant;
        }
    }
    /** Add a member (deduplicated). */
    addMember(who) {
        if (!this.members.includes(who)) {
            this.members.push(who);
        }
    }
    /** Remove a member. */
    removeMember(who) {
        const idx = this.members.indexOf(who);
        if (idx >= 0)
            this.members.splice(idx, 1);
    }
    /** Whether the faction is ready to revolt. */
    isReadyToRevolt() {
        return this.power >= 80 && this.discontent >= 50 && this.members.length >= 2;
    }
    /** Whether the faction can send an ultimatum. */
    canSendUltimatum() {
        return this.power >= 60
            && this.discontent >= 40
            && !this.ultimatumSent
            && this.members.length >= 2;
    }
}
//# sourceMappingURL=Faction.js.map