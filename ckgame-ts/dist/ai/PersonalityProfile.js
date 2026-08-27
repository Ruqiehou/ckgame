export function computePersonality(character, world) {
    const p = {
        aggression: 0.5, diplomacy: 0.5, cunning: 0.5, ambition: 0.5,
        greed: 0.5, piety: 0.5, martial_prowess: 0.5, stewardship: 0.5,
    };
    if (!character)
        return p;
    for (const tid of character.traits) {
        switch (tid) {
            case 1:
                p.aggression += 0.1;
                p.martial_prowess += 0.3;
                break;
            case 2:
                p.cunning += 0.25;
                p.greed += 0.1;
                break;
            case 3:
                p.diplomacy += 0.3;
                p.aggression -= 0.1;
                break;
            case 4:
                p.greed += 0.4;
                p.ambition += 0.1;
                break;
            case 5:
                p.diplomacy += 0.25;
                p.aggression -= 0.15;
                break;
            case 6:
                p.aggression += 0.2;
                p.martial_prowess += 0.25;
                break;
            case 7:
                p.aggression -= 0.2;
                p.martial_prowess -= 0.1;
                break;
            case 8:
                p.piety += 0.15;
                p.diplomacy += 0.1;
                break;
            case 9:
                p.ambition += 0.3;
                p.aggression += 0.2;
                p.greed += 0.1;
                break;
            case 10:
                p.diplomacy += 0.35;
                p.ambition -= 0.2;
                break;
            case 11:
                p.aggression += 0.15;
                p.cunning += 0.3;
                break;
            case 12:
                p.diplomacy += 0.35;
                break;
            default: break;
        }
    }
    const attrs = world.effectiveAttrs ? world.effectiveAttrs(character.id) : null;
    if (attrs) {
        p.aggression += (attrs.martial - 8) * 0.03;
        p.greed += (attrs.stewardship - 8) * 0.02;
        p.piety += (attrs.learning - 8) * 0.02;
        p.diplomacy += (attrs.diplomacy - 8) * 0.03;
        p.cunning += (attrs.intrigue - 8) * 0.02;
        p.martial_prowess += (attrs.martial - 8) * 0.02;
        p.stewardship += (attrs.stewardship - 8) * 0.02;
        p.ambition -= (character.stress / 400.0) * 0.2;
    }
    const age = character.birth ? world.date.year() - character.birth.year() : 30;
    if (age < 25) {
        p.ambition += 0.1;
        p.aggression += 0.05;
    }
    else if (age > 55) {
        p.ambition -= 0.1;
        p.aggression -= 0.05;
        p.diplomacy += 0.05;
    }
    const cl = (v) => Math.max(0, Math.min(1, v));
    p.aggression = cl(p.aggression);
    p.diplomacy = cl(p.diplomacy);
    p.cunning = cl(p.cunning);
    p.ambition = cl(p.ambition);
    p.greed = cl(p.greed);
    p.piety = cl(p.piety);
    p.martial_prowess = cl(p.martial_prowess);
    p.stewardship = cl(p.stewardship);
    return p;
}
//# sourceMappingURL=PersonalityProfile.js.map