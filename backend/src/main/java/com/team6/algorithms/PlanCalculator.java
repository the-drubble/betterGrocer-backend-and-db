package com.team6.algorithms;

import java.util.ArrayList;
import java.util.List;

/**
 * The shopping-plan maths: which stores to visit, what to buy where, and what it costs.
 * Plain Java with no Spring and no network, so it can be tested on its own.
 *
 * Index conventions:
 *  - price[s][k]  = unit price in cents of item k at store s, or -1 if that store doesn't stock it
 *  - qty[k]       = how many of item k are wanted
 *  - dist[a][b]   = driving metres from point a to point b, NaN if there is no route.
 *                   Point 0 is home, store s is point s + 1.
 */
public final class PlanCalculator {

    private PlanCalculator() {}

    /** One way of doing the whole shop. */
    public static final class Plan {
        /** store indexes in driving order (home -> stops -> home) */
        public final List<Integer> stops;
        /** for each item, the store index it is bought at */
        public final int[] buyAt;
        public final long groceriesCents;
        public final double routeMetres;
        public final double fuel;
        public final double total;

        Plan(List<Integer> stops, int[] buyAt, long groceriesCents, double routeMetres, double fuel) {
            this.stops = stops;
            this.buyAt = buyAt;
            this.groceriesCents = groceriesCents;
            this.routeMetres = routeMetres;
            this.fuel = fuel;
            this.total = groceriesCents / 100.0 + fuel;
        }
    }

    /**
     * The best way to buy every item using only the given stores.
     * Each item is bought at the cheapest of those stores (ties go to the nearer store),
     * and the stores are visited in the shortest driving order.
     * Returns null if the stores can't cover every item, if any store would be visited
     * for nothing, or if there is no route.
     */
    public static Plan plan(List<Integer> chosen, int[][] price, int[] qty, double[][] dist, double costPerKm) {
        int n = qty.length;
        int[] buyAt = new int[n];
        int[] count = new int[price.length];
        long groceries = 0;

        for (int k = 0; k < n; k++) {
            int best = -1;
            for (int s : chosen) {
                int c = price[s][k];
                if (c < 0) continue;
                if (best == -1
                        || c < price[best][k]
                        || (c == price[best][k] && dist[0][s + 1] < dist[0][best + 1])) {
                    best = s;
                }
            }
            if (best == -1) return null;
            buyAt[k] = best;
            count[best]++;
            groceries += (long) price[best][k] * qty[k];
        }
        for (int s : chosen) if (count[s] == 0) return null;

        List<List<Integer>> orders = new ArrayList<>();
        permutations(new ArrayList<>(chosen), new ArrayList<>(), orders);

        double bestMetres = Double.POSITIVE_INFINITY;
        List<Integer> bestOrder = null;
        for (List<Integer> order : orders) {
            double metres = 0;
            int prev = 0;
            boolean valid = true;
            for (int s : order) {
                double d = dist[prev][s + 1];
                if (Double.isNaN(d)) { valid = false; break; }
                metres += d;
                prev = s + 1;
            }
            if (valid) {
                double back = dist[prev][0];
                if (Double.isNaN(back)) valid = false; else metres += back;
            }
            if (valid && metres < bestMetres) {
                bestMetres = metres;
                bestOrder = order;
            }
        }
        if (bestOrder == null) return null;

        double fuel = bestMetres / 1000.0 * costPerKm;
        return new Plan(bestOrder, buyAt, groceries, bestMetres, fuel);
    }

    /** The cheapest plan over every combination of the given stores. Fewer stops win ties. */
    public static Plan cheapest(List<Integer> stores, int[][] price, int[] qty, double[][] dist, double costPerKm) {
        int m = stores.size();
        Plan best = null;
        for (int size = 1; size <= m; size++) {
            for (int mask = 1; mask < (1 << m); mask++) {
                if (Integer.bitCount(mask) != size) continue;
                List<Integer> chosen = new ArrayList<>();
                for (int b = 0; b < m; b++) if ((mask & (1 << b)) != 0) chosen.add(stores.get(b));
                Plan p = plan(chosen, price, qty, dist, costPerKm);
                if (p != null && (best == null || p.total < best.total - 0.005)) best = p;
            }
        }
        return best;
    }

    private static void permutations(List<Integer> rest, List<Integer> current, List<List<Integer>> out) {
        if (rest.isEmpty()) {
            out.add(new ArrayList<>(current));
            return;
        }
        for (int i = 0; i < rest.size(); i++) {
            List<Integer> next = new ArrayList<>(rest);
            Integer x = next.remove(i);
            current.add(x);
            permutations(next, current, out);
            current.remove(current.size() - 1);
        }
    }
}
