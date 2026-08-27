import { NONE_ID } from '../core/Constants.js';
/**
 * A character's claim on a title or county.
 */
export class Claim {
    claimant;
    title;
    county;
    pressed = false;
    strength;
    constructor(claimant, title, county, strength) {
        this.claimant = claimant;
        this.title = title;
        this.county = county;
        this.strength = strength ?? 50;
        // Normalize null county to NONE_ID for internal use
        if (this.county === null || this.county === undefined) {
            this.county = NONE_ID;
        }
    }
}
//# sourceMappingURL=Claim.js.map