import type { World } from '../world/World.js';
import { Council } from './Council.js';
import { CouncilPosition } from './CouncilPosition.js';
import { CouncilTask } from './CouncilTask.js';

/**
 * Council registry: manages councils per ruler.
 */
export class CouncilManager {
  private world: World | null;
  private byRuler: Map<number, Council> = new Map();

  constructor(world?: World | null) {
    this.world = world ?? null;
  }

  getWorld(): World | null {
    return this.world;
  }

  /** Get or create a council for the given ruler. */
  councilFor(ruler: number): Council {
    let c = this.byRuler.get(ruler);
    if (!c) {
      c = Council.empty(ruler);
      this.byRuler.set(ruler, c);
    }
    return c;
  }

  /** Get a ruler's council; returns undefined if none exists. */
  get(ruler: number): Council | undefined {
    return this.byRuler.get(ruler);
  }

  /** Return all councils. */
  allCouncils(): Map<number, Council> {
    return this.byRuler;
  }

  /** Serialize council state for save games. */
  saveState(): Record<string, unknown> {
    const s: Record<string, unknown> = {};
    const cm: Record<string, Record<string, unknown>> = {};
    for (const [ruler, c] of this.byRuler) {
      const m: Record<string, unknown> = {
        chancellor: c.chancellor,
        marshal: c.marshal,
        steward: c.steward,
        spymaster: c.spymaster,
        chaplain: c.chaplain,
      };
      const tasks: Record<string, string> = {};
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
  loadState(s: Record<string, unknown>): void {
    this.byRuler.clear();
    const cm = s['councils'] as Record<string, Record<string, unknown>> | undefined;
    if (!cm) return;
    for (const [key, m] of Object.entries(cm)) {
      const ruler = Number(key);
      const c = Council.empty(ruler);
      c.chancellor = Number(m['chancellor'] ?? -1);
      c.marshal = Number(m['marshal'] ?? -1);
      c.steward = Number(m['steward'] ?? -1);
      c.spymaster = Number(m['spymaster'] ?? -1);
      c.chaplain = Number(m['chaplain'] ?? -1);
      const tasks = m['tasks'] as Record<string, string> | undefined;
      if (tasks) {
        c.tasks.clear();
        for (const [posKey, taskKey] of Object.entries(tasks)) {
          c.tasks.set(posKey as CouncilPosition, taskKey as CouncilTask);
        }
      }
      this.byRuler.set(ruler, c);
    }
  }
}
