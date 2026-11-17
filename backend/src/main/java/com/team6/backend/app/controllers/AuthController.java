package com.team6.backend.app.controllers;

import java.util.List;

import com.team6.backend.app.util.UserAuthReq;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.team6.backend.app.entities.AppUser;
import com.team6.backend.app.entities.SecurityUser;
import com.team6.backend.app.entities.UserProfile;
import com.team6.backend.app.services.AppUserService;
import com.team6.backend.app.util.PasswordPolicy;
import com.team6.backend.app.util.ProfileResponse;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;



@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AppUserService userService;
    private final AuthenticationManager authenticationManager;

    public AuthController(AppUserService userService, AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
    }

    /**
     * check if the user is logged in or not
     * true if logged in
     * false if not logged in
     * @return
     */
    @Operation(summary = "true means logged in, false means not logged in")
    @GetMapping("/status")
    public boolean isLoggedInStatus() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication==null || !authentication.isAuthenticated()) return false;
        return !(authentication instanceof AnonymousAuthenticationToken);
    }


    /**
     * register a new user
     * usernames must be unique
     * @return
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserAuthReq userAuthReq) {
        String username = userAuthReq.username() == null ? "" : userAuthReq.username().trim();
        String password = userAuthReq.password();

        if (username.isEmpty()) {
            return ResponseEntity.badRequest().body("Please enter a username");
        }

        String passwordError = PasswordPolicy.errorMessage(username, password);
        if (passwordError != null) {
            return ResponseEntity.badRequest().body(passwordError);
        }

        if (userService.usernameExists(username)) {
            return ResponseEntity.badRequest().body("Username already taken");
        }

        AppUser user = userService.createUser(username, password, "USER");
        userService.createProfile(user, username, "");
        return ResponseEntity.ok("User registered");
    }

    /**
     * pretty self-explanatory
     * requires a username and password
     *
     * this should prob be changed to @RequestBody for safety however the request is build like so:
     * http://localhost:8080/api/auth/login?username=bob&password=bobspass
     *
     * @param username
     * @param password
     * @param request
     * @param response
     * @return
     */
    @PostMapping("/login")
