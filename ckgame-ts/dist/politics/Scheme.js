/**
 * A scheme (conspiracy) object.
 */
export class Scheme {
    id;
    kind;
    owner;
    target;
    progress = 0;
    secrecy = 50;
    agents = [];
    started = null;
    exposed = false;
    constructor(id, kind, owner, target) {
        this.id = id;
        this.kind = kind;
        this.owner = owner;
        this.target = target;
    }
    /** Whether the scheme has completed. */
    isComplete() {
        return this.progress >= 100;
    }
}
//# sourceMappingURL=Scheme.js.map