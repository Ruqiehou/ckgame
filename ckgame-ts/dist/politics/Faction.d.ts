import { FactionKind } from './FactionKind.js';
/**
 * A faction object.
 */
export declare class Faction {
    readonly id: number;
    readonly kind: FactionKind;
    readonly targetLiege: number;
    members: number[];
    power: number;
    discontent: number;
    ultimatumSent: boolean;
    claimant: number;
    constructor(id: number, kind: FactionKind, targetLiege: number, claimant?: number);
    /** Add a member (deduplicated). */
    addMember(who: number): void;
    /** Remove a member. */
    removeMember(who: number): void;
    /** Whether the faction is ready to revolt. */
    isReadyToRevolt(): boolean;
    /** Whether the faction can send an ultimatum. */
    canSendUltimatum(): boolean;
}
