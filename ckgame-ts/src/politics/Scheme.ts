import { GameDate } from '../core/calendar/GameDate.js';
import { SchemeKind } from './SchemeKind.js';

/**
 * A scheme (conspiracy) object.
 */
export class Scheme {
  public readonly id: number;
  public readonly kind: SchemeKind;
  public readonly owner: number;
  public readonly target: number;
  public progress: number = 0;
  public secrecy: number = 50;
  public agents: number[] = [];
  public started: GameDate | null = null;
  public exposed: boolean = false;

  constructor(id: number, kind: SchemeKind, owner: number, target: number) {
    this.id = id;
    this.kind = kind;
    this.owner = owner;
    this.target = target;
  }

  /** Whether the scheme has completed. */
  isComplete(): boolean {
    return this.progress >= 100;
  }
}
