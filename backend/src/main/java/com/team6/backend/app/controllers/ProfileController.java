package com.team6.backend.app.controllers;

import com.team6.backend.app.services.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final ProfileService profileService;
    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    // TODO 99% sure that this is bad naming for rest things, ill change it later once its working

    @Operation(summary = "add storeIds to a profile, technically i dont need to return anything but it returns set of storeIds replaced")
    @PutMapping("/setStores")
    public ResponseEntity<Set<String>> saveStoreIdsToProfile(
            @RequestBody Set<String> storeIds,
            @Parameter(hidden = true) @SessionAttribute("profileId") Long profileId) {
        return ResponseEntity.ok(profileService.saveStoreIdsToProfile(profileId, storeIds));
    }

    @GetMapping("/getStores")
    public ResponseEntity<Set<String>> getStoreIdsFromGivenProfileId(
            @Parameter(hidden = true) @SessionAttribute("profileId") Long profileId) {
        return ResponseEntity.ok(profileService.getStoreIdsFromProfileId(profileId));
    }

    @GetMapping("/getStores/names")
    public ResponseEntity<Set<String>> getStoreNamesFromGivenProfileId(
            @Parameter(hidden = true) @SessionAttribute("profileId") Long profileId) {
        return ResponseEntity.ok(profileService.getStoreNamesFromProfileId(profileId));
    }

    @DeleteMapping("/clearStores")
    public ResponseEntity<Void> clearSavedStoreIdsForProfile(
            @Parameter(hidden = true) @SessionAttribute("profileId") Long profileId) {
        profileService.clearSavedStoreIdsForProfile(profileId);
        return ResponseEntity.noContent().build();
    }
}
