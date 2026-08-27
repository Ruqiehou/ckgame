import { County } from './County.js';
/** 地图：省份集合与邻接关系，支持 BFS 路径查找。 */
export declare class MapGraph {
    private _counties;
    private countyByKey;
    insert(county: County): void;
    get(countyId: number): County | undefined;
    getByKey(key: string): County | undefined;
    connect(a: number, b: number): void;
    path(from: number, to: number): number[] | null;
    [Symbol.iterator](): Iterator<County>;
    /** 全部省份（无序）。 */
    list(): County[];
    /** 省份 Map 访问器。 */
    get counties(): Map<number, County>;
}
