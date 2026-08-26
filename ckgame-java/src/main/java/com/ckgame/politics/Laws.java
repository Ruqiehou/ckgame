package com.ckgame.politics;

import com.ckgame.core.Gender;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * 政治法律：继承法、王权、性别法与领地法统。
 */
public final class Laws {
    private Laws() {}

    /** 继承法类型。 */
    public enum SuccessionLaw {
        PRIMOGENITURE("长子继承制"),
        CONFEDERATE_PARTITION("联邦分割继承"),
        ELECTIVE("选举君主制"),
        HOUSE_SENIORITY("家族长老制"),
        ULTIMOGENITURE("幼子继承制");

        private final String nameZh;

        SuccessionLaw(String nameZh) {
            this.nameZh = nameZh;
        }

        public String nameZh() {
            return nameZh;
        }
    }

    /** 王权等级。 */
    public enum CrownAuthority {
        AUTONOMOUS(0, "自治王权", 0, 0.0),
        LIMITED(1, "有限王权", -5, 0.05),
        HIGH(2, "高度王权", -15, 0.15),
        ABSOLUTE(3, "绝对王权", -30, 0.25);

        private final int value;
        private final String nameZh;
        private final int vassalOpinionPenalty;
        private final double taxBonus;

        CrownAuthority(int value, String nameZh, int vassalOpinionPenalty, double taxBonus) {
            this.value = value;
            this.nameZh = nameZh;
            this.vassalOpinionPenalty = vassalOpinionPenalty;
            this.taxBonus = taxBonus;
        }

        public int value() {
            return value;
        }

        public String nameZh() {
            return nameZh;
        }

        public int vassalOpinionPenalty() {
            return vassalOpinionPenalty;
        }

        public double taxBonus() {
            return taxBonus;
        }
    }

    /** 性别继承法。 */
    public enum GenderLaw {
        AGNATIC("男系继承"),
        AGNATIC_COGNATIC("男系优先"),
        ABSOLUTE_COGNATIC("绝对双系"),
        ENATIC("女系继承");

        private final String nameZh;

        GenderLaw(String nameZh) {
            this.nameZh = nameZh;
        }

        public String nameZh() {
            return nameZh;
        }

        public boolean allows(Gender gender, boolean hasMaleHeir) {
            return switch (this) {
                case AGNATIC -> gender == Gender.MALE;
                case AGNATIC_COGNATIC -> gender == Gender.MALE || !hasMaleHeir;
                case ENATIC -> gender == Gender.FEMALE;
                case ABSOLUTE_COGNATIC -> true;
            };
        }
    }

    /** 继承人候选项：对应 Python 的元组 (id, gender, birth_ordinal, alive)。 */
    public static record HeirCandidate(int id, Gender gender, int birthOrdinal, boolean alive) {}

    /** 领地法统：继承法、王权、性别继承规则与分割继承。 */
    public static final class RealmLaw {
        private final SuccessionLaw succession;
        private final CrownAuthority crownAuthority;
        private final GenderLaw genderLaw;
        private final boolean partitionEnabled;

        public RealmLaw() {
            this(SuccessionLaw.PRIMOGENITURE, CrownAuthority.LIMITED, GenderLaw.AGNATIC_COGNATIC, false);
        }

        public RealmLaw(SuccessionLaw succession, CrownAuthority crownAuthority,
                        GenderLaw genderLaw, boolean partitionEnabled) {
            this.succession = succession;
            this.crownAuthority = crownAuthority;
            this.genderLaw = genderLaw;
            this.partitionEnabled = partitionEnabled;
        }

        public static RealmLaw feudalDefault() {
            return new RealmLaw();
        }

        public SuccessionLaw succession() {
            return succession;
        }

        public CrownAuthority crownAuthority() {
            return crownAuthority;
        }

        public GenderLaw genderLaw() {
            return genderLaw;
        }

        public boolean partitionEnabled() {
            return partitionEnabled;
        }

