package com.team6.backend.app.util;

import com.team6.backend.app.entities.ShoppingList;
import com.team6.backend.app.entities.ShoppingListItem;
import com.team6.backend.app.entities.Store;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * the idea of converters is to simply flatten an object, since some objects may start calling recursively
 *
 * shared / ownerProfileId / ownerProfileName / ownedByCurrentProfile were added for shared lists.
 * ownedByCurrentProfile is relative to the profile that made the request.
 */
public record ShoppingListResponse(
        Long id,
        Long userId,
        String shoppingListName,
        Set<ShoppingListItemResponse> shoppingListItemSet,
        Set<ShoppingListStoreResponse> shoppingListStoreResponseSet,
        boolean shared,
        Long ownerProfileId,
        String ownerProfileName,
        boolean ownedByCurrentProfile
) {
    public record ShoppingListItemResponse(
        Long itemId,
        String displayName,
        String productReference,
        Short quantity,
        Map<String, ProductDetails> storeIdToProductPriceAndOtherDetails
    ) {
        public static ShoppingListItemResponse converter(ShoppingListItem shoppingListItem, Map<String, ProductDetails> storeIdToProductPriceAndOtherDetails) {
            return new ShoppingListItemResponse(
                    shoppingListItem.getId(),
                    shoppingListItem.getProduct().getDisplayName(),
                    shoppingListItem.getProduct().getBarcode(),
                    shoppingListItem.getQuantity(),
                    storeIdToProductPriceAndOtherDetails
            );
        }
    }

    public record ShoppingListStoreResponse(String storeId, String name, String location) {
        public static ShoppingListStoreResponse converter(Store store) {
            return new ShoppingListStoreResponse(store.getId(), store.getName(), store.getLocation());
        }
    }
}
