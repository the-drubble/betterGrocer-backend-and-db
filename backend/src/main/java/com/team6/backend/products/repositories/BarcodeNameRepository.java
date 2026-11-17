package com.team6.backend.products.repositories;

import com.team6.backend.products.entities.BarcodeName;
import org.springframework.data.jpa.repository.JpaRepository;


public interface BarcodeNameRepository extends JpaRepository<BarcodeName, String> {
}
