package com.ckgame.politics;

import com.ckgame.core.Gender;
import com.ckgame.core.Constants;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 领地法统：继承法、王权、性别继承规则与分割继承。
 */
public final class RealmLaw {
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

    public SuccessionLaw succession() { return succession; }
    public CrownAuthority crownAuthority() { return crownAuthority; }
    public GenderLaw genderLaw() { return genderLaw; }
    public boolean partitionEnabled() { return partitionEnabled; }

    /** 性别排序键：男系继承时男性优先（0），女系继承时女性优先（0）。 */
    private int genderKey(Gender g) {
        if (genderLaw == GenderLaw.AGNATIC || genderLaw == GenderLaw.AGNATIC_COGNATIC) {
            return g == Gender.MALE ? 0 : 1;
        }
        if (genderLaw == GenderLaw.ENATIC) {
            return g == Gender.FEMALE ? 0 : 1;
        }
        return 0;
    }

    public HeirCandidate pickHeir(List<HeirCandidate> children, List<HeirCandidate> dynastyMembers) {
        List<HeirCandidate> livingChildren = children.stream()
                .filter(HeirCandidate::alive)
                .toList();
        boolean hasMale = livingChildren.stream().anyMatch(c -> c.gender() == Gender.MALE);

        java.util.function.Predicate<HeirCandidate> eligible =
                c -> genderLaw.allows(c.gender(), hasMale);

        // 家族长老制：整个王朝中年长优先
        if (succession == SuccessionLaw.HOUSE_SENIORITY) {
            List<HeirCandidate> pool = dynastyMembers.stream()
                    .filter(c -> c.alive() && eligible.test(c))
                    .sorted(Comparator.comparingInt(HeirCandidate::birthOrdinal))
                    .toList();
            return pool.isEmpty() ? null : pool.get(0);
        }

        // 幼子继承制：年长优先…实际按出生序倒序取最幼
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

    /** 分割继承分配：返回 (头衔 id, 继承人 id) 列表。 */
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
