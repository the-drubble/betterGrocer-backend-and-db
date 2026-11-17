package com.team6.backend.products.repositories;

import com.team6.backend.products.entities.BarcodeFoodstuffSku;
import org.springframework.data.jpa.repository.JpaRepository;


public interface BarcodeFoodstuffSkuRepository extends JpaRepository<BarcodeFoodstuffSku, String> {
}
