package com.team6.backend.app.repositories;

import com.team6.backend.app.entities.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, String> {

    /**
     * 
     * Safer product insert for conflicts 
     */
    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO products (barcode, display_name)
            VALUES (:barcode, :displayName)
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("barcode") String barcode, @Param("displayName") String displayName);
}
