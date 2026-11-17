package com.team6.algorithms;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/travel-cost")
public class NearestStoreController {

    private final NearestStoreService service;

    public NearestStoreController(NearestStoreService service) {
        this.service = service;
    }

    public static class NearestRequest {
        public String homeAddress;
        public String vehicleSize = "MEDIUM";
        public double fuelPriceNZD = 2.80;
    }

    @PostMapping("/nearest")
    public ResponseEntity<?> nearest(@RequestBody NearestRequest req) {
        if (req.homeAddress == null || req.homeAddress.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "homeAddress is required"));
        try {
            return ResponseEntity.ok(service.nearest(req.homeAddress, req.vehicleSize, req.fuelPriceNZD));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(502).body(Map.of("error", "Routing service failed"));
        }
    }

    @GetMapping("/route")
    public ResponseEntity<?> route(@RequestParam double fromLon, @RequestParam double fromLat,
                                   @RequestParam double toLon, @RequestParam double toLat) {
        try {
            return ResponseEntity.ok(service.route(fromLon, fromLat, toLon, toLat));
        } catch (Exception e) {
            return ResponseEntity.status(502).body(Map.of("error", "Routing service failed"));
        }
    }

    public static class MatrixRequest {
        public List<List<Double>> coords;
        public String vehicleSize = "MEDIUM";
        public double fuelPriceNZD = 2.80;
    }

    public static class TourRequest {
        public List<List<Double>> coords;
    }

    @PostMapping("/matrix")
    public ResponseEntity<?> matrix(@RequestBody MatrixRequest req) {
        try {
            return ResponseEntity.ok(service.matrix(req.coords, req.vehicleSize, req.fuelPriceNZD));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(502).body(Map.of("error", "Routing service failed"));
        }
    }

    @PostMapping("/tour")
    public ResponseEntity<?> tour(@RequestBody TourRequest req) {
        try {
            return ResponseEntity.ok(service.tour(req.coords));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(502).body(Map.of("error", "Routing service failed"));
        }
    }
}
