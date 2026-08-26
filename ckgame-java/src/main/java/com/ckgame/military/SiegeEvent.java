package com.ckgame.military;

/**
 * 围城事件。
 */
public final class SiegeEvent {
    public final String kind;       // progressed / captured / lifted
    public final int siegeId;
    public final int county;
    public final int attacker;
    public final int defender;
    public final double progress;
    public final double required;
    public final String reason;

    public SiegeEvent(String kind, int siegeId, int county,
                      int attacker, int defender,
                      double progress, double required, String reason) {
        this.kind = kind;
        this.siegeId = siegeId;
        this.county = county;
        this.attacker = attacker;
        this.defender = defender;
        this.progress = progress;
        this.required = required;
        this.reason = reason;
    }

    public SiegeEvent(String kind, int siegeId, int county, double progress, double required) {
        this(kind, siegeId, county, 0, 0, progress, required, "");
    }

    public SiegeEvent(String kind, int siegeId, int county, int attacker, int defender) {
        this(kind, siegeId, county, attacker, defender, 0.0, 0.0, "");
    }

    public SiegeEvent(String kind, int siegeId, int county, String reason) {
        this(kind, siegeId, county, 0, 0, 0.0, 0.0, reason);
    }
}
