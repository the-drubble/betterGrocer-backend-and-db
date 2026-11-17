package com.team6.backend.app.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import org.hibernate.annotations.Immutable;

import java.io.Serializable;

@EqualsAndHashCode
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
@Immutable
public class StoreProductPriceId implements Serializable {
    @Column(name="store_id") private String storeId;
    @Column(name="product_reference") private String productReference;
}
