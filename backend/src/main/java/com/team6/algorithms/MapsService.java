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

@Service
public class MapsService {

    private final String apiKey;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MapsService(@Value("${ors.api-key}") String apiKey) {
        this.apiKey = apiKey;
    }

    public static class Coordinates {
        public final double lon;
        public final double lat;

        public Coordinates(double lon, double lat) {
            this.lon = lon;
            this.lat = lat;
        }
    }

    public static class DistanceResult {
        public final int distanceMetres;
        public final int durationSeconds;

        public DistanceResult(int distanceMetres, int durationSeconds) {
            this.distanceMetres = distanceMetres;
            this.durationSeconds = durationSeconds;
        }
    }

    public Coordinates geocode(String address) {
        try {
            String encoded = URLEncoder.encode(address, StandardCharsets.UTF_8);
            String url = "https://api.openrouteservice.org/geocode/search"
                    + "?api_key=" + apiKey
                    + "&text=" + encoded
                    + "&size=1";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("Geocoding failed (" + response.statusCode() + ") for: " + address);
                return null;
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode features = root.path("features");

            if (!features.isArray() || features.isEmpty()) {
                return null;
            }

            JsonNode coords = features.get(0).path("geometry").path("coordinates");
            double lon = coords.get(0).asDouble();
            double lat = coords.get(1).asDouble();

            return new Coordinates(lon, lat);

        } catch (Exception e) {
            System.err.println("Geocoding error for '" + address + "': " + e.getMessage());
            return null;
        }
    }

    public DistanceResult getDistance(Coordinates origin, Coordinates destination) {
        try {
            String url = "https://api.openrouteservice.org/v2/directions/driving-car"
                    + "?api_key=" + apiKey
                    + "&start=" + origin.lon + "," + origin.lat
                    + "&end=" + destination.lon + "," + destination.lat;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("Directions failed (" + response.statusCode() + ")");
                return null;
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode summary = root.path("features").get(0).path("properties").path("summary");

            int distanceMetres = (int) Math.round(summary.path("distance").asDouble());
            int durationSeconds = (int) Math.round(summary.path("duration").asDouble());

            return new DistanceResult(distanceMetres, durationSeconds);

        } catch (Exception e) {
            System.err.println("Directions error: " + e.getMessage());
            return null;
        }
    }

    public DistanceResult getDistanceBetweenAddresses(String fromAddress, String toAddress) {
        Coordinates from = geocode(fromAddress);
        if (from == null) return null;

        Coordinates to = geocode(toAddress);
        if (to == null) return null;

        return getDistance(from, to);
    }
}
