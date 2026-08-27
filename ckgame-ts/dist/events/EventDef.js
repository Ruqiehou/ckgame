/**
 * Event definition.
 */
export class EventDef {
    id;
    title;
    description;
    weight;
    cooldownDays;
    major;
    choices;
    requiresRuler;
    requiresAdult;
    minGold;
    requiresMarried;
    constructor(id, title, description, weight, cooldownDays, major, choices, requiresRuler = true, requiresAdult = true, minGold = 0, requiresMarried = false) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.weight = weight;
        this.cooldownDays = cooldownDays;
        this.major = major;
        this.choices = choices;
        this.requiresRuler = requiresRuler;
        this.requiresAdult = requiresAdult;
        this.minGold = minGold;
        this.requiresMarried = requiresMarried;
    }
}
//# sourceMappingURL=EventDef.js.map