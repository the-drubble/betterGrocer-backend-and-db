package com.team6.backend.products.services;


import com.team6.backend.app.entities.Store;
import com.team6.backend.products.repositories.ProductsSearchRepository;
import com.team6.backend.products.repositories.StoreSearchRepository;
import com.team6.backend.products.util.PerStoreProductsSearchDetails;
import com.team6.backend.products.util.ProductsSearchResult;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ProductsSearchService {

    private final ProductsSearchRepository productsSearchRepository;
    private final StoreSearchRepository storeSearchRepository;
    private final int limit = 75; // could put that into the query itself

    public List<ProductsSearchResult> search(String query) {
        if (query == null || query.isBlank() || query.length() < 3) return List.of();

        List<ProductsSearchProjection> rows =
                productsSearchRepository.searchByNameAndListOfStoreIds(query.trim(), limit, storeSearchRepository.findAllStoreIds());
        return productsSearchResults(rows);
    }

    public List<ProductsSearchResult> search(String query, List<String> storeIds) {
        int storeIdsMaxCount = 20; // i believe there is a limit in characters, but not 100% sure so best be safe and return nothing
        if (query == null || query.isBlank() || query.length() < 3) return List.of();
        if (storeIds == null || storeIds.isEmpty() || storeIds.size() >= storeIdsMaxCount) return List.of();
        List<ProductsSearchProjection> rows =
                productsSearchRepository.searchByNameAndListOfStoreIds(query.trim(), limit, storeIds);
        return productsSearchResults(rows);
    }

    public Optional<PerStoreProductsSearchDetails> getPriceFromGivenStoreAndProductReference(Store store, String productReference) {
        return productsSearchRepository.findByBarcodeAndStoreIdAndFranchiseName(store.getFranchiseName(), store.getId(), productReference)
                .map(e->
                        new PerStoreProductsSearchDetails(
                                e.getStoreId(),
                                e.getPrice(),
                                e.getFranchiseName(),
                                e.getStoreName(),
                                e.getInStockStatus()
                        )
                );
    }

    private List<ProductsSearchResult> productsSearchResults(List<ProductsSearchProjection> rows) {
        Map<String, List<ProductsSearchProjection>> barcode_product_mapping =
                rows.stream().collect(Collectors.groupingBy(
                        e -> e.getBarcode(),
                        () -> new LinkedHashMap<String, List<ProductsSearchProjection>>(),
                        Collectors.toList()
                ));

        List<ProductsSearchResult> productsSearchResults =
                barcode_product_mapping.values() // List<List<ProductsSearchProjection>>
                        .stream().map(e -> {
                            ProductsSearchProjection mainThing = e.get(0);

                            List<PerStoreProductsSearchDetails> perStoreProductsSearchDetails =
                                    e.stream().map(el ->
                                            new PerStoreProductsSearchDetails(
                                                    el.getStoreId(),
                                                    el.getPrice(),
                                                    el.getFranchiseName(),
                                                    el.getStoreName(),
                                                    el.getInStockStatus()
                                            )).toList();

                            return new ProductsSearchResult(
                                    mainThing.getProductName(),
                                    mainThing.getBarcode(),
                                    mainThing.getImageUrl(),
                                    perStoreProductsSearchDetails
                            );
                        }).toList();

        return productsSearchResults;
    }
}
