package com.team6.backend.products.entities;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;
import lombok.*;
import org.hibernate.annotations.Immutable;

/**
 * CREATE TABLE barcode_name (
 *     barcode TEXT PRIMARY KEY,
 *     product_name TEXT
 *     img TEXT
 * );
 */
@Entity
@Table(name = "barcode_name")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Immutable
public class BarcodeName {
    @Id
    @Column(nullable = false, updatable = false)
    private String barcode;

    @Column(name="product_name")
    private String productName;

    @Column(name="img")
    private String img;

    @OneToMany(mappedBy = "barcode")
    private Set<Woolworths> barcodeWoolworthses = new HashSet<>();
}
