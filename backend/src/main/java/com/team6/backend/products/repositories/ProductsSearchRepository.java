package com.team6.backend.products.repositories;

import com.team6.backend.products.entities.Woolworths;
import com.team6.backend.products.entities.WoolworthsId;
import com.team6.backend.products.services.ProductsSearchProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * the Woolworths, WoolworthsId was added since those are managed bean objets and this repository doesnt have an entity
 * so its like just filler, probably a terrible way of handling it but it works
 */
public interface ProductsSearchRepository extends JpaRepository<Woolworths, WoolworthsId> {

    @Query(value = """
            with returned_products as (
            	select barcode, product_name, img
            	from barcode_name
            	where to_tsvector(product_name) @@ websearch_to_tsquery(:query)
            	or :query <% product_name
            	order by greatest(
            		ts_rank(
            			setweight(to_tsvector(product_name), 'A'),
            			websearch_to_tsquery(:query)
            		),
            		similarity(:query, product_name)
            	) desc
             ),
             fs_skus as (
            	select r.barcode, bfs.internal_sku from barcode_foodstuff_sku as bfs
            	join returned_products as r on bfs.barcode = r.barcode
             ),
             nw_matched as (
            	select f.barcode,
            	'newworld' as franchise_name,
            	nw_map_table.store_name as store_name,
            	n.store_id, n.stock_level, n.price
            	from newworld as n
            	join fs_skus as f on f.internal_sku = n.internal_sku
            	join nw_store_id_store_name_mapping as nw_map_table on n.store_id = nw_map_table.store_id
            	-- where n.store_id in (store_id_list_nw)
             ),
             ps_matched as (
            	select f.barcode,
            	'paknsave' as franchise_name,
            	ps_map_table.store_name as store_name,
            	p.store_id, p.stock_level, p.price
            	from paknsave as p
            	join fs_skus as f on f.internal_sku = p.internal_sku
            	join ps_store_id_store_name_mapping as ps_map_table on p.store_id = ps_map_table.store_id
            	-- where n.store_id in (store_id_list_ps)
             ),
             ww_matched as (
            	select r.barcode,
            	'woolworths' as franchise_name,
            	ww_map_table.store_name as store_name,
            	w.store_id, w.stock_level, w.price
            	from woolworths as w
            	join returned_products as r on r.barcode = w.barcode
            	join ww_store_id_store_name_mapping as ww_map_table on w.store_id = ww_map_table.store_id
            	-- where n.store_id in (store_id_list_ww)
             )
            
             -- select * from returned_products;
            
             select
            	r.barcode as barcode,
            	r.product_name as productName,
            	r.img as imageUrl,
            	prods.franchise_name as franchiseName,
            	prods.store_name as storeName,
            	prods.store_id as storeId,
            	prods.stock_level as inStockStatus,
            	prods.price as price
             from (
            	select * from ww_matched
            	union all
            	select * from ps_matched
            	union all
            	select * from nw_matched
             ) prods join returned_products as r on r.barcode = prods.barcode
             -- order by r.barcode
    """, nativeQuery = true)
    List<ProductsSearchProjection> searchByName(@Param("query") String query);


    @Query(value = """
                with returned_products as (
                	select barcode, product_name, img
                	from barcode_name
                	where barcode = :barcode
                ),
                fs_skus as (
                	select r.barcode, bfs.internal_sku
                	from barcode_foodstuff_sku as bfs
                	join returned_products as r on bfs.barcode = r.barcode
                	where bfs.barcode = :barcode
                	and :franchise_name in ('paknsave', 'newworld')
                ),
                nw_matched as (
                	select f.barcode,
                	'newworld' as franchise_name,
                	nw_map_table.store_name as store_name,
                	n.store_id, n.stock_level, n.price
                	from newworld as n
                	join fs_skus as f on f.internal_sku = n.internal_sku
                	join nw_store_id_store_name_mapping as nw_map_table on n.store_id = nw_map_table.store_id
                	where :franchise_name = 'newworld' and n.store_id = :store_id
                ),
                ps_matched as (
                	select f.barcode,
                	'paknsave' as franchise_name,
                	ps_map_table.store_name as store_name,
                	p.store_id, p.stock_level, p.price
                	from paknsave as p
                	join fs_skus as f on f.internal_sku = p.internal_sku
                	join ps_store_id_store_name_mapping as ps_map_table on p.store_id = ps_map_table.store_id
                	where :franchise_name = 'paknsave' and p.store_id = :store_id
                ),
                ww_matched as (
                	select r.barcode,
                	'woolworths' as franchise_name,
                	ww_map_table.store_name as store_name,
                	w.store_id, w.stock_level, w.price
                	from woolworths as w
                	join returned_products as r on r.barcode = w.barcode
                	join ww_store_id_store_name_mapping as ww_map_table on w.store_id = ww_map_table.store_id
                	where :franchise_name = 'woolworths' and w.store_id = :store_id and w.barcode = :barcode
                )
            
                select
                	r.barcode as barcode,
                	r.product_name as productName,
                	r.img as imageUrl,
                	prods.franchise_name as franchiseName,
                	prods.store_name as storeName,
                	prods.store_id as storeId,
                	prods.stock_level as inStockStatus,
                	prods.price as price
                from (
                	select * from ww_matched
                	union all
                	select * from ps_matched
                	union all
                	select * from nw_matched
                ) prods join returned_products as r on r.barcode = prods.barcode
            """, nativeQuery = true)
    Optional<ProductsSearchProjection> findByBarcodeAndStoreIdAndFranchiseName(
            @Param("franchise_name") String franchiseName,
            @Param("store_id") String storeId,
            @Param("barcode") String barcode
    );


