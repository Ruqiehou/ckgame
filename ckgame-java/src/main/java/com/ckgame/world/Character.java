package com.ckgame.world;

import com.ckgame.core.Constants;
import com.ckgame.core.Gender;
import com.ckgame.core.calendar.GameDate;
import com.ckgame.core.stats.AttributeSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 角色。字段保持可变，贴近 Python dataclass 语义。
 */
public final class Character {
    public int id;
    public String name;
    public int dynasty = Constants.NONE_ID;
    public Gender gender = Gender.MALE;
    public GameDate birth;
    public GameDate death;
    public LifeState life = LifeState.ALIVE;
    public AttributeSet baseAttrs = AttributeSet.defaults();
    public List<Integer> traits = new ArrayList<>();
    public int culture = 0;
    public int faith = 0;
    public double gold = 50.0;
    public double prestige = 50.0;
    public double piety = 50.0;
    public int stress = 0;
    public double health = 5.0;
    public double fertility = 0.5;
    public int father = Constants.NONE_ID;
    public int mother = Constants.NONE_ID;
    public List<Integer> spouses = new ArrayList<>();
    public List<Integer> children = new ArrayList<>();
    public List<Integer> heldTitles = new ArrayList<>();
    public int primaryTitle = Constants.NONE_ID;
    public boolean isRuler = false;
    public int employer = Constants.NONE_ID;
    public Map<Integer, Integer> opinionCache = new HashMap<>();
    public int level = 1;
    public int xp = 0;

    public Character(int id, String name, int dynasty, Gender gender, GameDate birth) {
        this.id = id;
        this.name = name;
        this.dynasty = dynasty;
        this.gender = gender;
        this.birth = birth;
    }

    public int ageAt(GameDate date) {
        int age = date.year() - birth.year();
        if (date.month() < birth.month() ||
                (date.month() == birth.month() && date.day() < birth.day())) {
            age -= 1;
        }
        return Math.max(0, age);
    }

    public boolean isAdult(GameDate date) {
        return ageAt(date) >= 16;
    }

    public boolean isAlive() {
        return life == LifeState.ALIVE;
    }

    public boolean isMarried() {
        return !spouses.isEmpty();
    }

    public AttributeSet effectiveAttrs(AttributeSet traitBonus) {
        return baseAttrs.add(traitBonus);
    }

    public void kill(GameDate date) {
        life = LifeState.DEAD;
        death = date;
        isRuler = false;
    }

    public void addGold(double amount) {
        gold = Math.max(0.0, gold + amount);
    }

    public void addPrestige(double amount) {
        prestige = Math.max(0.0, prestige + amount);
    }

    public void addStress(int amount) {
        stress = Math.max(0, Math.min(400, stress + amount));
    }

    /** 获得经验值，返回是否升级。 */
    public boolean gainXp(int amount) {
        xp += amount;
        boolean leveled = false;
        while (xp >= xpToNextLevel()) {
            xp -= xpToNextLevel();
            level += 1;
            leveled = true;
        }
        return leveled;
    }

    /** 升级所需经验值，随等级递增。 */
    public int xpToNextLevel() {
        return 100 + (level - 1) * 50;
    }
}
