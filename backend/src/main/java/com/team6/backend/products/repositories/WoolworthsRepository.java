package com.team6.backend.products.repositories;

import com.team6.backend.products.entities.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.*;


public interface WoolworthsRepository extends JpaRepository<Woolworths, WoolworthsId> {
    @Query(value = "select store_id from woolworths", nativeQuery = true)
    List<String> findAllStoreIds();
}
