/** 地图：省份集合与邻接关系，支持 BFS 路径查找。 */
export class MapGraph {
    _counties = new Map();
    countyByKey = new Map();
    insert(county) {
        this._counties.set(county.id, county);
        this.countyByKey.set(county.name.toLowerCase(), county);
    }
    get(countyId) {
        return this._counties.get(countyId);
    }
    getByKey(key) {
        return this.countyByKey.get(key.toLowerCase());
    }
    connect(a, b) {
        const ca = this._counties.get(a);
        const cb = this._counties.get(b);
        if (ca !== undefined && !ca.neighbors.includes(b)) {
            ca.neighbors.push(b);
        }
        if (cb !== undefined && !cb.neighbors.includes(a)) {
            cb.neighbors.push(a);
        }
    }
    path(from, to) {
        if (from === to) {
            return [from];
        }
        const visited = new Set();
        visited.add(from);
        const queue = [from];
        const parent = new Map();
        while (queue.length > 0) {
            const cur = queue.shift();
            const county = this._counties.get(cur);
            if (county === undefined)
                continue;
            for (const n of county.neighbors) {
                if (visited.has(n))
                    continue;
                visited.add(n);
                parent.set(n, cur);
                if (n === to) {
                    const result = [to];
                    let p = n;
                    while (parent.has(p)) {
                        const prev = parent.get(p);
                        result.push(prev);
                        if (prev === from)
                            break;
                        p = prev;
                    }
                    result.reverse();
                    return result;
                }
                queue.push(n);
            }
        }
        return null;
    }
    [Symbol.iterator]() {
        return this._counties.values();
    }
    /** 全部省份（无序）。 */
    list() {
        return Array.from(this._counties.values());
    }
    /** 省份 Map 访问器。 */
    get counties() {
        return this._counties;
    }
}
//# sourceMappingURL=MapGraph.js.map