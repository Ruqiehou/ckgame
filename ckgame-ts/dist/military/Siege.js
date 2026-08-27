/**
 * A siege on a county.
 */
export class Siege {
    id;
    county;
    attackerArmy;
    attacker;
    defender;
    progress = 0;
    fortLevel = 1;
    garrison = 50;
    started = null;
    active = true;
    constructor(id, county, attackerArmy, attacker, defender) {
        this.id = id;
        this.county = county;
        this.attackerArmy = attackerArmy;
        this.attacker = attacker;
        this.defender = defender;
    }
    /** Required total siege progress. */
    requiredProgress() {
        return 100 + this.fortLevel * 40;
    }
    /** Whether the siege is complete. */
    isComplete() {
        return this.progress >= this.requiredProgress();
    }
    /** Daily tick: advance siege progress. Returns daily gain. */
    dailyTick(besiegerMen, martial) {
        if (!this.active)
            return 0;
        const menFactor = Math.max(0.2, Math.min(3.0, besiegerMen / Math.max(50, this.garrison)));
        const fortPenalty = 1.0 / (1.0 + this.fortLevel * 0.25);
        const martialBonus = 1.0 + (martial - 8) * 0.03;
        const gain = 1.2 * menFactor * fortPenalty * martialBonus;
        this.progress = Math.min(this.requiredProgress(), this.progress + gain);
        return gain;
    }
}
//# sourceMappingURL=Siege.js.map