package com.team6.algorithms;

import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/travel-cost")
public class RoutePlanController {

    private static final Logger log = LoggerFactory.getLogger(RoutePlanController.class);

    private final RoutePlanService service;

    public RoutePlanController(RoutePlanService service) {
        this.service = service;
    }

    public static class PlanRequest {
        public Long listId;
        public String homeAddress;
        public String vehicleSize = "MEDIUM";
        public double fuelPriceNZD = 2.80;
    }

    /**
     * Where to shop for a list, using the signed-in profile's saved stores.
     * Body: { "listId": 3, "homeAddress": "...", "vehicleSize": "MEDIUM", "fuelPriceNZD": 2.8 }
     */
    @PostMapping("/plan")
    public ResponseEntity<?> plan(
            @RequestBody PlanRequest req,
            @Parameter(hidden = true) @SessionAttribute(name = "profileId", required = false) Long profileId) {
        if (profileId == null)
            return ResponseEntity.status(400).body(Map.of("error", "Choose a profile first."));
        try {
            return ResponseEntity.ok(
                    service.plan(profileId, req.listId, req.homeAddress, req.vehicleSize, req.fuelPriceNZD));
        } catch (RoutePlanService.PlanException e) {
            return ResponseEntity.status(e.status).body(Map.of("error", e.getMessage()));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode().value())
                    .body(Map.of("error", e.getReason() == null ? "Request failed" : e.getReason()));
        } catch (Exception e) {
            log.error("Plan failed", e);
            return ResponseEntity.status(500).body(Map.of("error", "Could not work out the plan."));
        }
    }
}
