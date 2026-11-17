package com.team6.backend.products.repositories;


import com.team6.backend.products.entities.Woolworths;
import com.team6.backend.products.entities.WoolworthsId;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * the Woolworths, WoolworthsId was added since those are managed bean objets and this repository doesnt have an entity
 * so its like just filler, probably a terrible way of handling it but it works
 */
public interface StoreSearchRepository extends JpaRepository<Woolworths, WoolworthsId> {

    @Cacheable("allStoreIds")
    @Query(value = """
        select store_id from nw_store_id_store_name_mapping
        union
        select store_id from ps_store_id_store_name_mapping
        union
        select store_id from ww_store_id_store_name_mapping  
      """, nativeQuery = true)
    List<String> findAllStoreIds();
}
