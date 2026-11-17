package com.team6.backend.app.util;

import com.team6.backend.app.entities.UserProfile;

/**
 * main = this is the account's main profile (the one created at registration).
 * Only the main profile can delete the account.
 */
public record ProfileResponse(Long id, String profileName, boolean hasPin, boolean main) {
    public static ProfileResponse from(UserProfile p, Long mainProfileId) {
        return new ProfileResponse(p.getId(), p.getProfileName(),
                p.getPin() != null && !p.getPin().isBlank(),
                p.getId().equals(mainProfileId));
    }
}
