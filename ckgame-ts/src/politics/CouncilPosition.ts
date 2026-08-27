/**
 * Council positions.
 */
export enum CouncilPosition {
  CHANCELLOR = 'CHANCELLOR',
  MARSHAL = 'MARSHAL',
  STEWARD = 'STEWARD',
  SPYMASTER = 'SPYMASTER',
  COURT_CHAPLAIN = 'COURT_CHAPLAIN',
}

const councilPositionNames: Record<CouncilPosition, string> = {
  [CouncilPosition.CHANCELLOR]: '首相',
  [CouncilPosition.MARSHAL]: '元帅',
  [CouncilPosition.STEWARD]: '总管',
  [CouncilPosition.SPYMASTER]: '间谍总管',
  [CouncilPosition.COURT_CHAPLAIN]: '宫廷神甫',
};

export function councilPositionNameZh(pos: CouncilPosition): string {
  return councilPositionNames[pos] ?? pos;
}

/** All council positions. */
export function allCouncilPositions(): CouncilPosition[] {
  return [
    CouncilPosition.CHANCELLOR,
    CouncilPosition.MARSHAL,
    CouncilPosition.STEWARD,
    CouncilPosition.SPYMASTER,
    CouncilPosition.COURT_CHAPLAIN,
  ];
}
