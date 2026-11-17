package com.team6.backend.products.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

import java.io.Serializable;

/**
 * CREATE TABLE newworld (
 *     store_id TEXT NOT NULL,
 *     internal_sku TEXT NOT NULL,
 *     price INT,
 *     price_per_unit TEXT,
 *     stock_level BOOLEAN,
 *     dep_id SMALLINT,
 *
 *     PRIMARY KEY (store_id, internal_sku),
 *     FOREIGN KEY (internal_sku) REFERENCES barcode_foodstuff_sku(internal_sku),
 *     FOREIGN KEY (dep_id) REFERENCES fs_dep_id(dep_id)
 * );
 */
@Entity
@Table(name = "newworld")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Immutable
public class Newworld {

    @EmbeddedId
    private NewworldId newworldId;

    @Column(name="price")
    private Integer price;

    @Column(name="price_per_unit")
    private String pricePerUnit;

    @Column(name="stock_level")
    private Boolean stockLevel;

    @MapsId("internalSku")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "internal_sku", nullable = false)
    private BarcodeFoodstuffSku internalSku;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dep_id")
    private FsDepId dep;

}

