package com.team6.backend.products.entities;

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
public class NewworldId implements Serializable {
    @Column(name="store_id") String store_id;
    @Column(name="internal_sku") String internalSku;
}
