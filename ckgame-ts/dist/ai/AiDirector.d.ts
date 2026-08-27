import type { AiAction } from './AiAction.js';
import type { World } from '../world/World.js';
import type { WarManager } from '../military/WarManager.js';
import { Diplomacy } from '../politics/Diplomacy.js';
import type { SchemeManager } from '../politics/SchemeManager.js';
export declare class AiDirector {
    static monthlyActions(world: World, wars: WarManager, diplomacy: Diplomacy, schemes: SchemeManager, playerIds: Set<number>): AiAction[];
    static applyActions(world: World, wars: WarManager, diplomacy: Diplomacy, schemes: SchemeManager, actions: AiAction[]): void;
}
