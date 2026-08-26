package com.ckgame.world;

import com.ckgame.core.Constants;
import com.ckgame.core.TitleTier;
import com.ckgame.politics.RealmLaw;

import java.util.ArrayList;
import java.util.List;

/** 头衔（领地）。 */
public final class Title {
    public int id;
    public String name;
    public TitleTier tier;
    public String adjective = "";
    public int holder = Constants.NONE_ID;
    public int deJureLiege = Constants.NONE_ID;
    public int deFactoLiege = Constants.NONE_ID;
    public List<Integer> deJureVassals = new ArrayList<>();
    public List<Integer> deFactoVassals = new ArrayList<>();
    public int capital = Constants.NONE_ID;
    public List<Integer> counties = new ArrayList<>();
    public double creationCost = 0.0;
    public boolean destroyable = false;
    public RealmLaw realmLaw = RealmLaw.feudalDefault();

    public Title(int id, String name, TitleTier tier) {
        this.id = id;
        this.name = name;
        this.tier = tier;
    }

    public static Title newTitle(int titleId, String name, TitleTier tier) {
        Title t = new Title(titleId, name, tier);
        t.adjective = name + "的";
        t.creationCost = tier.creationCost();
        t.destroyable = tier.compareTo(TitleTier.DUCHY) >= 0;
        return t;
    }

    public boolean isHeld() {
        return holder != Constants.NONE_ID;
    }

    public void setHolder(int holder) {
        this.holder = holder;
    }

    public void clearHolder() {
        this.holder = Constants.NONE_ID;
    }
}
