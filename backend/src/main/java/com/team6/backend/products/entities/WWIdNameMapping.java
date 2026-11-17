package com.team6.backend.products.entities;


import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

/*
CREATE TABLE IF NOT EXISTS ww_store_id_store_name_mapping (
    store_id TEXT PRIMARY KEY,
    store_name TEXT,
    store_address TEXT
);
 */
@Entity
@Table(name = "ww_store_id_store_name_mapping")
@Immutable
@Getter
@Setter
public class WWIdNameMapping extends IdNameMappingEntity {

}


