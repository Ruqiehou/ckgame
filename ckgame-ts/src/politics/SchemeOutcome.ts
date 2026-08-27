import { SchemeKind } from './SchemeKind.js';

/**
 * Scheme monthly tick outcome.
 */
export class SchemeOutcome {
  public kind: string; // 'success' | 'exposed' | 'progressed'
  public schemeId: number;
  public schemeKind: SchemeKind | null = null;
  public owner: number = 0;
  public target: number = 0;
  public progress: number = 0;

  constructor(kind: string, schemeId: number) {
    this.kind = kind;
    this.schemeId = schemeId;
  }
}
