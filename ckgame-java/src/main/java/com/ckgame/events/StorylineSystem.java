package com.ckgame.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 剧情线系统：管理长期剧情弧。
 */
public final class StorylineSystem {
    public final List<Storyline> storylines = new ArrayList<>();
    public final Map<Integer, Storyline> activePerCharacter = new HashMap<>();

    public void createStoryline(Storyline storyline) {
        storylines.add(storyline);
    }

    public Storyline startStoryline(int characterId, int storylineId) {
        for (Storyline s : storylines) {
            if (s.id == storylineId && s.characterId == characterId) {
                if (s.status == StorylineStatus.AVAILABLE) {
                    s.status = StorylineStatus.ACTIVE;
                    s.currentStage = 0;
                    activePerCharacter.put(characterId, s);
                    return s;
                }
            }
        }
        return null;
    }

    public Storyline advanceStage(int characterId) {
        Storyline storyline = activePerCharacter.get(characterId);
        if (storyline == null || !storyline.isActive()) {
            return null;
        }
        StorylineStage current = null;
        for (StorylineStage s : storyline.stages) {
            if (s.stageId == storyline.currentStage) {
                current = s;
                break;
            }
        }
        if (current != null && current.nextStage != null) {
            StorylineStage next = null;
            for (StorylineStage s : storyline.stages) {
                if (s.stageId == current.nextStage) {
                    next = s;
                    break;
                }
            }
            if (next != null) {
                storyline.currentStage = next.stageId;
                return storyline;
            }
        }
        return null;
    }

    public Storyline completeStoryline(int characterId) {
        Storyline storyline = activePerCharacter.get(characterId);
        if (storyline != null) {
            storyline.status = StorylineStatus.COMPLETED;
            return storyline;
        }
        return null;
    }

    public Storyline failStoryline(int characterId) {
        Storyline storyline = activePerCharacter.get(characterId);
        if (storyline != null) {
            storyline.status = StorylineStatus.FAILED;
            return storyline;
        }
        return null;
    }

    public Storyline getActiveStoryline(int characterId) {
        return activePerCharacter.get(characterId);
    }

    public List<Storyline> getAvailableStorylines(int characterId) {
        List<Storyline> out = new ArrayList<>();
        for (Storyline s : storylines) {
            if (s.characterId == characterId && s.status == StorylineStatus.AVAILABLE) {
                out.add(s);
            }
        }
        return out;
    }

