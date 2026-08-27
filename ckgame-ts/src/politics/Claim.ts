import { NONE_ID } from '../core/Constants.js';

/**
 * A character's claim on a title or county.
 */
export class Claim {
  public pressed: boolean = false;
  public strength: number;

  constructor(
    public readonly claimant: number,
    public readonly title: number,
    public readonly county: number | null,
    strength?: number,
  ) {
    this.strength = strength ?? 50;
    // Normalize null county to NONE_ID for internal use
    if (this.county === null || this.county === undefined) {
      (this as { county: number }).county = NONE_ID;
    }
  }
}
