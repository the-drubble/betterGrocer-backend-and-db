package com.team6.backend.app.services;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.team6.backend.app.entities.AppUser;
import com.team6.backend.app.entities.UserProfile;
import com.team6.backend.app.repositories.AppUserRepository;
import com.team6.backend.app.repositories.ProfileRepository;

@Service
public class AppUserService {

    private final AppUserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;

    /** profile PINs are exactly four digits */
    private static final Pattern PIN_FORMAT = Pattern.compile("^\\d{4}$");

    public AppUserService(AppUserRepository userRepository, ProfileRepository profileRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AppUser createUser(String username, String rawPassword, String role) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        System.out.println(user.getRole());
        return userRepository.save(user);
    }

    /**
     * null/blank -> null (no PIN). Anything else must be exactly 4 digits (400 otherwise).
     */
    public String normalizePin(String pin) {
        if (pin == null || pin.isBlank()) return null;
        String trimmed = pin.trim();
        if (!PIN_FORMAT.matcher(trimmed).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PIN must be exactly 4 digits");
        }
        return trimmed;
    }

    public boolean hasPin(UserProfile profile) {
        return profile.getPin() != null && !profile.getPin().isBlank();
    }

    /** true if the profile has no PIN, or rawPin matches it */
    public boolean pinMatches(UserProfile profile, String rawPin) {
        if (!hasPin(profile)) return true;
        return rawPin != null && passwordEncoder.matches(rawPin.trim(), profile.getPin());
    }

    /** pin may be null/blank for a profile without a PIN */
    public UserProfile createProfile(AppUser user, String name, String pin) {
        String validPin = normalizePin(pin);
        UserProfile profile = new UserProfile();
        profile.setUser(user);
        profile.setProfileName(name);
        if (validPin != null) {
            profile.setPin(passwordEncoder.encode(validPin));
        }
        System.out.println(profile.getProfileName());
        return profileRepository.save(profile);
    }

    public boolean usernameExists(String username) {
        return userRepository.findByUsername(username).isPresent();
    }


    @Transactional(transactionManager = "appTransactionManager", readOnly = true)
    public List<UserProfile> getAllProfilesByUserId(Long userId) {
        return profileRepository.findByUserId(userId);
    }

    public Optional<UserProfile> findProfileGivenUserId(Long profileId, Long userId) {
        return profileRepository.findByIdAndUserId(profileId, userId);
    }

    public boolean profileExists(String name, Long userId) {
        return profileRepository.findByProfileNameAndUserId(name, userId).isPresent();
    }

    /** set or change a profile's PIN (must be 4 digits) */
    @Transactional(transactionManager = "appTransactionManager")
    public void setPin(Long profileId, Long userId, String rawPin) {
        String validPin = normalizePin(rawPin);
        if (validPin == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PIN must be exactly 4 digits");
        }
        UserProfile profile = profileRepository.findByIdAndUserId(profileId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        profile.setPin(passwordEncoder.encode(validPin));
    }

    /** remove a profile's PIN, so it opens without one */
    @Transactional(transactionManager = "appTransactionManager")
    public void removePin(Long profileId, Long userId) {
        UserProfile profile = profileRepository.findByIdAndUserId(profileId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        profile.setPin(null);
    }

    /** id of the account's main profile (the first one created), or null if it has none */
    public Long getMainProfileId(Long userId) {
        return profileRepository.findMainProfileId(userId).orElse(null);
    }

    public boolean isMainProfile(Long profileId, Long userId) {
        return profileId != null && profileId.equals(getMainProfileId(userId));
    }

    /**
     * Permanently deletes an account and everything that belongs to it:
     * list items, lists (shared ones included), each profile's saved stores,
     * the profiles, then the account itself. All or nothing.
     */
    @Transactional(transactionManager = "appTransactionManager")
    public void deleteAccount(Long userId) {
        userRepository.deleteListItemsForUser(userId);
        userRepository.deleteListsForUser(userId);
        userRepository.deleteProfileStoresForUser(userId);
        userRepository.deleteProfilesForUser(userId);
        userRepository.deleteUserRow(userId);
    }
}