import { NONE_ID } from '../core/Constants.js';
import { Army } from './Army.js';
import { ArmyStatus } from './ArmyStatus.js';
import { UnitType } from './UnitType.js';
import { War } from './War.js';
import { WarParticipant } from './WarParticipant.js';
/**
 * War manager: manages all wars and armies.
 */
export class WarManager {
    wars = new Map();
    armies = new Map();
    nextWar = 1;
    nextArmy = 1;
    world;
    diplomacy;
    constructor(world, diplomacy) {
        this.world = world ?? null;
        this.diplomacy = diplomacy ?? null;
    }
    getWorld() {
        return this.world;
    }
    getDiplomacy() {
        return this.diplomacy;
    }
    declareWar(cb, attacker, defender, date, name, targetTitle = NONE_ID) {
        const wid = this.nextWar++;
        const war = new War(wid, name, cb, attacker, defender, date);
        war.targetTitle = targetTitle;
        war.participants.push(new WarParticipant(attacker, true, date));
        war.participants.push(new WarParticipant(defender, false, date));
        this.wars.set(wid, war);
        return wid;
    }
    /** Get a war by id. */
    war(wid) {
        return this.wars.get(wid);
    }
    /** All active wars. */
    activeWars() {
        const out = [];
        for (const w of this.wars.values()) {
            if (w.active)
                out.push(w);
        }
        return out;
    }
    /** End a war with the given result. */
    endWar(wid, result) {
        const w = this.wars.get(wid);
        if (!w)
            return;
        w.active = false;
        w.result = result;
    }
    raiseArmy(owner, location, levies, name = '') {
        const aid = this.nextArmy++;
        const armyName = (!name || name.trim() === '') ? `军团#${aid}` : name;
        const army = new Army(aid, owner, armyName, location, owner);
        army.addMen(UnitType.LEVIES, levies);
        this.armies.set(aid, army);
        return aid;
    }
    /** Get an army by id. */
    army(aid) {
        return this.armies.get(aid);
    }
    /** All active army ids for a given owner. */
    armiesOf(owner) {
        const out = [];
        for (const a of this.armies.values()) {
            if (a.owner === owner && a.isActive())
                out.push(a.id);
        }
        return out;
    }
    /** Total men across all active armies for a given owner. */
    totalMenOf(owner) {
        let total = 0;
        for (const a of this.armies.values()) {
            if (a.owner === owner && a.isActive())
                total += a.totalMen();
        }
        return total;
    }
    /** Tick movement for all moving armies. */
    tickMovement(moveChanceOf) {
        for (const army of this.armies.values()) {
            if (army.status === ArmyStatus.MOVING) {
                const chance = moveChanceOf ? moveChanceOf(army) : 1.0;
                army.advanceMove(chance);
            }
        }
    }
    /** Disband empty armies. */
    disbandEmpty() {
        for (const army of this.armies.values()) {
            if (army.totalMen() <= 0) {
                army.status = ArmyStatus.DISBANDED;
            }
        }
    }
    /** Set path for an army. */
    move(armyId, path) {
        const army = this.armies.get(armyId);
        if (army)
            army.setPath(path);
    }
    /** Disband an army. */
    disband(armyId) {
        const army = this.armies.get(armyId);
        if (army)
            army.status = ArmyStatus.DISBANDED;
    }
    /** Daily tick: movement + cleanup. */
    tick() {
        this.tickMovement();
        this.disbandEmpty();
    }
}
//# sourceMappingURL=WarManager.js.map