/**
 * Siege event (progress, capture, or lift).
 */
export class SiegeEvent {
    kind; // 'progressed' | 'captured' | 'lifted'
    siegeId;
    county;
    attacker;
    defender;
    progress;
    required;
    reason;
    constructor(kind, siegeId, county, attacker = 0, defender = 0, progress = 0, required = 0, reason = '') {
        this.kind = kind;
        this.siegeId = siegeId;
        this.county = county;
        this.attacker = attacker;
        this.defender = defender;
        this.progress = progress;
        this.required = required;
        this.reason = reason;
    }
}
//# sourceMappingURL=SiegeEvent.js.map