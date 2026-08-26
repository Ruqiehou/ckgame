package com.ckgame.politics;

import com.ckgame.core.Constants;

/**
 * 角色对头衔或领地的宣称。
 */
public final class Claim {
    public final int claimant;
    public final int title;
    public final int county;
    public boolean pressed = false;
    public int strength = 50;

    public Claim(int claimant, int title, Integer county, int strength) {
        this.claimant = claimant;
        this.title = title;
        this.county = county != null ? county : Constants.NONE_ID;
        this.strength = strength;
    }
}
