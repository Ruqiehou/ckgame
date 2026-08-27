export enum Season {
  SPRING = 'SPRING',
  SUMMER = 'SUMMER',
  AUTUMN = 'AUTUMN',
  WINTER = 'WINTER',
}

export function seasonZh(s: Season): string {
  switch (s) {
    case Season.SPRING: return '春';
    case Season.SUMMER: return '夏';
    case Season.AUTUMN: return '秋';
    case Season.WINTER: return '冬';
  }
}
