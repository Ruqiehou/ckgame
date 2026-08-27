/**
 * Two rulers' diplomatic relationship flags.
 */
export class DiplomacyFlags {
    allied = false;
    atWar = false;
    nonAggression = false;
    rival = false;
    marriagePact = false;
    vassalage = false;
    tradeAgreement = false;
    intelligenceSharing = false;
    /** Whether the current relationship blocks war declaration. */
    blocksWar() {
        return this.allied || this.nonAggression || this.marriagePact || this.vassalage;
    }
}
//# sourceMappingURL=DiplomacyFlags.js.map