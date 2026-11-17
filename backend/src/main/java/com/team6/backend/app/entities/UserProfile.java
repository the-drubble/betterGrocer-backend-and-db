package com.team6.backend.app.entities;
/*

Code from Alexander Obregon :
https://medium.com/@AlexanderObregon/building-a-login-system-with-spring-boot-and-spring-security-2ef6f110a9cb
and my goat:
https://medium.com/@alxkm/lombok-essentials-enhancing-java-coding-efficiency-and-maintainability-1e8904dbefe1
*/

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 *
 * Maps to the profiles table in the db
 * CREATE TABLE IF NOT EXISTS profiles (
 *     id BIGSERIAL PRIMARY KEY,
 *     user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
 *     profile_name TEXT NOT NULL,
 *     pin TEXT,
 *     CONSTRAINT unique_profile_name_per_user UNIQUE (user_id, profile_name)
 * );
 */
@Entity
@Table(name="profiles")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UserProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "profile_name")
    private String profileName;

    @Column(name = "pin")
    private String pin;

    //map lists to profile instead of user
    @OneToMany(mappedBy = "profile")
    private Set<ShoppingList> userShoppingLists = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "profile_stores",
            joinColumns = @JoinColumn(name = "profile_id"),
            inverseJoinColumns = @JoinColumn(name = "store_id")
    )
    private Set<Store> profileSavedStores = new HashSet<>();

}

