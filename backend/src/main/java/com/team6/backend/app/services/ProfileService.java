package com.team6.backend.app.services;

import java.util.*;
import java.util.stream.Collectors;

import com.team6.backend.app.entities.Store;
import com.team6.backend.app.repositories.StoreRepository;
import com.team6.backend.products.services.StoreSearchService;
import com.team6.backend.products.util.StoreSearchResult;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.team6.backend.app.entities.UserProfile;
import com.team6.backend.app.repositories.ProfileRepository;
import org.springframework.web.server.ResponseStatusException;


@Service
public class ProfileService {
    private static final int MAX_SAVE_STORE_LIMIT = 1000;
    private final ProfileRepository profileRepository;
    private final StoreRepository storeRepository;
    private final StoreSearchService storeSearchService;
    public ProfileService(ProfileRepository profileRepository, StoreRepository storeRepository, StoreSearchService storeSearchService){
        this.profileRepository = profileRepository;
        this.storeRepository = storeRepository;
        this.storeSearchService = storeSearchService;
    }


    private UserProfile getUserProfileGivenProfileId(Long profileId) {
        return profileRepository.findById(profileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
    }

    @Transactional(transactionManager = "appTransactionManager")
    public Set<String> saveStoreIdsToProfile(Long profileId, Set<String> storeIds) {
        if (storeIds.contains(null))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "storeIds set contains null");
        else if (storeIds.size() > MAX_SAVE_STORE_LIMIT || storeIds.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "storeIds set limits are [1,"+MAX_SAVE_STORE_LIMIT+ "]");

        Map<String, StoreSearchResult> storeIdsMapVerifier = storeSearchService.findByIds(storeIds);
        if (storeIdsMapVerifier.size() != storeIds.size())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "there are invalid store ids present");

        UserProfile userProfile = getUserProfileGivenProfileId(profileId);

        Set<Store> stores = storeIds.stream().map(storeId->storeRepository.findById(storeId).orElseGet(()->{
            StoreSearchResult storeToAddReq = storeIdsMapVerifier.get(storeId);
            Store store1 = new Store();
            store1.setId(storeId);
            store1.setName(storeToAddReq.storeName());
            store1.setFranchiseName(storeToAddReq.franchiseName());
            store1.setLocation(storeToAddReq.storeAddress());
            return storeRepository.save(store1);
        })).collect(Collectors.toSet());

        if (stores.size() != storeIds.size()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown/invalid storeIds");

        userProfile.setProfileSavedStores(stores);
        return storeIds;
    }

    @Transactional(transactionManager = "appTransactionManager")
    public void clearSavedStoreIdsForProfile(Long profileId) {
        UserProfile userProfile = getUserProfileGivenProfileId(profileId);
        userProfile.getProfileSavedStores().clear();
    }

    @Transactional(transactionManager = "appTransactionManager", readOnly = true)
    public Set<String> getStoreIdsFromProfileId(Long profileId) {
        return getUserProfileGivenProfileId(profileId).getProfileSavedStores().stream()
                .map(e->e.getId()).collect(Collectors.toSet());
    }

    public Set<String> getStoreNamesFromProfileId(Long profileId) {
        return getUserProfileGivenProfileId(profileId).getProfileSavedStores().stream()
                .map(e->e.getName()).collect(Collectors.toSet());
    }
}
