package com.ckgame.world;

import com.ckgame.core.Constants;

import java.util.ArrayList;
import java.util.List;

/** 王朝：家族实体，成员列表与家族威望。 */
public final class Dynasty {
    public int id;
    public String name;
    public int head = Constants.NONE_ID;
    public int founder = Constants.NONE_ID;
    public List<Integer> members = new ArrayList<>();
    public int colorR = 128, colorG = 128, colorB = 128;
    public String motto = "";
    public double prestige = 0.0;

    public Dynasty(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public static Dynasty newDynasty(int dynastyId, String name, int head) {
        Dynasty d = new Dynasty(dynastyId, name);
        d.head = head;
        d.founder = head;
        return d;
    }

    public void addMember(int who) {
        if (!members.contains(who)) {
            members.add(who);
        }
    }
}
