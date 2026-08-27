/**
 * A participant in a war.
 */
export class WarParticipant {
    character;
    isAttacker;
    joined;
    contribution = 0;
    constructor(character, isAttacker, joined) {
        this.character = character;
        this.isAttacker = isAttacker;
        this.joined = joined;
    }
}
//# sourceMappingURL=WarParticipant.js.map