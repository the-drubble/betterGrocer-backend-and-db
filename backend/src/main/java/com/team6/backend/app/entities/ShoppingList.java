package com.team6.backend.app.entities;


import com.team6.backend.app.entities.ShoppingListItem;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name="shopping_lists")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ShoppingList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
    //add "user" field and change profile_id to UserProfile 'profile' instead of UserProfile 'user'
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    // the profile that created (owns) the list
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private UserProfile profile;

    /**
     * false = private, only the owning profile can see it
     * true  = shared, every profile on the same account (user) can see and edit its items
     */
    @Column(name = "is_shared", nullable = false)
    private boolean shared = false;


    @OneToMany(mappedBy = "shoppingList")
    private Set<ShoppingListItem> shoppingListShoppingListItems = new HashSet<>();
}
