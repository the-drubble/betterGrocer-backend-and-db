package com.team6.backend.products.repositories;

import com.team6.backend.products.entities.NWIdNameMapping;
import com.team6.backend.products.entities.PSIdNameMapping;
import com.team6.backend.products.entities.WWIdNameMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WWidNameMappingRepository extends JpaRepository<WWIdNameMapping, String> {
    List<WWIdNameMapping> findByStoreNameContainingIgnoreCase(String query);
}

