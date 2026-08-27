import { Storyline } from './Storyline.js';
import { StorylineStage } from './StorylineStage.js';
import { StorylineStatus } from './StorylineStatus.js';

/**
 * Storyline system: manages long-term narrative arcs.
 */
export class StorylineSystem {
  public storylines: Storyline[] = [];
  public activePerCharacter: Map<number, Storyline> = new Map();

  /** Register a storyline. */
  createStoryline(storyline: Storyline): void {
    this.storylines.push(storyline);
  }

  /** Start a storyline for a character. Returns the storyline or null. */
  startStoryline(characterId: number, storylineId: number): Storyline | null {
    for (const s of this.storylines) {
      if (s.id === storylineId && s.characterId === characterId) {
        if (s.status === StorylineStatus.AVAILABLE) {
          s.status = StorylineStatus.ACTIVE;
          s.currentStage = 0;
          this.activePerCharacter.set(characterId, s);
          return s;
        }
      }
    }
    return null;
  }

  /** Advance the current stage for a character's active storyline. Returns the storyline or null. */
  advanceStage(characterId: number): Storyline | null {
    const storyline = this.activePerCharacter.get(characterId);
    if (!storyline || !storyline.isActive()) return null;

    const current = storyline.stages.find(s => s.stageId === storyline.currentStage);
    if (current && current.nextStage !== null) {
      const next = storyline.stages.find(s => s.stageId === current.nextStage!);
      if (next) {
        storyline.currentStage = next.stageId;
        return storyline;
      }
    }
    return null;
  }

  /** Complete a character's active storyline. */
  completeStoryline(characterId: number): Storyline | null {
    const storyline = this.activePerCharacter.get(characterId);
    if (storyline) {
      storyline.status = StorylineStatus.COMPLETED;
      return storyline;
    }
    return null;
  }

  /** Fail a character's active storyline. */
  failStoryline(characterId: number): Storyline | null {
    const storyline = this.activePerCharacter.get(characterId);
    if (storyline) {
      storyline.status = StorylineStatus.FAILED;
      return storyline;
    }
    return null;
  }

  /** Get a character's active storyline. */
  getActiveStoryline(characterId: number): Storyline | undefined {
    return this.activePerCharacter.get(characterId);
  }

  /** Get all available storylines for a character. */
  getAvailableStorylines(characterId: number): Storyline[] {
    return this.storylines.filter(
      s => s.characterId === characterId && s.status === StorylineStatus.AVAILABLE,
    );
  }

