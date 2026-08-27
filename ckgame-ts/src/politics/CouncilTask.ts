/**
 * Council tasks.
 */
export enum CouncilTask {
  DOMESTIC_RELATIONS = 'DOMESTIC_RELATIONS',
  FABRICATE_CLAIM = 'FABRICATE_CLAIM',
  TRAIN_COMMANDERS = 'TRAIN_COMMANDERS',
  INCREASE_CONTROL = 'INCREASE_CONTROL',
  COLLECT_TAXES = 'COLLECT_TAXES',
  DEVELOP_COUNTY = 'DEVELOP_COUNTY',
  DISRUPT_SCHEMES = 'DISRUPT_SCHEMES',
  SUPPORT_MURDER = 'SUPPORT_MURDER',
  CONVERT_FAITH = 'CONVERT_FAITH',
  FABRICATE_HOOK = 'FABRICATE_HOOK',
  RECRUIT_KNIGHTS = 'RECRUIT_KNIGHTS',
  IMPROVE_DIPLOMACY = 'IMPROVE_DIPLOMACY',
  SPREAD_CULTURE = 'SPREAD_CULTURE',
  ESTABLISH_TRADE = 'ESTABLISH_TRADE',
  MAINTAIN_BUILDINGS = 'MAINTAIN_BUILDINGS',
  TRAIN_TROOPS = 'TRAIN_TROOPS',
  GATHER_INTEL = 'GATHER_INTEL',
  PROMOTE_CULTURE = 'PROMOTE_CULTURE',
}

const councilTaskNames: Record<CouncilTask, string> = {
  [CouncilTask.DOMESTIC_RELATIONS]: '内政外交',
  [CouncilTask.FABRICATE_CLAIM]: '伪造宣称',
  [CouncilTask.TRAIN_COMMANDERS]: '训练将领',
  [CouncilTask.INCREASE_CONTROL]: '强化控制',
  [CouncilTask.COLLECT_TAXES]: '催收税赋',
  [CouncilTask.DEVELOP_COUNTY]: '发展领地',
  [CouncilTask.DISRUPT_SCHEMES]: '破坏阴谋',
  [CouncilTask.SUPPORT_MURDER]: '协助密谋',
  [CouncilTask.CONVERT_FAITH]: '传播信仰',
  [CouncilTask.FABRICATE_HOOK]: '神权施压',
  [CouncilTask.RECRUIT_KNIGHTS]: '招募骑士',
  [CouncilTask.IMPROVE_DIPLOMACY]: '改善外交',
  [CouncilTask.SPREAD_CULTURE]: '传播文化',
  [CouncilTask.ESTABLISH_TRADE]: '建立商路',
  [CouncilTask.MAINTAIN_BUILDINGS]: '维护建筑',
  [CouncilTask.TRAIN_TROOPS]: '训练部队',
  [CouncilTask.GATHER_INTEL]: '收集情报',
  [CouncilTask.PROMOTE_CULTURE]: '推广文化',
};

export function councilTaskNameZh(task: CouncilTask): string {
  return councilTaskNames[task] ?? task;
}
