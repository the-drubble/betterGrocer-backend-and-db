package com.team6.algorithms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team6.backend.app.entities.Store;
import com.team6.backend.app.repositories.StoreRepository;
import com.team6.backend.app.services.ProfileService;
import com.team6.backend.app.services.ShoppingListBuilderService;
import com.team6.backend.app.util.ShoppingListResponse;
import com.team6.backend.products.services.ProductsSearchService;
import com.team6.backend.products.util.PerStoreProductsSearchDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Works out where to shop for a list, using the signed-in profile's saved stores (up to 5).
 * Prices come from the products database, locations and driving distances from OpenRouteService.
 * The maths itself lives in PlanCalculator.
 */
@Service
public class RoutePlanService {

    private static final Logger log = LoggerFactory.getLogger(RoutePlanService.class);
    private static final long GEOCODE_RETRY_MS = 60L * 60 * 1000;
    private static final Map<String, String> CHAIN_LABELS = Map.of(
            "woolworths", "Woolworths",
            "countdown", "Woolworths",
            "newworld", "New World",
            "paknsave", "PAK'nSAVE");

    /** Thrown for anything the user should be told about; carries the HTTP status to return. */
    public static class PlanException extends RuntimeException {
        public final int status;
        public PlanException(int status, String message) {
            super(message);
            this.status = status;
        }
    }

    private record Geo(double lon, double lat, String label) {}
    private record Mx(double[][] dist, double[][] time) {}
    private record Route(List<double[]> points, double km, long minutes) {}
    private record Located(int storeIndex, Store store, Geo geo) {}

    private final String apiKey;
    private final String orsBase;
    private final ProfileService profileService;
    private final StoreRepository storeRepository;
    private final ShoppingListBuilderService listService;
    private final ProductsSearchService productsSearchService;
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    // small in-memory caches so repeat searches don't spend OpenRouteService quota
    private final Map<String, Geo> homeCache = lru(300);
    private final Map<String, Geo> storeCache = lru(300);
    private final Map<String, Long> storeFailed = lru(300);
    private final Map<String, Mx> matrixCache = lru(200);
    private final Map<String, Route> routeCache = lru(200);

    public RoutePlanService(@Value("${ors.api-key:}") String apiKey,
                            @Value("${ors.base-url:https://api.openrouteservice.org}") String orsBase,
                            ProfileService profileService,
                            StoreRepository storeRepository,
                            ShoppingListBuilderService listService,
                            ProductsSearchService productsSearchService) {
        this.apiKey = apiKey;
        this.orsBase = orsBase;
        this.profileService = profileService;
        this.storeRepository = storeRepository;
        this.listService = listService;
        this.productsSearchService = productsSearchService;
    }

