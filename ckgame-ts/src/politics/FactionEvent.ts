import { FactionKind } from './FactionKind.js';

/**
 * Faction-related event.
 */
export interface FactionEvent {
  kind: string;
  factionId: number;
  liege: number;
  founder: number;
  who: number;
  factionKind?: FactionKind;
  members?: number[];
  reason?: string;
}

/**
 * Create a FactionEvent with defaults.
 */
export function createFactionEvent(
  kind: string,
  factionId: number,
  liege: number = 0,
  founder: number = 0,
  who: number = 0,
  factionKind?: FactionKind,
  members?: number[],
  reason?: string,
): FactionEvent {
  return {
    kind,
    factionId,
    liege,
    founder,
    who,
    factionKind,
    members: members ? [...members] : undefined,
    reason: reason ?? '',
  };
}
