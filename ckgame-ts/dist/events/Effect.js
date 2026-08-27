/**
 * Event effect: gold/prestige/piety/stress/health/trait/log.
 */
export class Effect {
    kind;
    amount;
    text;
    constructor(kind, amount = 0, text = '') {
        this.kind = kind;
        this.amount = amount;
        this.text = text;
    }
    /** Apply this effect to a character in the world. */
    apply(world, who) {
        const c = world.characters.get(who);
        if (!c)
            return;
        switch (this.kind) {
            case 'gold':
                c.gold += this.amount;
                break;
            case 'prestige':
                c.prestige += this.amount;
                break;
            case 'piety':
                c.piety = Math.max(0, c.piety + this.amount);
                break;
            case 'stress':
                c.stress += Math.floor(this.amount);
                break;
            case 'health':
                c.health += this.amount;
                break;
            case 'trait': {
                const tid = Math.floor(this.amount);
                if (!c.traits.includes(tid)) {
                    c.traits.push(tid);
                }
                break;
            }
            case 'log':
                if (this.text && this.text.length > 0) {
                    world.log.push(this.text);
                }
                break;
            default:
                // Unknown effect type, silently ignore
                break;
        }
    }
}
//# sourceMappingURL=Effect.js.map