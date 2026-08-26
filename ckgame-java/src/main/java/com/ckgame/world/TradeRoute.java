package com.ckgame.world;

/** 贸易路线：起点-终点、贸易量与汇率。 */
public record TradeRoute(String from, String to, double tradeVolume, double exchangeRate) {
    public String key() { return from + "|" + to; }
}
