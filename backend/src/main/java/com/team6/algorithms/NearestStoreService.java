package com.team6.algorithms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class NearestStoreService {

    // display name, search text, keys used to recognise the chain in results
    private static final String[][] CHAINS = {
        {"New World", "New World", "newworld"},
        {"Woolworths", "Woolworths|Countdown", "woolworths", "countdown"},
        {"PAK'nSAVE", "PAKnSAVE", "paknsave"}
    };

    private final String apiKey;
    private final MapsService maps;
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public NearestStoreService(@Value("${ors.api-key}") String apiKey, MapsService maps) {
        this.apiKey = apiKey;
        this.maps = maps;
    }

    private record Cand(String chain, String name, String address, double lon, double lat) {}

    private static String norm(String s) {
        return s == null ? "" : s.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private double consumption(String size) {
        if ("SMALL".equalsIgnoreCase(size)) return 6.5;
        if ("LARGE".equalsIgnoreCase(size)) return 13.0;
        return 9.0;
    }

    private JsonNode get(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) throw new RuntimeException("ORS " + res.statusCode() + ": " + res.body());
        return mapper.readTree(res.body());
    }

    private List<Cand> findCandidates(String[] chain, MapsService.Coordinates home) throws Exception {
        List<Cand> out = new ArrayList<>();
        for (String term : chain[1].split("\\|")) {
            String url = "https://api.openrouteservice.org/geocode/search"
                    + "?api_key=" + apiKey
                    + "&text=" + URLEncoder.encode(term, StandardCharsets.UTF_8)
                    + "&focus.point.lon=" + home.lon + "&focus.point.lat=" + home.lat
                    + "&boundary.country=NZ&size=10";
            for (JsonNode f : get(url).path("features")) {
                String label = f.path("properties").path("label").asText();
                String name = f.path("properties").path("name").asText();
                String n = norm(label);
                boolean match = false;
                for (int i = 2; i < chain.length; i++) if (n.contains(chain[i])) match = true;
                if (!match) continue;
                JsonNode c = f.path("geometry").path("coordinates");
                out.add(new Cand(chain[0], name, label, c.get(0).asDouble(), c.get(1).asDouble()));
            }
        }
        return out;
    }

    public Map<String, Object> nearest(String homeAddress, String vehicleSize, double fuelPrice) throws Exception {
        MapsService.Coordinates home = maps.geocode(homeAddress);
        if (home == null) throw new IllegalArgumentException("Could not find that home address");

        List<Cand> cands = new ArrayList<>();
        for (String[] chain : CHAINS) cands.addAll(findCandidates(chain, home));
        if (cands.isEmpty()) throw new IllegalStateException("No stores found near that address");

        // one matrix call: home -> every candidate
        List<double[]> locs = new ArrayList<>();
        locs.add(new double[]{home.lon, home.lat});
        for (Cand c : cands) locs.add(new double[]{c.lon(), c.lat()});
        List<Integer> dests = new ArrayList<>();
        for (int i = 1; i <= cands.size(); i++) dests.add(i);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("locations", locs);
        body.put("sources", List.of(0));
        body.put("destinations", dests);
        body.put("metrics", List.of("distance", "duration"));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openrouteservice.org/v2/matrix/driving-car"))
                .header("Authorization", apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) throw new RuntimeException("ORS matrix " + res.statusCode() + ": " + res.body());
        JsonNode root = mapper.readTree(res.body());

        // best (shortest driving distance) candidate per chain
        Map<String, Map<String, Object>> best = new LinkedHashMap<>();
        for (int i = 0; i < cands.size(); i++) {
            JsonNode d = root.path("distances").path(0).path(i);
            JsonNode t = root.path("durations").path(0).path(i);
            if (d.isNull() || d.isMissingNode()) continue;
            double km = d.asDouble() / 1000.0;
            Cand c = cands.get(i);
            Map<String, Object> cur = best.get(c.chain());
            if (cur != null && (double) cur.get("oneWayKm") <= km) continue;
            double litres = (km * 2) * consumption(vehicleSize) / 100.0;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("chain", c.chain());
            m.put("name", c.name());
            m.put("address", c.address());
            m.put("lon", c.lon());
            m.put("lat", c.lat());
            m.put("oneWayKm", Math.round(km * 10) / 10.0);
            m.put("minutes", Math.round(t.asDouble() / 60.0));
            m.put("roundTripKm", Math.round(km * 20) / 10.0);
            m.put("litres", Math.round(litres * 100) / 100.0);
            m.put("fuelCostNZD", Math.round(litres * fuelPrice * 100) / 100.0);
            best.put(c.chain(), m);
        }

        List<Map<String, Object>> stores = new ArrayList<>(best.values());
        stores.sort(Comparator.comparingDouble(s -> (double) s.get("oneWayKm")));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("home", Map.of("lon", home.lon, "lat", home.lat));
        out.put("stores", stores);
        out.put("closestChain", stores.isEmpty() ? null : stores.get(0).get("chain"));
        return out;
    }

    private void checkCoords(List<List<Double>> coords) {
        if (coords == null || coords.size() < 2 || coords.size() > 8)
            throw new IllegalArgumentException("coords must have between 2 and 8 points");
        for (List<Double> c : coords)
            if (c == null || c.size() != 2 || c.get(0) == null || c.get(1) == null)
                throw new IllegalArgumentException("each point must be [lon, lat]");
    }

    private JsonNode post(String url, Object body) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) throw new RuntimeException("ORS " + res.statusCode() + ": " + res.body());
        return mapper.readTree(res.body());
    }

    /** Driving distances (metres) and durations (seconds) between every pair of points. */
    public Map<String, Object> matrix(List<List<Double>> coords, String vehicleSize, double fuelPrice) throws Exception {
        checkCoords(coords);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("locations", coords);
        body.put("metrics", List.of("distance", "duration"));
        JsonNode root = post("https://api.openrouteservice.org/v2/matrix/driving-car", body);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("distances", mapper.convertValue(root.path("distances"), Object.class));
        out.put("durations", mapper.convertValue(root.path("durations"), Object.class));
        out.put("costPerKm", consumption(vehicleSize) / 100.0 * fuelPrice);
        return out;
    }

    /** One road route through the points in the given order, as [lat, lon] pairs. */
    public Map<String, Object> tour(List<List<Double>> coords) throws Exception {
        checkCoords(coords);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("coordinates", coords);
        JsonNode root = post("https://api.openrouteservice.org/v2/directions/driving-car/geojson", body);
        JsonNode f = root.path("features").path(0);
        List<double[]> pts = new ArrayList<>();
        for (JsonNode p : f.path("geometry").path("coordinates"))
            pts.add(new double[]{p.get(1).asDouble(), p.get(0).asDouble()});
        JsonNode sum = f.path("properties").path("summary");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("points", pts);
        out.put("km", Math.round(sum.path("distance").asDouble() / 100.0) / 10.0);
        out.put("minutes", Math.round(sum.path("duration").asDouble() / 60.0));
        return out;
    }

    /** Route geometry as [lat, lon] pairs, ready for Leaflet. */
    public List<double[]> route(double fromLon, double fromLat, double toLon, double toLat) throws Exception {
        String url = "https://api.openrouteservice.org/v2/directions/driving-car"
                + "?api_key=" + apiKey
                + "&start=" + fromLon + "," + fromLat
                + "&end=" + toLon + "," + toLat;
        List<double[]> pts = new ArrayList<>();
        for (JsonNode p : get(url).path("features").path(0).path("geometry").path("coordinates"))
            pts.add(new double[]{p.get(1).asDouble(), p.get(0).asDouble()});
        return pts;
    }
}