    @Query(value = """
            with returned_products as (
            	select barcode, product_name, img, (
            		(similarity(:query, product_name)*0.5) +
            		(ts_rank(
            			setweight(to_tsvector(product_name), 'A'),
            			websearch_to_tsquery(:query),
                		34 --32 squashes 0..1 via x/(x+1), 2  based on length so [super, cool, fresh, banana, bread] would be penalized by dividing the raw rank by length (5)
            		)*0.3)) as score
            	from barcode_name bn
            	where to_tsvector(bn.product_name) @@ websearch_to_tsquery(:query)
            	or :query <% bn.product_name
                limit :limit*4 -- like a buffer since there is a limit later, this way if there is a 1000 rows 
             ),
             fs_skus as (
            	select r.barcode, bfs.internal_sku from barcode_foodstuff_sku as bfs
            	join returned_products as r on bfs.barcode = r.barcode
             ),
             nw_matched as (
            	select f.barcode,
            	'newworld' as franchise_name,
            	nw_map_table.store_name as store_name,
            	n.store_id, n.stock_level, n.price
            	from newworld as n
            	join fs_skus as f on f.internal_sku = n.internal_sku
            	join nw_store_id_store_name_mapping as nw_map_table on n.store_id = nw_map_table.store_id
            	where n.store_id in (:store_ids)
             ),
             ps_matched as (
            	select f.barcode,
            	'paknsave' as franchise_name,
            	ps_map_table.store_name as store_name,
            	p.store_id, p.stock_level, p.price
            	from paknsave as p
            	join fs_skus as f on f.internal_sku = p.internal_sku
            	join ps_store_id_store_name_mapping as ps_map_table on p.store_id = ps_map_table.store_id
            	where p.store_id in (:store_ids)
             ),
             ww_matched as (
            	select r.barcode,
            	'woolworths' as franchise_name,
            	ww_map_table.store_name as store_name,
            	w.store_id, w.stock_level, w.price
            	from woolworths as w
            	join returned_products as r on r.barcode = w.barcode
            	join ww_store_id_store_name_mapping as ww_map_table on w.store_id = ww_map_table.store_id
            	where w.store_id in (:store_ids)
             ),
             all_prods as (
            	select * from ww_matched
            	union all
            	select * from ps_matched
            	union all
            	select * from nw_matched
             ),
             top_prods as (
            	select r.barcode,
            	r.product_name,
            	r.img,
            	r.score
            	from returned_products r
            	where r.barcode in (select p.barcode from all_prods p)
            	order by r.score desc
            	limit :limit
             )
            
             select
            	tp.barcode as barcode,
            	tp.product_name as productName,
            	tp.img as imageUrl,
            	prods.franchise_name as franchiseName,
            	prods.store_name as storeName,
            	prods.store_id as storeId,
            	prods.stock_level as inStockStatus,
            	prods.price as price
             from top_prods tp join all_prods as prods on tp.barcode = prods.barcode
             order by tp.score desc, length(tp.product_name), tp.barcode, prods.price
    """, nativeQuery = true)
    List<ProductsSearchProjection> searchByNameAndListOfStoreIds(@Param("query") String query, @Param("limit") int limit, @Param("store_ids") List<String> store_ids);

}

