package com.team6.backend.products.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

import java.util.HashSet;
import java.util.Set;


/**
 * CREATE TABLE barcode_foodstuff_sku (
 *     internal_sku TEXT PRIMARY KEY,
 *     barcode TEXT
 * );
 */
@Entity
@Table(name = "barcode_foodstuff_sku")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Immutable
public class BarcodeFoodstuffSku {

    @Id
    @Column(nullable = false, updatable = false)
    private String internalSku;

    @Column
    private String barcode;

    @OneToMany(mappedBy = "internalSku")
    private Set<Paknsave> internalSkuPaknsaves = new HashSet<>();

    @OneToMany(mappedBy = "internalSku")
    private Set<Newworld> internalSkuNewworlds = new HashSet<>();

}