  /** Return 10 preset storylines. */
  static builtinStorylines(): Storyline[] {
    const list: Storyline[] = [];

    const s1 = new Storyline(1, '继承危机',
      '你发现远亲对你的领位有野心，必须稳固继承权。', 0);
    s1.startYear = 1066;
    s1.endYear = 1070;
    s1.tags.push('政治', '继承');
    s1.stages.push(
      new StorylineStage(0, '风声鹤唳', '密探报告远亲在拉拢封臣。', [16], 1),
      new StorylineStage(1, '拉拢封臣', '你需要争取关键封臣的支持。', [5], 2),
      new StorylineStage(2, '最后通牒', '远亲公开提出 claim，你必须做出回应。', [24], 3),
      new StorylineStage(3, '尘埃落定', '危机解除或爆发战争。', []),
    );
    list.push(s1);

    const s2 = new Storyline(2, '宗教狂热',
      '教会请求你支持一场圣战，这将考验你的虔诚与外交。', 0);
    s2.startYear = 1066;
    s2.endYear = 1072;
    s2.tags.push('宗教', '战争');
    s2.stages.push(
      new StorylineStage(0, '教皇的号召', '教皇来信要求你参与圣战。', [4], 1),
      new StorylineStage(1, '备战', '你需要筹集资金和军队。', [22], 2),
      new StorylineStage(2, '圣战开始', '军队集结完毕，开赴前线。', [], 3),
      new StorylineStage(3, '凯旋或陨落', '战争结束，你获得了荣耀或教训。', []),
    );
    list.push(s2);

    const s3 = new Storyline(3, '商路崛起',
      '商会提议开辟一条横跨大陆的商路，这将带来巨大财富。', 0);
    s3.startYear = 1066;
    s3.endYear = 1075;
    s3.tags.push('经济', '贸易');
    s3.stages.push(
      new StorylineStage(0, '商机初现', '商会代表带来商路计划。', [13], 1),
      new StorylineStage(1, '投资建设', '你需要投入大量资金建设商路。', [25], 2),
      new StorylineStage(2, '商路开通', '商路正式开通，财富开始流入。', [], 3),
      new StorylineStage(3, '垄断之争', '其他领主试图抢夺商路控制权。', []),
    );
    list.push(s3);

    const s4 = new Storyline(4, '宫廷阴谋',
      '你发现宫廷中有人密谋反对你，必须找出内鬼。', 0);
    s4.startYear = 1066;
    s4.endYear = 1069;
    s4.tags.push('阴谋', '宫廷');
    s4.stages.push(
      new StorylineStage(0, '蛛丝马迹', '你收到匿名警告信。', [14], 1),
      new StorylineStage(1, '暗中调查', '你派出密探调查宫廷异动。', [29], 2),
      new StorylineStage(2, '阴谋败露', '你发现了叛徒的身份。', [24], 3),
      new StorylineStage(3, '清洗或宽恕', '你必须决定如何处理叛徒。', []),
    );
    list.push(s4);

    const s5 = new Storyline(5, '联姻外交',
      '通过联姻巩固与邻国的关系，但政治婚姻往往充满变数。', 0);
    s5.startYear = 1066;
    s5.endYear = 1070;
    s5.tags.push('外交', '婚姻');
    s5.stages.push(
      new StorylineStage(0, '求婚', '你收到了一位贵族的求婚。', [20], 1),
      new StorylineStage(1, '婚礼', '盛大的婚礼即将举行。', [], 2),
      new StorylineStage(2, '婚后风波', '婚姻生活中出现波折。', [6], 3),
      new StorylineStage(3, '联盟稳固', '联姻带来了预期的政治利益。', []),
    );
    list.push(s5);

    const s6 = new Storyline(6, '瘟疫蔓延',
      '一场可怕的瘟疫席卷你的领地，你必须做出艰难抉择。', 0);
    s6.startYear = 1066;
    s6.endYear = 1068;
    s6.tags.push('灾难', '瘟疫');
    s6.stages.push(
      new StorylineStage(0, '疫情初现', '边境村落出现不明疫病。', [8], 1),
      new StorylineStage(1, '封锁还是救济', '你必须决定如何应对疫情。', [27], 2),
      new StorylineStage(2, '疫情高峰', '瘟疫达到顶峰，大量人口死亡。', [], 3),
      new StorylineStage(3, '疫情结束', '瘟疫终于过去，但留下了深刻教训。', []),
    );
    list.push(s6);

    const s7 = new Storyline(7, '比武大会',
      '领地举办盛大的比武大会，各地骑士云集，这是展示武勋的绝佳机会。', 0);
    s7.startYear = 1066;
    s7.endYear = 1067;
    s7.tags.push('军事', '荣誉');
    s7.stages.push(
      new StorylineStage(0, '筹备', '你开始筹备比武大会。', [28], 1),
      new StorylineStage(1, '比赛日', '骑士们展开激烈角逐。', [], 2),
      new StorylineStage(2, '决赛', '决赛在两位最强骑士之间展开。', [], 3),
      new StorylineStage(3, '加冕', '冠军产生，你授予其荣誉。', []),
    );
    list.push(s7);

    const s8 = new Storyline(8, '外敌入侵',
      '强大的外敌入侵你的边境，你必须组织防御。', 0);
    s8.startYear = 1066;
    s8.endYear = 1068;
    s8.tags.push('战争', '防御');
    s8.stages.push(
      new StorylineStage(0, '警报', '边境传来敌军入侵的消息。', [22], 1),
      new StorylineStage(1, '动员', '你召集封臣准备防御。', [], 2),
      new StorylineStage(2, '决战', '两军在主战场相遇。', [], 3),
      new StorylineStage(3, '战后', '战争结束，你评估损失与收获。', []),
    );
    list.push(s8);

    const s9 = new Storyline(9, '文化繁荣',
      '你的宫廷成为文化中心，吸引学者和艺术家前来。', 0);
    s9.startYear = 1066;
    s9.endYear = 1075;
    s9.tags.push('文化', '发展');
    s9.stages.push(
      new StorylineStage(0, '学者来访', '一位著名学者来到你的宫廷。', [10], 1),
      new StorylineStage(1, '赞助艺术', '你决定赞助艺术家和建筑师。', [15], 2),
      new StorylineStage(2, '文化繁荣', '你的宫廷成为文化中心。', [], 3),
      new StorylineStage(3, '遗产', '你留下了不朽的文化遗产。', []),
    );
    list.push(s9);

    const s10 = new Storyline(10, '王朝崛起',
      '你的王朝正在崛起，你需要通过联姻、战争和外交扩张势力。', 0);
    s10.startYear = 1066;
    s10.endYear = 1080;
    s10.tags.push('王朝', '扩张');
    s10.stages.push(
      new StorylineStage(0, '家族壮大', '你开始规划家族的扩张。', [20], 1),
      new StorylineStage(1, '第一场战争', '你发动了第一场扩张战争。', [], 2),
      new StorylineStage(2, '联姻巩固', '通过联姻巩固新获得的领土。', [6], 3),
      new StorylineStage(3, '王朝建立', '你的王朝已经崛起为强大势力。', []),
    );
    list.push(s10);

    return list;
  }
}
