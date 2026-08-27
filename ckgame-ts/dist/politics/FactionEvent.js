/**
 * Create a FactionEvent with defaults.
 */
export function createFactionEvent(kind, factionId, liege = 0, founder = 0, who = 0, factionKind, members, reason) {
    return {
        kind,
        factionId,
        liege,
        founder,
        who,
        factionKind,
        members: members ? [...members] : undefined,
        reason: reason ?? '',
    };
}
//# sourceMappingURL=FactionEvent.js.map