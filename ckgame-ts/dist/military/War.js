import { NONE_ID } from '../core/Constants.js';
import { Balance } from '../core/balance/Balance.js';
import { cbWarscoreGoal } from '../politics/CasusBelli.js';
import { WarResult } from './WarResult.js';
/**
 * A war object.
 */
export class War {
    id;
    name;
    cb;
    attackerPrimary;
    defenderPrimary;
    start;
    participants = [];
    warscore = 0;
    active = true;
    result = WarResult.ONGOING;
    targetTitle = NONE_ID;
    constructor(id, name, cb, attackerPrimary, defenderPrimary, start) {
        this.id = id;
        this.name = name;
        this.cb = cb;
        this.attackerPrimary = attackerPrimary;
        this.defenderPrimary = defenderPrimary;
        this.start = start;
    }
    /** Whether a character is involved in this war. */
    involves(who) {
        return this.participants.some(p => p.character === who);
    }
    /** Whether a character is on the attacker side. */
    isAttacker(who) {
        return this.participants.some(p => p.character === who && p.isAttacker);
    }
    /** Apply warscore change, clamped to [-100, 100]. */
    applyWarscore(delta) {
        this.warscore = Math.max(-100, Math.min(100, this.warscore + delta));
    }
    /** Whether the attacker can enforce their demands. */
    canEnforce() {
        return this.warscore >= cbWarscoreGoal(this.cb);
    }
    /** Whether the defender can surrender. */
    canSurrender() {
        return this.warscore <= -cbWarscoreGoal(this.cb);
    }
    /** Months elapsed since war started. */
    monthsElapsed(now) {
        return Math.max(0, Math.floor((now.toOrdinal() - this.start.toOrdinal()) / 30));
    }
    /**
     * Check if white peace conditions are met.
     * @param now Current date
     * @param atkExh Attacker war exhaustion
     * @param defExh Defender war exhaustion
     */
    canWhitePeace(now, atkExh, defExh) {
        const months = this.monthsElapsed(now);
        if (months < Balance.WHITE_PEACE_MIN_MONTHS)
            return false;
        if (Math.abs(this.warscore) <= Balance.WHITE_PEACE_STALEMATE_SCORE
            && months >= Balance.WHITE_PEACE_STALEMATE_MONTHS) {
            return true;
        }
        if (Math.abs(this.warscore) <= Balance.WHITE_PEACE_FATIGUE_SCORE
            && atkExh >= Balance.WHITE_PEACE_FATIGUE_THRESHOLD
            && defExh >= Balance.WHITE_PEACE_FATIGUE_THRESHOLD) {
            return true;
        }
        if (months >= Balance.WHITE_PEACE_MAX_MONTHS
            && Math.abs(this.warscore) < Balance.WHITE_PEACE_MAX_SCORE) {
            return true;
        }
        return false;
    }
}
//# sourceMappingURL=War.js.map