package com.team6.backend.products.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import org.hibernate.annotations.Immutable;

import java.io.Serializable;
/**
 *     store_id TEXT NOT NULL,
 *     barcode TEXT NOT NULL,
 */
@EqualsAndHashCode
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
@Immutable
public class WoolworthsId implements Serializable {
    @Column(name="store_id") private String storeId;
    @Column(name="barcode") private String barcode;
}
