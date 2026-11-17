package com.team6.backend.products.controllers;


import com.team6.backend.products.services.ProductsSearchService;
import com.team6.backend.products.util.ProductsSearchResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductsController {

    private final ProductsSearchService productsSearchService;

    public ProductsController(ProductsSearchService productSearchService) {
        this.productsSearchService = productSearchService;
    }

    @GetMapping("/search")
    public List<ProductsSearchResult> search(@RequestParam(name = "q", required = false) String q) {
        return productsSearchService.search(q);
    }

    /**
     * limit to 20 stores
     * @param q
     * @param storeIds
     * @return
     */
    @GetMapping("/searchX")
    public List<ProductsSearchResult> searchWithStoreIdsFilter(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "store_ids") List<String> storeIds
    ) {
        return productsSearchService.search(q, storeIds);
    }

}