    /** 返回 10 条预设剧情线（与 Python 版 builtin_storylines 一一对应）。 */
    public static List<Storyline> builtinStorylines() {
        List<Storyline> list = new ArrayList<>();

        Storyline s1 = new Storyline(1, "继承危机",
                "你发现远亲对你的领位有野心，必须稳固继承权。", 0);
        s1.startYear = 1066;
        s1.endYear = 1070;
        s1.tags.addAll(List.of("政治", "继承"));
        s1.stages.addAll(List.of(
                new StorylineStage(0, "风声鹤唳", "密探报告远亲在拉拢封臣。", List.of(16)),
                new StorylineStage(1, "拉拢封臣", "你需要争取关键封臣的支持。", List.of(5)),
                new StorylineStage(2, "最后通牒", "远亲公开提出 claim，你必须做出回应。", List.of(24)),
                new StorylineStage(3, "尘埃落定", "危机解除或爆发战争。", List.of())
        ));
        list.add(s1);

        Storyline s2 = new Storyline(2, "宗教狂热",
                "教会请求你支持一场圣战，这将考验你的虔诚与外交。", 0);
        s2.startYear = 1066;
        s2.endYear = 1072;
        s2.tags.addAll(List.of("宗教", "战争"));
        s2.stages.addAll(List.of(
                new StorylineStage(0, "教皇的号召", "教皇来信要求你参与圣战。", List.of(4)),
                new StorylineStage(1, "备战", "你需要筹集资金和军队。", List.of(22)),
                new StorylineStage(2, "圣战开始", "军队集结完毕，开赴前线。", List.of()),
                new StorylineStage(3, "凯旋或陨落", "战争结束，你获得了荣耀或教训。", List.of())
        ));
        list.add(s2);

        Storyline s3 = new Storyline(3, "商路崛起",
                "商会提议开辟一条横跨大陆的商路，这将带来巨大财富。", 0);
        s3.startYear = 1066;
        s3.endYear = 1075;
        s3.tags.addAll(List.of("经济", "贸易"));
        s3.stages.addAll(List.of(
                new StorylineStage(0, "商机初现", "商会代表带来商路计划。", List.of(13)),
                new StorylineStage(1, "投资建设", "你需要投入大量资金建设商路。", List.of(25)),
                new StorylineStage(2, "商路开通", "商路正式开通，财富开始流入。", List.of()),
                new StorylineStage(3, "垄断之争", "其他领主试图抢夺商路控制权。", List.of())
        ));
        list.add(s3);

        Storyline s4 = new Storyline(4, "宫廷阴谋",
                "你发现宫廷中有人密谋反对你，必须找出内鬼。", 0);
        s4.startYear = 1066;
        s4.endYear = 1069;
        s4.tags.addAll(List.of("阴谋", "宫廷"));
        s4.stages.addAll(List.of(
                new StorylineStage(0, "蛛丝马迹", "你收到匿名警告信。", List.of(14)),
                new StorylineStage(1, "暗中调查", "你派出密探调查宫廷异动。", List.of(29)),
                new StorylineStage(2, "阴谋败露", "你发现了叛徒的身份。", List.of(24)),
                new StorylineStage(3, "清洗或宽恕", "你必须决定如何处理叛徒。", List.of())
        ));
        list.add(s4);

        Storyline s5 = new Storyline(5, "联姻外交",
                "通过联姻巩固与邻国的关系，但政治婚姻往往充满变数。", 0);
        s5.startYear = 1066;
        s5.endYear = 1070;
        s5.tags.addAll(List.of("外交", "婚姻"));
        s5.stages.addAll(List.of(
                new StorylineStage(0, "求婚", "你收到了一位贵族的求婚。", List.of(20)),
                new StorylineStage(1, "婚礼", "盛大的婚礼即将举行。", List.of()),
                new StorylineStage(2, "婚后风波", "婚姻生活中出现波折。", List.of(6)),
                new StorylineStage(3, "联盟稳固", "联姻带来了预期的政治利益。", List.of())
        ));
        list.add(s5);

        Storyline s6 = new Storyline(6, "瘟疫蔓延",
                "一场可怕的瘟疫席卷你的领地，你必须做出艰难抉择。", 0);
        s6.startYear = 1066;
        s6.endYear = 1068;
        s6.tags.addAll(List.of("灾难", "瘟疫"));
        s6.stages.addAll(List.of(
                new StorylineStage(0, "疫情初现", "边境村落出现不明疫病。", List.of(8)),
                new StorylineStage(1, "封锁还是救济", "你必须决定如何应对疫情。", List.of(27)),
                new StorylineStage(2, "疫情高峰", "瘟疫达到顶峰，大量人口死亡。", List.of()),
                new StorylineStage(3, "疫情结束", "瘟疫终于过去，但留下了深刻教训。", List.of())
        ));
        list.add(s6);

        Storyline s7 = new Storyline(7, "比武大会",
                "领地举办盛大的比武大会，各地骑士云集，这是展示武勋的绝佳机会。", 0);
        s7.startYear = 1066;
        s7.endYear = 1067;
        s7.tags.addAll(List.of("军事", "荣誉"));
        s7.stages.addAll(List.of(
                new StorylineStage(0, "筹备", "你开始筹备比武大会。", List.of(28)),
                new StorylineStage(1, "比赛日", "骑士们展开激烈角逐。", List.of()),
                new StorylineStage(2, "决赛", "决赛在两位最强骑士之间展开。", List.of()),
                new StorylineStage(3, "加冕", "冠军产生，你授予其荣誉。", List.of())
        ));
        list.add(s7);

        Storyline s8 = new Storyline(8, "外敌入侵",
                "强大的外敌入侵你的边境，你必须组织防御。", 0);
        s8.startYear = 1066;
        s8.endYear = 1068;
        s8.tags.addAll(List.of("战争", "防御"));
        s8.stages.addAll(List.of(
                new StorylineStage(0, "警报", "边境传来敌军入侵的消息。", List.of(22)),
                new StorylineStage(1, "动员", "你召集封臣准备防御。", List.of()),
                new StorylineStage(2, "决战", "两军在主战场相遇。", List.of()),
                new StorylineStage(3, "战后", "战争结束，你评估损失与收获。", List.of())
        ));
        list.add(s8);

        Storyline s9 = new Storyline(9, "文化繁荣",
                "你的宫廷成为文化中心，吸引学者和艺术家前来。", 0);
        s9.startYear = 1066;
        s9.endYear = 1075;
        s9.tags.addAll(List.of("文化", "发展"));
        s9.stages.addAll(List.of(
                new StorylineStage(0, "学者来访", "一位著名学者来到你的宫廷。", List.of(10)),
                new StorylineStage(1, "赞助艺术", "你决定赞助艺术家和建筑师。", List.of(15)),
                new StorylineStage(2, "文化繁荣", "你的宫廷成为文化中心。", List.of()),
                new StorylineStage(3, "遗产", "你留下了不朽的文化遗产。", List.of())
        ));
        list.add(s9);

        Storyline s10 = new Storyline(10, "王朝崛起",
                "你的王朝正在崛起，你需要通过联姻、战争和外交扩张势力。", 0);
        s10.startYear = 1066;
        s10.endYear = 1080;
        s10.tags.addAll(List.of("王朝", "扩张"));
        s10.stages.addAll(List.of(
                new StorylineStage(0, "家族壮大", "你开始规划家族的扩张。", List.of(20)),
                new StorylineStage(1, "第一场战争", "你发动了第一场扩张战争。", List.of()),
                new StorylineStage(2, "联姻巩固", "通过联姻巩固新获得的领土。", List.of(6)),
                new StorylineStage(3, "王朝建立", "你的王朝已经崛起为强大势力。", List.of())
        ));
        list.add(s10);

        return list;
    }
}
