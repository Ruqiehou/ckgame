import { Character } from './Character';
import { Dynasty } from './entities';
import { Gender } from '../core/Gender';
import { GameDate } from '../core/calendar/GameDate';

export class World {
  date: GameDate;
  characters: Map<number, Character> = new Map();
  dynasties: Map<number, Dynasty> = new Map();
  titles: Map<number, any> = new Map(); // 待扩展
  map: MapGraph = new MapGraph();
  traits: Trait[] = [];
  log: string[] = [];
  nextChar = 1;
  nextDynasty = 1;
  nextTitle = 1;
  nextCounty = 1;

  constructor(date: GameDate) {
    this.date = date;
  }

  createDynasty(name: string): number {
    const did = this.nextDynasty++;
    this.dynasties.set(did, { id: did, name, head: 0, founder: 0, members: [], colorR: 128, colorG: 128, colorB: 128, motto: '', prestige: 0 });
    return did;
  }

  createCharacter(name: string, dynasty: number, gender: Gender, birth: GameDate): number {
    const cid = this.nextChar++;
    this.characters.set(cid, {
      id: cid,
      name,
      dynasty,
      gender,
      birth,
      life: 'ALIVE',
      baseAttrs: new AttributeSet(),
      traits: [],
      culture: 0,
      faith: 0,
      gold: 50,
      prestige: 50,
      piety: 50,
      stress: 0,
      health: 5,
      fertility: 0.5,
      father: 0,
      mother: 0,
      spouses: [],
      children: [],
      heldTitles: [],
      primaryTitle: 0,
      isRuler: false,
      employer: 0,
      opinionCache: new Map(),
      level: 1,
      xp: 0
    });
    const d = this.dynasties.get(dynasty);
    if (d) {
      d.members.push(cid);
      if (d.head === 0) d.head = cid;
    }
    return cid;
  }

  createTitle(name: string, tier: any): number {
    const tid = this.nextTitle++;
    this.titles.set(tid, { id: tid, name, tier, adjective: '', holder: 0, deJureLiege: 0, deFactoLiege: 0, deJureVassals: [], deFactoVassals: [], capital: 0, counties: [], creationCost: 0, destroyable: false, realmLaw: new RealmLaw(SuccessionLaw.PRIMOGENITURE, CrownAuthority.LIMITED, GenderLaw.AGNATIC_COGNATIC, false) });
    return tid;
  }

  createCounty(name: string, terrain: string): number {
    const cid = this.nextCounty++;
    this.map.insert({ id: cid, name, terrain, development: 10, control: 100, prosperity: 50, culture: 0, faith: 0, ownerTitle: 0, holder: 0, fortLevel: 1, buildings: [], levies: 200, tax: 1, neighbors: [] });
    return cid;
  }

  marry(a: number, b: number): boolean {
    const ca = this.characters.get(a);
    const cb = this.characters.get(b);
    if (!ca || !cb || ca.gender === cb.gender) return false;
    ca.spouses.push(b);
    cb.spouses.push(a);
    return true;
  }

  grantTitle(titleId: number, holder: number): boolean {
    const t = this.titles.get(titleId);
    const h = this.characters.get(holder);
    if (!t || !h) return false;
    t.holder = holder;
    h.heldTitles.push(titleId);
    h.isRuler = true;
    h.primaryTitle = titleId;
    return true;
  }

  setParents(child: number, father: number, mother: number): void {
    const c = this.characters.get(child);
    if (c) {
      c.father = father;
      c.mother = mother;
      if (father) this.characters.get(father)?.children.push(child);
      if (mother) this.characters.get(mother)?.children.push(child);
    }
  }

  findHeir(ruler: number, law: RealmLaw): number | null {
    const c = this.characters.get(ruler);
    if (!c) return null;
    const children = c.children.filter(id => this.characters.get(id)?.life === 'ALIVE');
    const dynastyMembers = Array.from(this.dynasties.values())
      .find(d => d.id === c.dynasty)?.members.filter(id => this.characters.get(id)?.life === 'ALIVE') || [];

    return law.pickHeir(children, dynastyMembers);
  }
}
