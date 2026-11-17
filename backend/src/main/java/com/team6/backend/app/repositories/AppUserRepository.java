package com.team6.backend.app.repositories;
/*

Code from Alexander Obregon :
https://medium.com/@AlexanderObregon/building-a-login-system-with-spring-boot-and-spring-security-2ef6f110a9cb

*/

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.team6.backend.app.entities.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);

    /*
     * ACCOUNT DELETION
     * Explicit deletes, run in this order inside one transaction (see
     * AppUserService.deleteAccount). The schema declares ON DELETE CASCADE on most
     * of these, but tables Hibernate created itself (e.g. profile_stores) may not
     * have it, so nothing here relies on cascades.
     * Products and stores are shared by every account, so they are never deleted.
     */

    @Modifying
    @Query(value = """
            DELETE FROM shopping_list_item
            WHERE shopping_list_id IN (SELECT id FROM shopping_lists WHERE user_id = :userId)
            """, nativeQuery = true)
    int deleteListItemsForUser(@Param("userId") Long userId);

    @Modifying
    @Query(value = "DELETE FROM shopping_lists WHERE user_id = :userId", nativeQuery = true)
    int deleteListsForUser(@Param("userId") Long userId);

    @Modifying
    @Query(value = """
            DELETE FROM profile_stores
            WHERE profile_id IN (SELECT id FROM profiles WHERE user_id = :userId)
            """, nativeQuery = true)
    int deleteProfileStoresForUser(@Param("userId") Long userId);

    @Modifying
    @Query(value = "DELETE FROM profiles WHERE user_id = :userId", nativeQuery = true)
    int deleteProfilesForUser(@Param("userId") Long userId);

    @Modifying
    @Query(value = "DELETE FROM app_users WHERE id = :userId", nativeQuery = true)
    int deleteUserRow(@Param("userId") Long userId);
}
