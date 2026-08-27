/**
 * A pending event instance for a specific character.
 */
export class EventInstance {
    eventId;
    character;
    title;
    description;
    choices;
    constructor(eventId, character, title, description, choices) {
        this.eventId = eventId;
        this.character = character;
        this.title = title;
        this.description = description;
        this.choices = choices;
    }
}
//# sourceMappingURL=EventInstance.js.map