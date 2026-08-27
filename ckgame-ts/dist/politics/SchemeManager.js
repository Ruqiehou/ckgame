import { GameDate } from '../core/calendar/GameDate.js';
import { Scheme } from './Scheme.js';
import { schemeKindBaseMonthlyProgress, schemeKindSecrecyBase } from './SchemeKind.js';
import { SchemeOutcome } from './SchemeOutcome.js';
/**
 * Scheme manager: tracks and ticks all schemes.
 */
export class SchemeManager {
    world;
    schemes = new Map();
    nextId = 1;
    constructor(world) {
        this.world = world ?? null;
    }
    getWorld() {
        return this.world;
    }
    /** Return all active schemes (read-only view). */
    getSchemes() {
        return this.schemes;
    }
    /** Return scheme values as iterable. */
    schemeValues() {
        return this.schemes.values();
    }
    /** Start a scheme and return its id. */
    start(kind, owner, target, date) {
        const sid = this.nextId++;
        const s = new Scheme(sid, kind, owner, target);
        s.secrecy = schemeKindSecrecyBase(kind);
        s.started = date;
        this.schemes.set(sid, s);
        return sid;
    }
    /**
     * Monthly tick for all schemes.
     * @param intrigueOf Map from character id to intrigue skill
     * @param rngRoll Function returning 0~1 random number
     */
    monthlyTick(intrigueOf, rngRoll) {
        const outcomes = [];
        const completed = [];
        const exposed = [];
        for (const s of this.schemes.values()) {
            const intrigue = intrigueOf.get(s.owner) ?? 8;
            const targetIntrigue = intrigueOf.get(s.target) ?? 8;
            const gain = schemeKindBaseMonthlyProgress(s.kind)
                + intrigue * 0.5
                + s.agents.length * 2.0
                - targetIntrigue * 0.2;
            s.progress = Math.min(100, s.progress + Math.max(0.5, gain));
            const discovery = Math.max(0.01, Math.min(0.4, (100 - s.secrecy) * 0.01 + targetIntrigue * 0.005));
            if (rngRoll() < discovery) {
                s.exposed = true;
                exposed.push(s.id);
            }
            const progressed = new SchemeOutcome('progressed', s.id);
            progressed.progress = s.progress;
            progressed.owner = s.owner;
            progressed.target = s.target;
            outcomes.push(progressed);
            if (s.isComplete()) {
                completed.push(s.id);
            }
        }
        // Exposed schemes
        for (const sid of exposed) {
            const s = this.schemes.get(sid);
            if (s) {
                const o = new SchemeOutcome('exposed', sid);
                o.owner = s.owner;
                o.target = s.target;
                o.schemeKind = s.kind;
                outcomes.push(o);
            }
        }
        // Completed schemes
        for (const sid of completed) {
            const s = this.schemes.get(sid);
            if (s) {
                this.schemes.delete(sid);
                const o = new SchemeOutcome('success', sid);
                o.schemeKind = s.kind;
                o.owner = s.owner;
                o.target = s.target;
                outcomes.push(o);
            }
        }
        return outcomes;
    }
    /** Serialize scheme state for save games. */
    saveState() {
        const s = {};
        s['nextId'] = this.nextId;
        const sl = [];
        for (const sc of this.schemes.values()) {
            sl.push({
                id: sc.id,
                kind: sc.kind,
                owner: sc.owner,
                target: sc.target,
                progress: sc.progress,
                secrecy: sc.secrecy,
                agents: [...sc.agents],
                started: sc.started ? [sc.started.year(), sc.started.month(), sc.started.day()] : null,
                exposed: sc.exposed,
            });
        }
        s['schemes'] = sl;
        return s;
    }
    /** Restore scheme state from a save. */
    loadState(s) {
        this.schemes.clear();
        this.nextId = Number(s['nextId'] ?? 1);
        const sl = s['schemes'];
        if (!sl)
            return;
        for (const m of sl) {
            const id = Number(m['id']);
            const kind = m['kind'];
            const sc = new Scheme(id, kind, Number(m['owner']), Number(m['target']));
            sc.progress = Number(m['progress']);
            sc.secrecy = Number(m['secrecy']);
            sc.agents = [...m['agents']];
            const sd = m['started'];
            if (sd) {
                sc.started = new GameDate(sd[0], sd[1], sd[2]);
            }
            sc.exposed = Boolean(m['exposed']);
            this.schemes.set(id, sc);
        }
    }
}
//# sourceMappingURL=SchemeManager.js.map