public ResponseEntity<?> login(
        @RequestBody UserAuthReq userAuthReq,
        HttpServletRequest request,
        HttpServletResponse response) {
        String username = userAuthReq.username();
        String password = userAuthReq.password();

        try {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        request.getSession(true).setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext()
        );

        return ResponseEntity.ok("Logged in");

    } catch (BadCredentialsException e) {
        return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED)
                .body("Invalid username or password");
    }
}
    /**
     * also self-explanatory
     * doesnt require anything
     * http://localhost:8080/auth/logout
     *
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false); // log out error npe fix
        if (session!=null) session.invalidate();

        SecurityContextHolder.clearContext();
        return ResponseEntity.ok("Logged out");
    }

    /* PIN brute-force protection: after 5 wrong PINs for a profile, that profile
       is locked for 60 seconds in this session. */
    private static final int MAX_PIN_ATTEMPTS = 5;
    private static final long PIN_LOCK_MILLIS = 60_000;

    /**
     * Selects (logs in) a profile.
     * pin is only needed when the profile has one; profiles without a PIN open freely.
     * Wrong PIN -> 403, too many wrong PINs -> 429.
     */
    @PostMapping("/profile")
    public ResponseEntity<?> selectProfile(
        @RequestParam Long profileId,
        @RequestParam(required = false) String pin,
        @AuthenticationPrincipal SecurityUser userDetails,
        HttpSession session) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not logged in");
        }

        UserProfile profile = userService.findProfileGivenUserId(profileId, userDetails.getId()).orElse(null);
        if (profile == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Profile not found");
        }

        String failKey = "pinFailures:" + profileId;
        String lockKey = "pinLockedUntil:" + profileId;

        Long lockedUntil = (Long) session.getAttribute(lockKey);
        if (lockedUntil != null && lockedUntil > System.currentTimeMillis()) {
            long seconds = Math.max(1, (lockedUntil - System.currentTimeMillis() + 999) / 1000);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Too many wrong PINs. Try again in " + seconds + " seconds.");
        }

        if (!userService.pinMatches(profile, pin)) {
            Integer failures = (Integer) session.getAttribute(failKey);
            failures = failures == null ? 1 : failures + 1;

            if (failures >= MAX_PIN_ATTEMPTS) {
                session.setAttribute(lockKey, System.currentTimeMillis() + PIN_LOCK_MILLIS);
                session.removeAttribute(failKey);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body("Too many wrong PINs. Try again in " + (PIN_LOCK_MILLIS / 1000) + " seconds.");
            }

            session.setAttribute(failKey, failures);
            boolean noPinGiven = pin == null || pin.isBlank();
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(noPinGiven ? "This profile needs a PIN" : "Incorrect PIN");
        }

        session.removeAttribute(failKey);
        session.removeAttribute(lockKey);
        session.setAttribute("profileId", profile.getId());
        return ResponseEntity.ok("Profile selected");
    }
    
    @GetMapping("/getProfiles")
    public List<ProfileResponse> getAllUserProfiles(Authentication authentication) {

        System.out.println("AUTHENTICATION: " + authentication);
        System.out.println("PRINCIPAL: " + authentication.getPrincipal());
        System.out.println("PRINCIPAL CLASS: " +
                authentication.getPrincipal().getClass().getName());

        SecurityUser userDetails =
                (SecurityUser) authentication.getPrincipal();

        Long mainProfileId = userService.getMainProfileId(userDetails.getId());

        return userService.getAllProfilesByUserId(userDetails.getId())
                .stream()
                .map(profile -> ProfileResponse.from(profile, mainProfileId))
                .toList();
    }


        /**
     * register a new user
     * usernames must be unique
     *
     * this should prob be changed to @RequestBody for safety however the request is build like so:
     * http://localhost:8080/api/auth/register?username=bob&password=bobspass
     *
     * @param username
     * @param password
     * @return
     */
    /**
     * pin is optional: leave it out for a profile that opens without one,
     * otherwise it must be exactly 4 digits.
     */
    @PostMapping("/addProfile")
    public ResponseEntity<?> addProfile(@AuthenticationPrincipal SecurityUser userDetails, @RequestParam String name, @RequestParam(required = false) String pin) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not logged in");
        }

        String trimmedName = name == null ? "" : name.trim();
        if (trimmedName.isEmpty()) {
            return ResponseEntity.badRequest().body("Profile name is required");
        }
        if (userService.profileExists(trimmedName, userDetails.getId())) {
            return ResponseEntity.badRequest().body("Profile name already taken");
        }

        try {
            userService.createProfile(userDetails.getAppUser(), trimmedName, pin);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
        return ResponseEntity.ok("Profile added");
    }

    /** set or change the selected profile's PIN (4 digits) */
    @Operation(summary = "set or change the selected profile's PIN (exactly 4 digits)")
    @PutMapping("/profile/pin")
    public ResponseEntity<?> setProfilePin(
            @RequestParam String pin,
            @AuthenticationPrincipal SecurityUser userDetails,
            HttpSession session) {
        Long profileId = selectedProfileId(userDetails, session);
        if (profileId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No profile selected");
        }
        try {
            userService.setPin(profileId, userDetails.getId(), pin);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
        return ResponseEntity.ok("PIN saved");
    }

    /** remove the selected profile's PIN */
    @Operation(summary = "remove the selected profile's PIN")
    @DeleteMapping("/profile/pin")
    public ResponseEntity<?> removeProfilePin(
            @AuthenticationPrincipal SecurityUser userDetails,
            HttpSession session) {
        Long profileId = selectedProfileId(userDetails, session);
        if (profileId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No profile selected");
        }
        userService.removePin(profileId, userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Main profile only: remove another profile's PIN (e.g. when it has been forgotten).
     */
    @Operation(summary = "main profile only: remove any profile's PIN on the account (forgotten PIN)")
    @DeleteMapping("/profiles/{profileId}/pin")
    public ResponseEntity<?> resetOtherProfilePin(
            @PathVariable Long profileId,
            @AuthenticationPrincipal SecurityUser userDetails,
            HttpSession session) {
        Long currentProfileId = selectedProfileId(userDetails, session);
        if (currentProfileId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No profile selected");
        }
        if (!userService.isMainProfile(currentProfileId, userDetails.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only the main profile can reset another profile's PIN");
        }
        try {
            userService.removePin(profileId, userDetails.getId());
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
        return ResponseEntity.noContent().build();
    }

    /** the selected profile id, or null if not logged in / none selected */
    private Long selectedProfileId(SecurityUser userDetails, HttpSession session) {
        if (userDetails == null) return null;
        return (Long) session.getAttribute("profileId");
    }

    /**
     * 
     * http://localhost:8080/api/auth/profileStatus
     * @return
     */
    @GetMapping("/profile/status")
    public ResponseEntity<?> profileStatus(HttpSession session) {
        Long profileId = (Long) session.getAttribute("profileId");

        if (profileId == null) {
            return ResponseEntity.ok(false);
        }

        return ResponseEntity.ok(true);
    }

    /**
     * The currently selected profile, including whether it is the account's main profile
     * (main = true means it may delete the account).
     * 401 if not logged in, 404 if no profile is selected.
     */
    @Operation(summary = "the selected profile; main = true if it is the account's main profile")
    @GetMapping("/profile/current")
    public ResponseEntity<?> currentProfile(
            @AuthenticationPrincipal SecurityUser userDetails,
            HttpSession session) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not logged in");
        }

        Long profileId = (Long) session.getAttribute("profileId");
        Long mainProfileId = userService.getMainProfileId(userDetails.getId());

        return userService.findProfileGivenUserId(profileId, userDetails.getId())
                .<ResponseEntity<?>>map(profile -> ResponseEntity.ok(ProfileResponse.from(profile, mainProfileId)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("No profile selected"));
    }

    /**
     * Permanently deletes the logged-in account: every profile, list and saved store.
     * Only allowed while the account's main profile is selected (403 otherwise).
     * Logs the user out afterwards.
     */
    @Operation(summary = "delete the whole account (main profile only), then log out")
    @DeleteMapping("/account")
    public ResponseEntity<?> deleteAccount(
            @AuthenticationPrincipal SecurityUser userDetails,
            HttpServletRequest request) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not logged in");
        }

        HttpSession session = request.getSession(false);
        Long profileId = session == null ? null : (Long) session.getAttribute("profileId");

        if (!userService.isMainProfile(profileId, userDetails.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only the main profile can delete this account");
        }

        userService.deleteAccount(userDetails.getId());

        // same as logout
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();

        return ResponseEntity.noContent().build();
    }
}
