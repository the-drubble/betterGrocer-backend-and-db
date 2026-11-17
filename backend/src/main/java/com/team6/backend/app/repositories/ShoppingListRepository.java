package com.team6.backend.app.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.team6.backend.app.entities.ShoppingList;

import jakarta.persistence.LockModeType;


public interface ShoppingListRepository extends JpaRepository<ShoppingList, Long> {
    List<ShoppingList> findByUserId(Long userId);
    List<ShoppingList> findByProfileId(Long profieId);
    Optional<ShoppingList> findByIdAndUserId(Long id, Long userId);
    Optional<ShoppingList> findByUserIdAndName(Long userId, String listName);
    Optional<ShoppingList> findByIdAndProfileId(Long id, Long profileId);
    Optional<ShoppingList> findByProfileIdAndName(Long profileId, String listName);



    /** Self explanitory : owned lists / account shared lists */
    @Query("""
            select l from ShoppingList l
            where l.profile.id = :profileId
               or (l.shared = true and l.user.id = :userId)
            order by l.createdAt asc, l.id asc
            """)
    List<ShoppingList> findAccessible(@Param("profileId") Long profileId, @Param("userId") Long userId);

    /** read a single accessible list */
    @Query("""
            select l from ShoppingList l
            where l.id = :listId
              and (l.profile.id = :profileId or (l.shared = true and l.user.id = :userId))
            """)
    Optional<ShoppingList> findAccessibleById(@Param("listId") Long listId,
                                              @Param("profileId") Long profileId,
                                              @Param("userId") Long userId);

    /**
     * Uses @Lock PESSIMISTIC to assume conflict and read and lock row
     * Meaning any further read/write to row will fail until unlocked
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select l from ShoppingList l
            where l.id = :listId
              and (l.profile.id = :profileId or (l.shared = true and l.user.id = :userId))
            """)
    Optional<ShoppingList> findAccessibleByIdForUpdate(@Param("listId") Long listId,
                                                       @Param("profileId") Long profileId,
                                                       @Param("userId") Long userId);
}
