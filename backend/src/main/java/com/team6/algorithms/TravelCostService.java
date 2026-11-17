package com.team6.algorithms;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
public class TravelCostService {

    private static final double CONSUMPTION_SMALL  = 6.5;
    private static final double CONSUMPTION_MEDIUM = 9.0;
    private static final double CONSUMPTION_LARGE  = 13.0;

    private final MapsService mapsService;

    public TravelCostService(MapsService mapsService) {
        this.mapsService = mapsService;
    }

    public enum VehicleSize { SMALL, MEDIUM, LARGE }

    public static class StoreTravelResult {
        public final String storeName;
        public final double oneWayKm;
        public final int    durationSeconds;
        public final double roundTripKm;
        public final double litresUsed;
        public final double fuelCostNZD;

        public StoreTravelResult(String storeName, double oneWayKm, int durationSeconds,
                                 double roundTripKm, double litresUsed, double fuelCostNZD) {
            this.storeName       = storeName;
            this.oneWayKm        = oneWayKm;
            this.durationSeconds = durationSeconds;
            this.roundTripKm     = roundTripKm;
            this.litresUsed      = litresUsed;
            this.fuelCostNZD     = fuelCostNZD;
        }

        public static StoreTravelResult unreachable(String name) {
            return new StoreTravelResult(name, -1, -1, -1, -1, -1);
        }

        public boolean isReachable() {
            return oneWayKm >= 0;
        }

        public String durationFormatted() {
            int mins = durationSeconds / 60;
            return mins + " min" + (mins == 1 ? "" : "s");
        }

        public String toString() {
            if (!isReachable()) return "  " + storeName + ": unreachable";
            return String.format(
                "  %-38s  %4.1f km one-way  %s  ->  %.2f L  ->  $%.2f NZD",
                storeName, oneWayKm, durationFormatted(), litresUsed, fuelCostNZD
            );
        }
    }

    public List<StoreTravelResult> calculateCosts(
            String homeAddress,
            List<String> storeAddresses,
            VehicleSize vehicleSize,
            double fuelPriceNZD) {

        double consumptionPer100km = getConsumption(vehicleSize);
        List<StoreTravelResult> results = new ArrayList<>();

        for (String store : storeAddresses) {
            MapsService.DistanceResult data = mapsService.getDistanceBetweenAddresses(homeAddress, store);

            if (data == null) {
                results.add(StoreTravelResult.unreachable(store));
                continue;
            }

            double distanceKm  = data.distanceMetres / 1000.0;
            int    durationSec = data.durationSeconds;
            double roundTripKm = distanceKm * 2;
            double litresUsed  = (roundTripKm * consumptionPer100km) / 100.0;
            double fuelCostNZD = litresUsed * fuelPriceNZD;

            results.add(new StoreTravelResult(
                store, distanceKm, durationSec, roundTripKm, litresUsed, fuelCostNZD
            ));
        }

        results.sort((a, b) -> Double.compare(a.fuelCostNZD, b.fuelCostNZD));

        return results;
    }

    private double getConsumption(VehicleSize size) {
        if (size == VehicleSize.SMALL) return CONSUMPTION_SMALL;
        if (size == VehicleSize.LARGE) return CONSUMPTION_LARGE;
        return CONSUMPTION_MEDIUM;
    }
}