        /** 性别排序键：男系继承时男性优先（0），女系继承时女性优先（0）。 */
        private int genderKey(Gender g) {
            return switch (genderLaw) {
                case AGNATIC, AGNATIC_COGNATIC -> g == Gender.MALE ? 0 : 1;
                case ENATIC -> g == Gender.FEMALE ? 0 : 1;
                case ABSOLUTE_COGNATIC -> 0;
            };
        }

        /**
         * 根据继承法挑选继承人。
         *
         * @param children       子女候选列表
         * @param dynastyMembers 王朝成员候选列表
         * @return 被选中的继承人，若无可用人选则返回 null
         */
        public HeirCandidate pickHeir(List<HeirCandidate> children, List<HeirCandidate> dynastyMembers) {
            List<HeirCandidate> livingChildren = children.stream()
                    .filter(HeirCandidate::alive)
                    .toList();
            boolean hasMale = livingChildren.stream().anyMatch(c -> c.gender() == Gender.MALE);

            Predicate<HeirCandidate> eligible = c -> genderLaw.allows(c.gender(), hasMale);

            // 家族长老制：整个王朝中年长优先
            if (succession == SuccessionLaw.HOUSE_SENIORITY) {
                List<HeirCandidate> pool = dynastyMembers.stream()
                        .filter(c -> c.alive() && eligible.test(c))
                        .sorted(Comparator.comparingInt(HeirCandidate::birthOrdinal))
                        .toList();
                return pool.isEmpty() ? null : pool.get(0);
            }

            // 幼子继承制：按性别键与倒序出生序取最幼
            if (succession == SuccessionLaw.ULTIMOGENITURE) {
                List<HeirCandidate> pool = livingChildren.stream()
                        .filter(eligible)
                        .sorted(Comparator.<HeirCandidate>comparingInt(c -> genderKey(c.gender()))
                                .thenComparing(Comparator.comparingInt(HeirCandidate::birthOrdinal).reversed()))
                        .toList();
                return pool.isEmpty() ? null : pool.get(0);
            }

            // 长子 / 分割 / 选举：简化为长嗣
            List<HeirCandidate> pool = livingChildren.stream()
                    .filter(eligible)
                    .sorted(Comparator.<HeirCandidate>comparingInt(c -> genderKey(c.gender()))
                            .thenComparingInt(HeirCandidate::birthOrdinal))
                    .toList();
            if (!pool.isEmpty()) {
                return pool.get(0);
            }

            // 无子女则取王朝成员
            List<HeirCandidate> fallback = dynastyMembers.stream()
                    .filter(c -> c.alive() && eligible.test(c))
                    .sorted(Comparator.<HeirCandidate>comparingInt(c -> genderKey(c.gender()))
                            .thenComparingInt(HeirCandidate::birthOrdinal))
                    .toList();
            return fallback.isEmpty() ? null : fallback.get(0);
        }

        /**
         * 分割继承分配：将一组头衔按轮转方式分配给若干继承人。
         *
         * @param titles 待分配的头衔 id 列表
         * @param heirs  继承人 id 列表
         * @return 每个继承人分得的头衔列表
         */
        public List<TitledHeir> partitionTitles(List<Integer> titles, List<Integer> heirs) {
            if (heirs.isEmpty()) {
                return List.of();
            }
            if (!partitionEnabled || succession != SuccessionLaw.CONFEDERATE_PARTITION) {
                return List.of(new TitledHeir(heirs.get(0), new ArrayList<>(titles)));
            }
            List<TitledHeir> result = new ArrayList<>();
            for (int h : heirs) {
                result.add(new TitledHeir(h, new ArrayList<>()));
            }
            for (int i = 0; i < titles.size(); i++) {
                result.get(i % heirs.size()).titles().add(titles.get(i));
            }
            return result;
        }

        /** 分割结果行：持有人 + 分得的头衔列表。 */
        public record TitledHeir(int heirId, List<Integer> titles) {}
    }
}
