package com.ckgame.world;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 地图：省份集合与邻接关系，支持 BFS 路径查找。 */
public final class MapGraph {
    private final Map<Integer, County> counties = new HashMap<>();
    private final Map<String, County> countyByKey = new HashMap<>();

    public void insert(County county) {
        counties.put(county.id, county);
        countyByKey.put(county.name.toLowerCase(), county);
    }

    public County get(int countyId) {
        return counties.get(countyId);
    }

    public County getByKey(String key) {
        return countyByKey.get(key.toLowerCase());
    }

    public void connect(int a, int b) {
        County ca = counties.get(a);
        County cb = counties.get(b);
        if (ca != null && !ca.neighbors.contains(b)) {
            ca.neighbors.add(b);
        }
        if (cb != null && !cb.neighbors.contains(a)) {
            cb.neighbors.add(a);
        }
    }

    public List<Integer> path(int from, int to) {
        if (from == to) {
            return List.of(from);
        }
        Set<Integer> visited = new HashSet<>();
        visited.add(from);
        Deque<Integer> queue = new ArrayDeque<>();
        queue.add(from);
        Map<Integer, Integer> parent = new HashMap<>();
        while (!queue.isEmpty()) {
            int cur = queue.poll();
            County county = counties.get(cur);
            if (county == null) continue;
            for (int n : county.neighbors) {
                if (visited.contains(n)) continue;
                visited.add(n);
                parent.put(n, cur);
                if (n == to) {
                    List<Integer> path = new ArrayList<>();
                    path.add(to);
                    Integer p = n;
                    while (parent.containsKey(p)) {
                        int prev = parent.get(p);
                        path.add(prev);
                        if (prev == from) break;
                        p = prev;
                    }
                    java.util.Collections.reverse(path);
                    return path;
                }
                queue.add(n);
            }
        }
        return null;
    }

    public Iterator<County> iterator() {
        return counties.values().iterator();
    }

    /** 全部省份（无序）。 */
    public List<County> list() {
        return new ArrayList<>(counties.values());
    }

    public Map<Integer, County> counties() {
        return counties;
    }
}
