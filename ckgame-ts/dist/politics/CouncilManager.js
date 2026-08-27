import { Council } from './Council.js';
/**
 * Council registry: manages councils per ruler.
 */
export class CouncilManager {
    world;
    byRuler = new Map();
    constructor(world) {
        this.world = world ?? null;
    }
    getWorld() {
        return this.world;
    }
    /** Get or create a council for the given ruler. */
    councilFor(ruler) {
        let c = this.byRuler.get(ruler);
        if (!c) {
            c = Council.empty(ruler);
            this.byRuler.set(ruler, c);
        }
        return c;
    }
    /** Get a ruler's council; returns undefined if none exists. */
    get(ruler) {
        return this.byRuler.get(ruler);
    }
    /** Return all councils. */
    allCouncils() {
        return this.byRuler;
    }
    /** Serialize council state for save games. */
    saveState() {
        const s = {};
        const cm = {};
        for (const [ruler, c] of this.byRuler) {
            const m = {
                chancellor: c.chancellor,
                marshal: c.marshal,
                steward: c.steward,
                spymaster: c.spymaster,
                chaplain: c.chaplain,
            };
            const tasks = {};
            for (const [pos, task] of c.tasks) {
                tasks[pos] = task;
            }
            m['tasks'] = tasks;
            cm[String(ruler)] = m;
        }
        s['councils'] = cm;
        return s;
    }
    /** Restore council state from a save. */
    loadState(s) {
        this.byRuler.clear();
        const cm = s['councils'];
        if (!cm)
            return;
        for (const [key, m] of Object.entries(cm)) {
            const ruler = Number(key);
            const c = Council.empty(ruler);
            c.chancellor = Number(m['chancellor'] ?? -1);
            c.marshal = Number(m['marshal'] ?? -1);
            c.steward = Number(m['steward'] ?? -1);
            c.spymaster = Number(m['spymaster'] ?? -1);
            c.chaplain = Number(m['chaplain'] ?? -1);
            const tasks = m['tasks'];
            if (tasks) {
                c.tasks.clear();
                for (const [posKey, taskKey] of Object.entries(tasks)) {
                    c.tasks.set(posKey, taskKey);
                }
            }
            this.byRuler.set(ruler, c);
        }
    }
}
//# sourceMappingURL=CouncilManager.js.map