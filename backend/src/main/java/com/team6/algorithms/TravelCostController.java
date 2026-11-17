package com.team6.algorithms;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/travel-cost")
public class TravelCostController {

    private final TravelCostService travelCostService;

    public TravelCostController(TravelCostService travelCostService) {
        this.travelCostService = travelCostService;
    }

    public static class TravelCostRequest {
        public String homeAddress;
        public List<String> storeAddresses;
        public TravelCostService.VehicleSize vehicleSize = TravelCostService.VehicleSize.MEDIUM;
        public double fuelPriceNZD;
    }

    @PostMapping
    public ResponseEntity<?> calculate(@RequestBody TravelCostRequest req) {
        if (req.homeAddress == null || req.homeAddress.isBlank()
                || req.storeAddresses == null || req.storeAddresses.isEmpty()
                || req.fuelPriceNZD <= 0) {
            return ResponseEntity.badRequest()
                    .body("homeAddress, storeAddresses and a positive fuelPriceNZD are required");
        }
        return ResponseEntity.ok(travelCostService.calculateCosts(
                req.homeAddress, req.storeAddresses, req.vehicleSize, req.fuelPriceNZD));
    }
}
