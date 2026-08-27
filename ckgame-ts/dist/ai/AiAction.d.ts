/**
 * AI action to be executed.
 */
export interface AiAction {
    type: string;
    actor: number;
    target?: number;
    params?: Record<string, any>;
}
