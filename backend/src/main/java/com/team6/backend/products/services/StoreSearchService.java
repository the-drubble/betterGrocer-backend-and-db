package com.team6.backend.products.services;


import com.team6.backend.products.repositories.NWidNameMappingRepository;
import com.team6.backend.products.repositories.PSidNameMappingRepository;
import com.team6.backend.products.repositories.StoreSearchRepository;
import com.team6.backend.products.repositories.WWidNameMappingRepository;
import com.team6.backend.products.util.StoreSearchResult;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

@Service
public class StoreSearchService {
    private final WWidNameMappingRepository wWidNameMappingRepository;
    private final NWidNameMappingRepository nWidNameMappingRepository;
    private final PSidNameMappingRepository pSidNameMappingRepository;
    public StoreSearchService(WWidNameMappingRepository wWidNameMappingRepository,
                              NWidNameMappingRepository nWidNameMappingRepository,
                              PSidNameMappingRepository pSidNameMappingRepository,
                              StoreSearchRepository storeSearchRepository
    ) {
        this.wWidNameMappingRepository = wWidNameMappingRepository;
        this.nWidNameMappingRepository = nWidNameMappingRepository;
        this.pSidNameMappingRepository = pSidNameMappingRepository;
    }

    /**
     * gets the
     *     String storeId,
     *     String storeName,
     *     String storeAddress,
     *     String franchiseName
     * of when the store name is searched
     * <p>
     * e.g
     * <p>
     * query="woolworths"->(123, woolworths auk, 123 auk st, woolworths),(234, woolworths wel, 90 wel st, woolworths)...
     * <p>
     * query="woolworths auk"->(123, woolworths auk, 123 auk st, woolworths)
     * @param query
     * @return
     */
    public List<StoreSearchResult> search(String query) {
        if (query == null || query.isBlank()) return List.of();

        return Stream.of(
                wWidNameMappingRepository.findByStoreNameContainingIgnoreCase(query).stream()
                        .map(s->new StoreSearchResult(
                                s.getStoreId(), s.getStoreName(), s.getStoreAddress(), "woolworths")),
                nWidNameMappingRepository.findByStoreNameContainingIgnoreCase(query).stream()
                        .map(s->new StoreSearchResult(
                                s.getStoreId(), s.getStoreName(), s.getStoreAddress(), "newworld")),
                pSidNameMappingRepository.findByStoreNameContainingIgnoreCase(query).stream()
                        .map(s->new StoreSearchResult(
                                s.getStoreId(), s.getStoreName(), s.getStoreAddress(), "paknsave"))
        ).flatMap(Function.identity()).toList();
    }



    public Map<String, StoreSearchResult> findByIds(Set<String> storeIds) {
        Map<String, StoreSearchResult> foundStoreIdsToStoreSearchResults = new HashMap<>();
        wWidNameMappingRepository.findAllById(storeIds).forEach(
                e->foundStoreIdsToStoreSearchResults.putIfAbsent(e.getStoreId(),
                        new StoreSearchResult(e.getStoreId(), e.getStoreName(), e.getStoreAddress(), "woolworths")));
        nWidNameMappingRepository.findAllById(storeIds).forEach(
                e->foundStoreIdsToStoreSearchResults.putIfAbsent(e.getStoreId(),
                        new StoreSearchResult(e.getStoreId(), e.getStoreName(), e.getStoreAddress(), "newworld")));
        pSidNameMappingRepository.findAllById(storeIds).forEach(
                e->foundStoreIdsToStoreSearchResults.putIfAbsent(e.getStoreId(),
                        new StoreSearchResult(e.getStoreId(), e.getStoreName(), e.getStoreAddress(), "paknsave")));
        return foundStoreIdsToStoreSearchResults;
    }
}
