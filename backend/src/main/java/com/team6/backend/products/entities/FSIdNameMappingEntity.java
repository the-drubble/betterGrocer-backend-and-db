package com.team6.backend.products.entities;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
public abstract class FSIdNameMappingEntity extends IdNameMappingEntity {
    @Column(name = "store_island_key")
    private String storeIslandKey;
}
