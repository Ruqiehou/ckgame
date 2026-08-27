/**
 * An event choice.
 */
export class EventChoice {
    id;
    text;
    effects;
    aiWeight;
    constructor(id, text, effects, aiWeight = 5) {
        this.id = id;
        this.text = text;
        this.effects = effects;
        this.aiWeight = aiWeight;
    }
}
//# sourceMappingURL=EventChoice.js.map