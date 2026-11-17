package com.team6.backend.app.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.team6.backend.app.entities.AppUser;
import com.team6.backend.app.entities.UserProfile;

public interface ProfileRepository extends JpaRepository<UserProfile, Long> {
    List<UserProfile> findByUser(AppUser user);
    List<UserProfile> findByUserId(Long userId);
    Optional<UserProfile> findByIdAndUserId(Long id, Long userId);
    Optional<UserProfile> findByProfileNameAndUserId(String profileName, Long userId);

    /** the account (user) a profile belongs to, used for shared-list access checks */
    @Query("select p.user.id from UserProfile p where p.id = :profileId")
    Optional<Long> findUserIdByProfileId(@Param("profileId") Long profileId);

    /**
     * The account's main profile: the one registration creates, i.e. the first
     * profile made on the account (lowest id). Only it may delete the account.
     */
    @Query("select min(p.id) from UserProfile p where p.user.id = :userId")
    Optional<Long> findMainProfileId(@Param("userId") Long userId);

}
