package com.team6.backend.products.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.Immutable;

import java.io.Serializable;


/**
 * CREATE TABLE woolworths (
 *     store_id TEXT NOT NULL,
 *     barcode TEXT NOT NULL,
 *     price INT,
 *     save_price INT,
 *     member_price INT,
 *     price_per_unit TEXT,
 *     stock_level BOOLEAN,
 *     dep_id SMALLINT,
 *
 *     PRIMARY KEY (store_id, barcode),
 *     FOREIGN KEY (barcode) REFERENCES barcode_name(barcode),
 *     FOREIGN KEY (dep_id) REFERENCES ww_dep_id(dep_id)
 * );
 */
@Entity
@Table(name = "woolworths")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Immutable
public class Woolworths {

    @EmbeddedId
    private WoolworthsId woolworthsId;

    @Column(name="price")
    private Integer price;

    @Column(name="save_price")
    private Integer savePrice;

    @Column(name="member_price")
    private Integer memberPrice;

    @Column(name="price_per_unit")
    private String pricePerUnit;

    @Column(name="stock_level")
    private Boolean stockLevel;

    @MapsId("barcode")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barcode", nullable = false)
    private BarcodeName barcode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dep_id")
    private WwDepId dep;

}

