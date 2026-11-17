package com.team6.backend.app.repositories;

import com.team6.backend.app.entities.StoreProductPrice;
import com.team6.backend.app.entities.StoreProductPriceId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoreProductPriceRepository extends JpaRepository<StoreProductPrice, StoreProductPriceId> {
}
