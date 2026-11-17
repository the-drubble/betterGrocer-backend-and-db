package com.team6.backend.products.controllers;


import com.team6.backend.products.services.StoreSearchService;
import com.team6.backend.products.util.StoreSearchResult;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashSet;
import java.util.List;


@RestController
@RequestMapping("/api/stores")
public class StoreController {
    private final StoreSearchService storeSearchService;

    public StoreController(StoreSearchService storeSearchService) { this.storeSearchService = storeSearchService; }

    @Operation(summary = "search (no fuzzy) for a store, e.g query=\"woolworths\" will return a list of all ww stores, in the form of: (storeId, name, address, franchiseName)")
    @GetMapping("/search")
    public List<StoreSearchResult> search(@RequestParam String query) {
        return storeSearchService.search(query);
    }

    @GetMapping("/by-ids")
    public List<StoreSearchResult> getStoresByIds(@RequestParam List<String> ids) {

        return storeSearchService.findByIds(new HashSet<>(ids))
                .values()
                .stream()
                .toList();
    }
}