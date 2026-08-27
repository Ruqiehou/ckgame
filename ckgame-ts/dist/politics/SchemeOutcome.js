/**
 * Scheme monthly tick outcome.
 */
export class SchemeOutcome {
    kind; // 'success' | 'exposed' | 'progressed'
    schemeId;
    schemeKind = null;
    owner = 0;
    target = 0;
    progress = 0;
    constructor(kind, schemeId) {
        this.kind = kind;
        this.schemeId = schemeId;
    }
}
//# sourceMappingURL=SchemeOutcome.js.map