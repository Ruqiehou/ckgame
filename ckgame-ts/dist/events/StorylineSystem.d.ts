import { Storyline } from './Storyline.js';
/**
 * Storyline system: manages long-term narrative arcs.
 */
export declare class StorylineSystem {
    storylines: Storyline[];
    activePerCharacter: Map<number, Storyline>;
    /** Register a storyline. */
    createStoryline(storyline: Storyline): void;
    /** Start a storyline for a character. Returns the storyline or null. */
    startStoryline(characterId: number, storylineId: number): Storyline | null;
    /** Advance the current stage for a character's active storyline. Returns the storyline or null. */
    advanceStage(characterId: number): Storyline | null;
    /** Complete a character's active storyline. */
    completeStoryline(characterId: number): Storyline | null;
    /** Fail a character's active storyline. */
    failStoryline(characterId: number): Storyline | null;
    /** Get a character's active storyline. */
    getActiveStoryline(characterId: number): Storyline | undefined;
    /** Get all available storylines for a character. */
    getAvailableStorylines(characterId: number): Storyline[];
    /** Return 10 preset storylines. */
    static builtinStorylines(): Storyline[];
}
