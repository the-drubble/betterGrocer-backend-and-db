package com.team6.backend.products.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;
/*
CREATE TABLE IF NOT EXISTS ps_store_id_store_name_mapping (
    store_id TEXT PRIMARY KEY,
    store_name TEXT,
    store_address TEXT,
    store_island_key VARCHAR(2)
);
 */
@Entity
@Table(name = "ps_store_id_store_name_mapping")
@Immutable
@Getter
@Setter
public class PSIdNameMapping extends FSIdNameMappingEntity {

}
