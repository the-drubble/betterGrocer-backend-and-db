package com.team6.backend.products.repositories;

import com.team6.backend.products.entities.Newworld;
import com.team6.backend.products.entities.NewworldId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;


public interface NewworldRepository extends JpaRepository<Newworld, NewworldId> {
    @Query(value = "select store_id from newworld", nativeQuery = true)
    List<String> findAllStoreIds();
}
