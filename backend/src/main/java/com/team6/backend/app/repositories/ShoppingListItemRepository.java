package com.team6.backend.app.repositories;

import com.team6.backend.app.entities.ShoppingListItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface ShoppingListItemRepository extends JpaRepository<ShoppingListItem, Long> {
    Optional<ShoppingListItem> findByShoppingListIdAndProductBarcode(Long listId, String productReference);
    Optional<ShoppingListItem> findByIdAndShoppingListId(Long itemId, Long shoppingListId);
}
