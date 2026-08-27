import { GameDate } from '../core/calendar/GameDate.js';
import { TreatyKind } from './TreatyKind.js';

/**
 * A treaty between two rulers.
 */
export class Treaty {
  constructor(
    public readonly a: number,
    public readonly b: number,
    public readonly kind: TreatyKind,
    public readonly start: GameDate,
    public readonly expiresYear: number,
  ) {}
}
