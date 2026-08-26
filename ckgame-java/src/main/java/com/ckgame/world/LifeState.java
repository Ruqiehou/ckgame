package com.ckgame.world;

/** 生命状态。 */
public enum LifeState {
    ALIVE(true),
    DEAD(false);

    private final boolean alive;

    LifeState(boolean alive) {
        this.alive = alive;
    }

    public boolean isAlive() { return alive; }
}
