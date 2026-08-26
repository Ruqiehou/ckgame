import { MapGraph } from './entities';

export class MapGraph {
  private counties: Map<number, any> = new Map();
  private countyByKey: Map<string, any> = new Map();

  insert(county: any): void {
    this.counties.set(county.id, county);
    this.countyByKey.set(county.name.toLowerCase(), county);
  }

  get(countyId: number): any {
    return this.counties.get(countyId);
  }

  getByKey(key: string): any {
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

  list(): any[] {
    return Array.from(this.counties.values());
  }
}