    private static <K, V> Map<K, V> lru(int max) {
        return Collections.synchronizedMap(new LinkedHashMap<K, V>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > max;
            }
        });
    }

    // ---------------------------------------------------------------- the plan

    public Map<String, Object> plan(Long profileId, Long listId, String homeAddress,
                                    String vehicleSize, double fuelPrice) {
        if (apiKey == null || apiKey.isBlank())
            throw new PlanException(503, "Maps are not set up on this server (ORS_API_KEY is missing).");
        if (listId == null)
            throw new PlanException(400, "Choose a list first.");
        if (homeAddress == null || homeAddress.isBlank() || homeAddress.length() > 200)
            throw new PlanException(400, "Enter your home address.");
        if (!(fuelPrice > 0) || fuelPrice > 20)
            throw new PlanException(400, "Enter a fuel price in dollars per litre.");

        // the list (this also checks the profile is allowed to use it)
        ShoppingListResponse list = listService.getUserShoppingList(profileId, listId);
        List<ShoppingListResponse.ShoppingListItemResponse> items = new ArrayList<>(list.shoppingListItemSet());
        items.sort(Comparator.comparing((ShoppingListResponse.ShoppingListItemResponse i) ->
                i.displayName() == null ? "" : i.displayName().toLowerCase()));
        if (items.isEmpty())
            throw new PlanException(400, "Your list has no items yet.");

        // the profile's saved stores
        Set<String> storeIds = profileService.getStoreIdsFromProfileId(profileId);
        if (storeIds.isEmpty())
            throw new PlanException(400, "Choose up to 5 stores on the Home page first.");
        List<Store> stores = new ArrayList<>(storeRepository.findAllById(storeIds));
        stores.sort(Comparator.comparing(Store::getId));
        if (stores.isEmpty())
            throw new PlanException(400, "Your saved stores could not be found.");

        // price of every item at every saved store, in cents (-1 = not stocked)
        int nItems = items.size();
        int[][] price = new int[stores.size()][nItems];
        for (int s = 0; s < stores.size(); s++) {
            for (int k = 0; k < nItems; k++) {
                Optional<PerStoreProductsSearchDetails> d = productsSearchService
                        .getPriceFromGivenStoreAndProductReference(stores.get(s), items.get(k).productReference());
                boolean usable = d.isPresent()
                        && d.get().getPrice() != null
                        && !Boolean.FALSE.equals(d.get().getInStockStatus());
                price[s][k] = usable ? d.get().getPrice() : -1;
            }
        }

        List<Map<String, Object>> skipped = new ArrayList<>();
        List<Integer> candidates = new ArrayList<>();
        for (int s = 0; s < stores.size(); s++) {
            boolean any = false;
            for (int k = 0; k < nItems; k++) if (price[s][k] >= 0) any = true;
            if (any) candidates.add(s);
            else skipped.add(skip(stores.get(s), "does not stock anything on this list"));
        }
        if (candidates.isEmpty())
            throw new PlanException(422, "None of your saved stores stock anything on this list.");

        // where everything is
        Geo home = homeGeo(homeAddress.trim());
        if (home == null)
            throw new PlanException(404, "Could not find that home address. Try adding the suburb and city.");

        List<Located> located = new ArrayList<>();
        for (int s : candidates) {
            Geo g = locate(stores.get(s), home);
            if (g != null) located.add(new Located(s, stores.get(s), g));
            else skipped.add(skip(stores.get(s), "could not be placed on the map"));
        }
        if (located.isEmpty())
            throw new PlanException(422, "None of your saved stores could be placed on the map.");

        // driving distances between home and every store (one matrix call, cached)
        List<double[]> pts = new ArrayList<>();
        pts.add(new double[]{home.lon(), home.lat()});
        for (Located l : located) pts.add(new double[]{l.geo().lon(), l.geo().lat()});
        Mx mx = matrix(pts);

        List<Integer> okPos = new ArrayList<>();
        for (int j = 0; j < located.size(); j++) {
            double there = mx.dist()[0][j + 1];
            double back = mx.dist()[j + 1][0];
            if (!Double.isNaN(there) && !Double.isNaN(back)) okPos.add(j);
            else skipped.add(skip(located.get(j).store(), "has no driving route from your home"));
        }
        if (okPos.isEmpty())
            throw new PlanException(422, "No driving route was found to your saved stores.");

        // re-index to the stores we can actually use: o = 0..m-1, point o + 1 in dist2
        int m = okPos.size();
        List<Located> ok = new ArrayList<>();
        for (int j : okPos) ok.add(located.get(j));
        double[][] dist2 = new double[m + 1][m + 1];
        for (int a = 0; a <= m; a++) {
            int ia = a == 0 ? 0 : okPos.get(a - 1) + 1;
            for (int b = 0; b <= m; b++) {
                int ib = b == 0 ? 0 : okPos.get(b - 1) + 1;
                dist2[a][b] = mx.dist()[ia][ib];
            }
        }

        // only plan for items at least one usable store stocks; report the rest
        List<Integer> reqItems = new ArrayList<>();
        List<String> unavailable = new ArrayList<>();
        for (int k = 0; k < nItems; k++) {
            boolean any = false;
            for (Located l : ok) if (price[l.storeIndex()][k] >= 0) any = true;
            if (any) reqItems.add(k);
            else unavailable.add(items.get(k).displayName());
        }
        if (reqItems.isEmpty())
            throw new PlanException(422, "None of the reachable stores stock anything on this list.");
        int n = reqItems.size();
        int[][] priceReq = new int[m][n];
        int[] qtyReq = new int[n];
        String[] nameReq = new String[n];
        for (int r = 0; r < n; r++) {
            ShoppingListResponse.ShoppingListItemResponse it = items.get(reqItems.get(r));
            qtyReq[r] = it.quantity() == null ? 1 : Math.max(1, it.quantity());
            nameReq[r] = it.displayName();
            for (int o = 0; o < m; o++) priceReq[o][r] = price[ok.get(o).storeIndex()][reqItems.get(r)];
        }

        double costPerKm = consumption(vehicleSize) / 100.0 * fuelPrice;

        // one-stop-shop options and the overall cheapest plan
        List<Map<String, Object>> rows = new ArrayList<>();
        PlanCalculator.Plan bestOne = null;
        int bestOneStore = -1;
        for (int o = 0; o < m; o++) {
            Located l = ok.get(o);
            PlanCalculator.Plan p = PlanCalculator.plan(List.of(o), priceReq, qtyReq, dist2, costPerKm);
            double oneWay = dist2[0][o + 1] / 1000.0;
            double roundTrip = (dist2[0][o + 1] + dist2[o + 1][0]) / 1000.0;
            List<String> missing = new ArrayList<>();
            for (int r = 0; r < n; r++) if (priceReq[o][r] < 0) missing.add(nameReq[r]);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("storeId", l.store().getId());
            row.put("chain", chain(l.store()));
            row.put("name", l.store().getName());
            row.put("address", l.store().getLocation());
            row.put("locatedAs", l.geo().label());
            row.put("lat", l.geo().lat());
            row.put("lon", l.geo().lon());
            row.put("oneWayKm", r1(oneWay));
            row.put("minutes", Math.round(mx.time()[0][okPos.get(o) + 1] / 60.0));
            row.put("roundTripKm", r1(roundTrip));
            row.put("fuel", r2(roundTrip * costPerKm));
            row.put("complete", p != null);
            row.put("groceries", p == null ? null : r2(p.groceriesCents / 100.0));
            row.put("total", p == null ? null : r2(p.total));
            row.put("missing", missing);
            rows.add(row);

            if (p != null && (bestOne == null || p.total < bestOne.total)) {
                bestOne = p;
                bestOneStore = o;
            }
        }

        List<Integer> allOk = new ArrayList<>();
        for (int o = 0; o < m; o++) allOk.add(o);
        PlanCalculator.Plan best = PlanCalculator.cheapest(allOk, priceReq, qtyReq, dist2, costPerKm);
        if (best == null)
            throw new PlanException(422, "Could not work out a plan for this list.");

        // road routes for the maps
        List<double[]> oneStopRoute = null;
        Map<String, Object> oneStop = null;
        if (bestOne != null) {
            Located l = ok.get(bestOneStore);
            Route rt = safeRoute(List.of(pts.get(0), pts.get(okPos.get(bestOneStore) + 1)));
            oneStopRoute = rt == null ? null : rt.points();
            oneStop = new LinkedHashMap<>();
            oneStop.put("storeId", l.store().getId());
            oneStop.put("chain", chain(l.store()));
            oneStop.put("name", l.store().getName());
            oneStop.put("groceries", r2(bestOne.groceriesCents / 100.0));
            oneStop.put("fuel", r2(bestOne.fuel));
            oneStop.put("total", r2(bestOne.total));
            oneStop.put("route", oneStopRoute);
        }

        List<Map<String, Object>> stops = new ArrayList<>();
        List<double[]> tourPts = new ArrayList<>();
        tourPts.add(pts.get(0));
        for (int o : best.stops) {
            Located l = ok.get(o);
            tourPts.add(pts.get(okPos.get(o) + 1));
            List<Map<String, Object>> bought = new ArrayList<>();
            long subtotal = 0;
            for (int r = 0; r < n; r++) {
                if (best.buyAt[r] != o) continue;
                long line = (long) priceReq[o][r] * qtyReq[r];
                subtotal += line;
                Map<String, Object> b = new LinkedHashMap<>();
                b.put("name", nameReq[r]);
                b.put("quantity", qtyReq[r]);
                b.put("unitPrice", r2(priceReq[o][r] / 100.0));
                b.put("lineTotal", r2(line / 100.0));
                bought.add(b);
            }
            Map<String, Object> stop = new LinkedHashMap<>();
            stop.put("storeId", l.store().getId());
            stop.put("chain", chain(l.store()));
            stop.put("name", l.store().getName());
            stop.put("lat", l.geo().lat());
            stop.put("lon", l.geo().lon());
            stop.put("items", bought);
            stop.put("subtotal", r2(subtotal / 100.0));
            stops.add(stop);
        }

        List<double[]> cheapestRoute = null;
        if (stops.size() > 1) {
            tourPts.add(pts.get(0));
            Route rt = safeRoute(tourPts);
            cheapestRoute = rt == null ? null : rt.points();
        } else {
            cheapestRoute = oneStopRoute;
        }

        double savings = bestOne == null ? 0 : bestOne.total - best.total;
        Map<String, Object> cheapest = new LinkedHashMap<>();
        cheapest.put("stops", stops);
        cheapest.put("singleStore", stops.size() == 1);
        cheapest.put("groceries", r2(best.groceriesCents / 100.0));
        cheapest.put("fuel", r2(best.fuel));
        cheapest.put("km", r1(best.routeMetres / 1000.0));
        cheapest.put("total", r2(best.total));
        cheapest.put("savingsVsOneStop", savings > 0.005 ? r2(savings) : 0.0);
        cheapest.put("route", cheapestRoute);

        Map<String, Object> out = new LinkedHashMap<>();
        Map<String, Object> homeOut = new LinkedHashMap<>();
        homeOut.put("lat", home.lat());
        homeOut.put("lon", home.lon());
        homeOut.put("label", home.label());
        out.put("home", homeOut);
        out.put("stores", rows);
        out.put("skippedStores", skipped);
        out.put("unavailableItems", unavailable);
        out.put("oneStop", oneStop);
        out.put("cheapest", cheapest);
        out.put("itemCount", nItems);
        return out;
    }

    // ---------------------------------------------------------------- helpers

    private static String norm(String s) {
        return s == null ? "" : s.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private static String chain(Store store) {
        return CHAIN_LABELS.getOrDefault(norm(store.getFranchiseName()), store.getFranchiseName());
    }

    private static Map<String, Object> skip(Store store, String reason) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("storeId", store.getId());
        m.put("chain", chain(store));
        m.put("name", store.getName());
        m.put("reason", reason);
        return m;
    }

    private static double consumption(String size) {
        if ("SMALL".equalsIgnoreCase(size)) return 6.5;
        if ("LARGE".equalsIgnoreCase(size)) return 13.0;
        return 9.0;
    }

    private static double r1(double v) { return Math.round(v * 10.0) / 10.0; }
    private static double r2(double v) { return Math.round(v * 100.0) / 100.0; }

    // ---------------------------------------------------------------- locations

    private Geo homeGeo(String address) {
        String key = address.toLowerCase().replaceAll("\\s+", " ");
        Geo cached = homeCache.get(key);
        if (cached != null) return cached;
        Geo g = geocode(address, null, null);
        if (g != null) homeCache.put(key, g);
        return g;
    }

    private Geo locate(Store store, Geo home) {
        Geo cached = storeCache.get(store.getId());
        if (cached != null) return cached;
        Long failedAt = storeFailed.get(store.getId());
        if (failedAt != null && System.currentTimeMillis() - failedAt < GEOCODE_RETRY_MS) return null;

        Geo g = null;
        if (store.getLocation() != null && !store.getLocation().isBlank())
            g = geocode(store.getLocation(), home.lon(), home.lat());
        if (g == null && store.getName() != null && !store.getName().isBlank())
            g = geocode(store.getName(), home.lon(), home.lat());

        if (g != null) storeCache.put(store.getId(), g);
        else storeFailed.put(store.getId(), System.currentTimeMillis());
        return g;
    }

    private Geo geocode(String text, Double focusLon, Double focusLat) {
        String url = orsBase + "/geocode/search"
                + "?api_key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
                + "&text=" + URLEncoder.encode(text, StandardCharsets.UTF_8)
                + "&boundary.country=NZ&size=1"
                + (focusLon != null ? "&focus.point.lon=" + focusLon + "&focus.point.lat=" + focusLat : "");
        JsonNode features = call(HttpRequest.newBuilder().uri(URI.create(url)).GET().build()).path("features");
        if (!features.isArray() || features.size() == 0) return null;
        JsonNode first = features.get(0);
        JsonNode c = first.path("geometry").path("coordinates");
        if (!c.isArray() || c.size() < 2) return null;
        return new Geo(c.get(0).asDouble(), c.get(1).asDouble(), first.path("properties").path("label").asText(""));
    }

    // ---------------------------------------------------------------- routing

    private Mx matrix(List<double[]> pts) {
        String key = pointsKey(pts);
        Mx cached = matrixCache.get(key);
        if (cached != null) return cached;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("locations", pts);
        body.put("metrics", List.of("distance", "duration"));
        JsonNode root = orsPost(orsBase + "/v2/matrix/driving-car", body);
        Mx mx = new Mx(grid(root.path("distances"), pts.size()), grid(root.path("durations"), pts.size()));
        matrixCache.put(key, mx);
        return mx;
    }

    private static double[][] grid(JsonNode node, int n) {
        double[][] g = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                JsonNode v = node.path(i).path(j);
                g[i][j] = (v.isMissingNode() || v.isNull()) ? Double.NaN : v.asDouble();
            }
        }
        return g;
    }

    /** Road route through the points in order. A failed route only costs the map line, never the plan. */
    private Route safeRoute(List<double[]> pts) {
        try {
            return route(pts);
        } catch (RuntimeException e) {
            log.warn("Could not get a route line: {}", e.getMessage());
            return null;
        }
    }

    private Route route(List<double[]> pts) {
        String key = pointsKey(pts);
        Route cached = routeCache.get(key);
        if (cached != null) return cached;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("coordinates", pts);
        JsonNode root = orsPost(orsBase + "/v2/directions/driving-car/geojson", body);
        JsonNode f = root.path("features").path(0);
        List<double[]> line = new ArrayList<>();
        for (JsonNode p : f.path("geometry").path("coordinates"))
            line.add(new double[]{p.get(1).asDouble(), p.get(0).asDouble()});
        if (line.isEmpty()) return null;

        JsonNode sum = f.path("properties").path("summary");
        Route rt = new Route(line, r1(sum.path("distance").asDouble() / 1000.0),
                Math.round(sum.path("duration").asDouble() / 60.0));
        routeCache.put(key, rt);
        return rt;
    }

    private static String pointsKey(List<double[]> pts) {
        StringBuilder k = new StringBuilder();
        for (double[] p : pts) k.append(String.format(Locale.ROOT, "%.5f,%.5f;", p[0], p[1]));
        return k.toString();
    }

    private JsonNode orsPost(String url, Object body) {
        String json;
        try {
            json = mapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new PlanException(500, "Could not build the routing request.");
        }
        return call(HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build());
    }

    /** One OpenRouteService call; turns failures into messages a person can act on. */
    private JsonNode call(HttpRequest request) {
        HttpResponse<String> res;
        try {
            res = http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PlanException(502, "The routing request was interrupted.");
        } catch (Exception e) {
            log.warn("OpenRouteService request failed: {}", e.toString());
            throw new PlanException(502, "Could not reach the routing service.");
        }

        int status = res.statusCode();
        String body = res.body() == null ? "" : res.body();
        if (status == 200) {
            try {
                return mapper.readTree(body);
            } catch (Exception e) {
                throw new PlanException(502, "The routing service sent an answer that could not be read.");
            }
        }

        log.warn("OpenRouteService returned {}: {}", status, body.length() > 200 ? body.substring(0, 200) : body);
        if (status == 403 && body.toLowerCase().contains("quota"))
            throw new PlanException(503, "The routing service has reached its daily limit. Please try again later.");
        if (status == 429)
            throw new PlanException(503, "The routing service is busy. Please try again in a minute.");
        if (status == 401 || status == 403)
            throw new PlanException(502, "The routing service rejected this server's API key.");
        throw new PlanException(502, "The routing service returned an error (" + status + ").");
    }
}
