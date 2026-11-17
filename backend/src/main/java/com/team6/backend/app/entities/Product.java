package com.team6.backend.app.entities;

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
 CREATE TABLE IF NOT EXISTS products (
    barcode TEXT PRIMARY KEY,
    display_name TEXT
 )
 */
@Entity
@Table(name = "products")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Product {

    @Id
    @Column(nullable = false)
    private String barcode;

    @Column(name = "display_name")
    private String displayName;

    @OneToMany(mappedBy = "productReference")
    private Set<StoreProductPrice> productReferenceStoreProductPrices = new HashSet<>();

    @OneToMany(mappedBy = "product")
    private Set<ShoppingListItem> productReferenceShoppingListItems = new HashSet<>();

}