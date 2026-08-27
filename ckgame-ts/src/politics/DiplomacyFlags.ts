/**
 * Two rulers' diplomatic relationship flags.
 */
export class DiplomacyFlags {
  public allied: boolean = false;
  public atWar: boolean = false;
  public nonAggression: boolean = false;
  public rival: boolean = false;
  public marriagePact: boolean = false;
  public vassalage: boolean = false;
  public tradeAgreement: boolean = false;
  public intelligenceSharing: boolean = false;

  /** Whether the current relationship blocks war declaration. */
  blocksWar(): boolean {
    return this.allied || this.nonAggression || this.marriagePact || this.vassalage;
  }
}
