/** 贸易路线：起点-终点、贸易量与汇率。 */
export interface TradeRoute {
    from: string;
    to: string;
    exchangeRate: number;
    tradeVolume: number;
}
export declare function tradeRouteKey(route: TradeRoute): string;
