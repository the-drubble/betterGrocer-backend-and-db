package com.team6.backend.products.repositories;

import com.team6.backend.products.entities.PSIdNameMapping;
import com.team6.backend.products.entities.WWIdNameMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PSidNameMappingRepository extends JpaRepository<PSIdNameMapping, String> {
    List<PSIdNameMapping> findByStoreNameContainingIgnoreCase(String query);
}
