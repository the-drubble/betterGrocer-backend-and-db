package com.team6.backend.products.repositories;

import com.team6.backend.products.entities.Paknsave;
import com.team6.backend.products.entities.PaknsaveId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;


public interface PaknsaveRepository extends JpaRepository<Paknsave, PaknsaveId> {
    @Query(value = "select store_id from paknsave", nativeQuery = true)
    List<String> findAllStoreIds();
}
