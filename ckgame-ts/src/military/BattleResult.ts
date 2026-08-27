/**
 * Battle result interface.
 */
export interface BattleResult {
  attackerWon: boolean;
  attackerLosses: number;
  defenderLosses: number;
  warscoreChange: number;
  description: string;
}
