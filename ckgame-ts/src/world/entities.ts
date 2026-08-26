import { Gender } from '../core/Gender';
import { TitleTier } from '../core/TitleTier';
import { GameDate } from '../core/calendar/GameDate';
import { RealmLaw, SuccessionLaw, CrownAuthority, GenderLaw } from './entities';

export interface County {
  id: number;
  name: string;
  terrain: string;
  development: number;
  control: number;
  prosperity: number;
  culture: number;
  faith: number;
  ownerTitle: number;
  holder: number;
  fortLevel: number;
  buildings: string[];
  levies: number;
  tax: number;
  neighbors: number[];
  tradeRouteProtected?: boolean;
  tradeRouteProtectionLevel?: number;
  tradeRouteMaintenanceCost?: number;
  hasPort?: boolean;
  portLevel?: number;
  portIncome?: number;
  tradeRouteMaintenanceLevel?: number;
  tradeRouteUpgradeCost?: number;
}

export interface Title {
  id: number;
  name: string;
  tier: TitleTier;
  adjective: string;
  holder: number;
  deJureLiege: number;
  deFactoLiege: number;
  deJureVassals: number[];
  deFactoVassals: number[];
  capital: number;
  counties: number[];
  creationCost: number;
  destroyable: boolean;
  realmLaw: RealmLaw;
}

export interface RealmLaw {
  succession: SuccessionLaw;
  crownAuthority: CrownAuthority;
  genderLaw: GenderLaw;
  partitionEnabled: boolean;
}

export enum SuccessionLaw {
  PRIMOGENITURE = 'primogeniture',
  CONFEDERATE_PARTITION = 'confederatePartition',
  ELECTIVE = 'elective',
  HOUSE_SENIORITY = 'houseSeniority',
  ULTIMOGENITURE = 'ultimogeniture'
}

export enum CrownAuthority {
  AUTONOMOUS = 'autonomous',
  LIMITED = 'limited',
  HIGH = 'high',
  ABSOLUTE = 'absolute'
}

export enum GenderLaw {
  AGNATIC = 'agnatic',
  AGNATIC_COGNATIC = 'agnaticCognatic',
  ABSOLUTE_COGNATIC = 'absoluteCognatic',
  ENATIC = 'enatic'
}

export class MapGraph {
  private counties: Map<number, County> = new Map();
  private countyByKey: Map<string, County> = new Map();

  insert(county: County): void {
    this.counties.set(county.id, county);
    this.countyByKey.set(county.name.toLowerCase(), county);
  }

  get(countyId: number): County | undefined {
    return this.counties.get(countyId);
  }

  getByKey(key: string): County | undefined {
    return this.countyByKey.get(key.toLowerCase());
  }

  connect(a: number, b: number): void {
    const ca = this.counties.get(a);
    const cb = this.counties.get(b);
    if (ca && !ca.neighbors.includes(b)) ca.neighbors.push(b);
    if (cb && !cb.neighbors.includes(a)) cb.neighbors.push(a);
  }

  path(from: number, to: number): number[] | undefined {
    if (from === to) return [from];
    const visited = new Set<number>();
    const queue: number[] = [from];
    const parent = new Map<number, number>();
    visited.add(from);

    while (queue.length > 0) {
      const cur = queue.shift()!;
      const county = this.counties.get(cur);
      if (!county) continue;
      for (const n of county.neighbors) {
        if (visited.has(n)) continue;
        visited.add(n);
        parent.set(n, cur);
        if (n === to) {
          const path: number[] = [to];
          let p = n;
          while (parent.has(p)) {
            path.unshift(parent.get(p)!);
            if (parent.get(p)! === from) break;
            p = parent.get(p)!;
          }
          return path;
        }
        queue.push(n);
      }
    }
    return undefined;
  }

  list(): County[] {
    return Array.from(this.counties.values());
  }
}

export class RealmLaw {
  constructor(
    public succession: SuccessionLaw,
    public crownAuthority: CrownAuthority,
    public genderLaw: GenderLaw,
    public partitionEnabled: boolean
  ) {}

  static feudalDefault(): RealmLaw {
    return new RealmLaw(SuccessionLaw.PRIMOGENITURE, CrownAuthority.LIMITED, GenderLaw.AGNATIC_COGNATIC, false);
  }

  pickHeir(children: number[], dynastyMembers: number[]): number | null {
    // 简化实现：优先男性长子
    const sons = children.filter(id => /* gender check */);
    if (sons.length > 0) return sons[0];
    const dynastySons = dynastyMembers.filter(id => /* gender check */);
    if (dynastySons.length > 0) return dynastySons[0];
    return null;
  }
}
