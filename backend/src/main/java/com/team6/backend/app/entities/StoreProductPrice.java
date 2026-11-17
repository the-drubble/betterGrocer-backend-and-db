package com.team6.backend.app.entities;


import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

/**
 CREATE TABLE IF NOT EXISTS store_product_prices (
     store_id TEXT NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
     product_reference TEXT NOT NULL REFERENCES products(barcode) ON DELETE CASCADE,
     price_cents INT NOT NULL CHECK (price_cents >=0),
     in_stock BOOLEAN,
     recorded_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

     PRIMARY KEY (store_id, product_reference)
 )
 */
@Entity
@Table(name = "store_product_prices")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class StoreProductPrice {
    @EmbeddedId
    private StoreProductPriceId id;

    @Column(nullable = false)
    private Integer priceCents;

    @Column
    private Boolean inStock;

    @Column(nullable = false)
    private OffsetDateTime recordedAt;

    @MapsId("storeId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @MapsId("productReference")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_reference", nullable = false)
    private Product productReference;
}



