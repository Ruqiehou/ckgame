import { ScenarioLoader } from './ScenarioLoader.js';
import type { World } from '../world/World.js';

export class Scenario1066 {
  static build(): World {
    return ScenarioLoader.loadScenario();
  }
  static buildFromPath(p: string): World {
    return ScenarioLoader.loadScenario(p);
  }
}
