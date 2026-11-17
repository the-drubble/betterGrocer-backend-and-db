package com.team6.backend.app.entities;


import com.team6.backend.app.entities.ShoppingList;
import com.team6.backend.app.entities.StoreProductPrice;
import com.team6.backend.app.entities.ShoppingListItem;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;


/**
CREATE TABLE IF NOT EXISTS stores (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    location TEXT NOT NULL
);
 */
@Entity
@Table(name="stores")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Store {

    @Id
    @Column(nullable = false)
    private String id;

    @Column(nullable = false)
    private String franchiseName;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String location;

    @OneToMany(mappedBy = "store")
    private Set<StoreProductPrice> storeStoreProductPrices = new HashSet<>();

}
