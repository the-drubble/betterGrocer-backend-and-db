package com.team6.backend.app.services;


import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.team6.backend.app.entities.AppUser;
import com.team6.backend.app.entities.Product;
import com.team6.backend.app.entities.ShoppingList;
import com.team6.backend.app.entities.ShoppingListItem;
import com.team6.backend.app.entities.Store;
import com.team6.backend.app.entities.UserProfile;
import com.team6.backend.app.repositories.ProductRepository;
import com.team6.backend.app.repositories.ProfileRepository;
import com.team6.backend.app.repositories.ShoppingListItemRepository;
import com.team6.backend.app.repositories.ShoppingListRepository;
import com.team6.backend.app.repositories.StoreRepository;
import com.team6.backend.app.util.ItemToAddReq;
import com.team6.backend.app.util.ProductDetails;
import com.team6.backend.app.util.ShoppingListResponse;
import com.team6.backend.products.services.ProductsSearchService;
import com.team6.backend.products.util.PerStoreProductsSearchDetails;

@Service
public class ShoppingListBuilderService {
    private final ProfileRepository profileRepository;
    private final ShoppingListRepository shoppingListRepository;
    private final ShoppingListItemRepository shoppingListItemRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final ProductsSearchService productsSearchService;


    public ShoppingListBuilderService(
            ProfileRepository profileRepository,
            ShoppingListRepository shoppingListRepository,
            ShoppingListItemRepository shoppingListItemRepository,
            ProductRepository productRepository,
            StoreRepository storeRepository,
            ProductsSearchService productsSearchService
    ) {
        this.profileRepository = profileRepository;
        this.shoppingListRepository = shoppingListRepository;
        this.shoppingListItemRepository = shoppingListItemRepository;
        this.productRepository = productRepository;
        this.storeRepository = storeRepository;
        this.productsSearchService = productsSearchService;
    }



    private Set<Store> getSetOfOwnersSelectionOfStoresGivenAShoppingList(ShoppingList shoppingList) {
        return shoppingList.getProfile().getProfileSavedStores();
    }

    /** profile -> userId */
    private Long userIdOf(Long profileId) {
        return profileRepository.findUserIdByProfileId(profileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
    }

    /** a list this profile may use (own list, or shared on the same account). */
    private ShoppingList getAccessibleList(Long listId, Long profileId) {
        return shoppingListRepository.findAccessibleById(listId, profileId, userIdOf(profileId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "List not found"));
    }

    /** Locks an accessible list (avoid concurrency errors) */
    private ShoppingList lockAccessibleList(Long listId, Long profileId) {
        return shoppingListRepository.findAccessibleByIdForUpdate(listId, profileId, userIdOf(profileId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "List not found"));
    }

    /** Same as above but owner */
    private ShoppingList lockOwnedList(Long listId, Long profileId) {
        ShoppingList shoppingList = lockAccessibleList(listId, profileId);
        if (!shoppingList.getProfile().getId().equals(profileId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the profile that created this list can do that");
        }
        return shoppingList;
    }

    /**
     * <pre>
     * adding an item to a list
     * Only the details of the item to be added are in the ItemToAddReq which is specified at the POST
     * e.g json payload to be POSTED:
     * </pre>
     * <pre>
     *     {@code
     *  {
     *      "displayName": "Banana",
     *      "productReference": "945717643932",
     *      "quantity": 999
     *  }
     *     }
     * </pre>
     * <pre>
     *  - As mentioned before this gets parsed into an ItemToAddReq object which then gets passed down to this service
     *  - If the item is already in the list then it gets incremented by x quantity (oldQuantity+xQuantity where xQuantity is defined in the POST req)
     *  - The list row is locked first, so the "is it already on the list?" check and the
     *    insert/update can't interleave with another profile adding the same item
     * </pre>
     * @param profileId who
     * @param listId list to be added to
     * @param itemToAddReq what to add to list
     * @return
     */
    @Transactional(transactionManager = "appTransactionManager")
    public ShoppingListResponse.ShoppingListItemResponse addItem(Long profileId, Long listId, ItemToAddReq itemToAddReq) {
        ShoppingList shoppingList = lockAccessibleList(listId, profileId);

        String displayName = itemToAddReq.displayName();
        String productReference = itemToAddReq.productReference();
        Short quantity = itemToAddReq.quantity();
        if (quantity == null || quantity <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be greater than or equal to 1");

        // can get messy, adds item for all lists so need to avoid conflicts on product itself
        productRepository.insertIfAbsent(productReference, displayName);
        Product product = productRepository.findById(productReference)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not save product"));

        Optional<ShoppingListItem> theItem =
                shoppingListItemRepository.findByShoppingListIdAndProductBarcode(listId, productReference);

        ShoppingListItem shoppingListItem;
        if (theItem.isPresent()) {
            // update the item quantity
            shoppingListItem = theItem.get();
            int newQuantity = shoppingListItem.getQuantity()+quantity;
            Short newQuantityClamped = (short) Math.max(1, Math.min(Short.MAX_VALUE, newQuantity));
            shoppingListItem.setQuantity(newQuantityClamped);
        } else {
            // create a new item and save
            shoppingListItem = new ShoppingListItem();
            shoppingListItem.setShoppingList(shoppingList);
            shoppingListItem.setQuantity(quantity);
            shoppingListItem.setProduct(product);
            shoppingListItem = shoppingListItemRepository.save(shoppingListItem);
        }

        Map<String, ProductDetails> storeIdToProductPriceAndOtherDetails =
                getPricesForAnItemForEachStore(shoppingListItem, getSetOfOwnersSelectionOfStoresGivenAShoppingList(shoppingList));
        return ShoppingListResponse.ShoppingListItemResponse.converter(shoppingListItem, storeIdToProductPriceAndOtherDetails);
    }

    /**
     * <pre>
     * creates a list
     * the list will look like
     * <pre>{@code
     * // shoppinglists =
     * [
     *      {
     *          "id": 6, //shopping list id
     *          "userId": 2,
     *          "shoppingListName": "Banana List",
     *          "shoppingListItemSet": [...],
     *          "shoppingListStoreResponseSet": [...],
     *          "shared": false,
     *          "ownerProfileId": 4,
     *          "ownerProfileName": "Frank",
     *          "ownedByCurrentProfile": true
     *      }, ...
     * ]
     * }</pre>
     * New lists are always private; use setShared to share them.
     * </pre>
     * @param profileId
     * @param listName
     * @return
     */
    @Transactional(transactionManager = "appTransactionManager")
    public ShoppingListResponse createList(Long profileId, String listName) {
        UserProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));

        AppUser user = profile.getUser();

        shoppingListRepository.findByProfileIdAndName(profileId, listName)
            .ifPresent(e -> {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "List name already exists for this user"
                );
            });

        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setUser(user);
        shoppingList.setProfile(profile);
        shoppingList.setName(listName);
        shoppingList.setCreatedAt(OffsetDateTime.now());
        shoppingList.setShared(false);
        return shoppingListResponseBuilder(shoppingListRepository.save(shoppingList), profileId);
     }

    /** owner only */
    @Transactional(transactionManager = "appTransactionManager")
    public ShoppingListResponse renameList(Long listId, String newName, Long profileId) {
        ShoppingList shoppingList = lockOwnedList(listId, profileId);
        shoppingList.setName(newName);
        return shoppingListResponseBuilder(shoppingList, profileId);
    }

    /**
     * owner only
     * shared = true  -> every profile on the account can see and edit the list's items
     * shared = false -> back to private; other profiles lose access straight away
     */
    @Transactional(transactionManager = "appTransactionManager")
    public ShoppingListResponse setShared(Long listId, boolean shared, Long profileId) {
        ShoppingList shoppingList = lockOwnedList(listId, profileId);
        shoppingList.setShared(shared);
        return shoppingListResponseBuilder(shoppingList, profileId);
    }

    /**
     * Gets a shopping list the profile can access (own or shared on the same account)
     * @param profileId
     * @param listId
     * @return
     */
    @Transactional(transactionManager = "appTransactionManager", readOnly = true)
    public ShoppingListResponse getUserShoppingList(Long profileId, Long listId) {
        return shoppingListResponseBuilder(getAccessibleList(listId, profileId), profileId);
    }


    /**
     * CHANGED TO USE PROFILEID INSTEAD OF USERID
     * Gets every list the profile can see: its own lists plus lists other profiles
     * on the same account have shared
     * Authentication is req
     * @param profileId
     * @return list of shopping lists (parsed)
     */
    @Transactional(transactionManager = "appTransactionManager", readOnly = true)
    public List<ShoppingListResponse> getAllUserShoppingListsByUserId(Long profileId) {
        return shoppingListRepository.findAccessible(profileId, userIdOf(profileId)).stream()
                .map(userShoppingList->shoppingListResponseBuilder(userShoppingList, profileId)).toList();
    }

    @Transactional(transactionManager = "appTransactionManager", readOnly = true)
    public ShoppingListResponse getUserShoppingListByListNameAndProfileId(Long userId, String listName) {
        ShoppingList shoppingList = shoppingListRepository.findByProfileIdAndName(userId, listName)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND, "List not found"));
        return shoppingListResponseBuilder(shoppingList, userId);
    }

    /**
     * deletes the whole shopping list given the listId and profileId
     * owner only
     * @param listId
     * @param profileId
     */
    @Transactional(transactionManager = "appTransactionManager")
    public void deleteWholeShoppingList(Long listId, Long profileId) {
        ShoppingList shoppingList = lockOwnedList(listId, profileId);
        shoppingListRepository.delete(shoppingList);
    }

    /**
     * <pre>
     * deletes a single item in the shopping list given the listId, itemId, and profileId
     * itemId is used to specify which item to delete from the list
     * (listId, itemId) so you dont delete same item from multiple lists
     * </pre>
     * @param listId
     * @param itemId
     * @param profileId
     */
    @Transactional(transactionManager = "appTransactionManager")
    public void deleteItemFromShoppingList(Long listId, Long itemId, Long profileId) {
        lockAccessibleList(listId, profileId);
        ShoppingListItem shoppingListItem = shoppingListItemRepository.findByIdAndShoppingListId(itemId, listId)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
        shoppingListItemRepository.delete(shoppingListItem);
    }

    /**
     * THESE ARE REALLY INEFFEIECNT, MOST STUFF HERE DOESNT ACTUALLY NEED TO RETURN WHAT THEY RETURN BUT ILL LEAVE IT FOR NOW SINCE ITS WORKING
     * <pre>
     * changes the quantity of the item
     * does NOT delete, it clamps the min to 1 and max to Short.MAX_VALUE
     * the list is locked first so concurrent +/- from different profiles all count
     * </pre>
     *
     * @param listId
     * @param itemId
     * @param quantityToAddOrRemove
     * @param profileId
     * @return
     */
    @Transactional(transactionManager = "appTransactionManager")
    public ShoppingListResponse.ShoppingListItemResponse changeItemQuantity(Long listId, Long itemId, short quantityToAddOrRemove, Long profileId) {
        ShoppingList shoppingList = lockAccessibleList(listId, profileId);
        ShoppingListItem shoppingListItem = shoppingListItemRepository.findByIdAndShoppingListId(itemId, listId)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));

        int newQuantity = shoppingListItem.getQuantity()+quantityToAddOrRemove;
        Short newQuantityClamped = (short) Math.max(1, Math.min(Short.MAX_VALUE, newQuantity));
        shoppingListItem.setQuantity(newQuantityClamped);
        Map<String, ProductDetails> storeIdToProductPriceAndOtherDetails =
                getPricesForAnItemForEachStore(shoppingListItem, getSetOfOwnersSelectionOfStoresGivenAShoppingList(shoppingList));
        return ShoppingListResponse.ShoppingListItemResponse.converter(shoppingListItem, storeIdToProductPriceAndOtherDetails);
    }

    @Transactional(transactionManager = "appTransactionManager")
    public ShoppingListResponse.ShoppingListItemResponse setItemQuantity(Long listId, Long itemId, short quantityToSet, Long profileId) {
        ShoppingList shoppingList = lockAccessibleList(listId, profileId);
        ShoppingListItem shoppingListItem = shoppingListItemRepository.findByIdAndShoppingListId(itemId, listId)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));

        if (quantityToSet < 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity can not be bellow 0");

        Short newQuantityClamped = (short) Math.max(1, Math.min(Short.MAX_VALUE, quantityToSet));
        shoppingListItem.setQuantity(newQuantityClamped);

        Map<String, ProductDetails> storeIdToProductPriceAndOtherDetails =
                getPricesForAnItemForEachStore(shoppingListItem, getSetOfOwnersSelectionOfStoresGivenAShoppingList(shoppingList));
        return ShoppingListResponse.ShoppingListItemResponse.converter(shoppingListItem, storeIdToProductPriceAndOtherDetails);
    }


    /**
     * Takes in a shoppingList and returns a flattened shopping list in the form of ShoppingListResponse
     * @param shoppingList
     * @param viewerProfileId the profile making the request (decides ownedByCurrentProfile)
     * @return a flattened shopping list in the form of ShoppingListResponse
     */
    private ShoppingListResponse shoppingListResponseBuilder(ShoppingList shoppingList, Long viewerProfileId) {
        Set<Store> userSelectedStores = getSetOfOwnersSelectionOfStoresGivenAShoppingList(shoppingList);

        Set<ShoppingListResponse.ShoppingListStoreResponse> shoppingListUserSelectedStoresResponses = userSelectedStores.stream()
                .map(selectedStore ->ShoppingListResponse.ShoppingListStoreResponse.converter(selectedStore))
                .collect(Collectors.toSet());

        Set<ShoppingListResponse.ShoppingListItemResponse> shoppingListItemResponses = shoppingList.getShoppingListShoppingListItems().stream()
                .map(shoppingListItem ->
                        ShoppingListResponse.ShoppingListItemResponse.converter(shoppingListItem, getPricesForAnItemForEachStore(shoppingListItem, userSelectedStores)))
                .collect(Collectors.toSet());

        UserProfile owner = shoppingList.getProfile();

        return new ShoppingListResponse(
                shoppingList.getId(),
                shoppingList.getUser().getId(),
                shoppingList.getName(),
                shoppingListItemResponses,
                shoppingListUserSelectedStoresResponses,
                shoppingList.isShared(),
                owner.getId(),
                owner.getProfileName(),
                owner.getId().equals(viewerProfileId)
        );

    }

    /**
     * each item needs is to a map, since, multiple stores will carry the same item
     * so what that means is that you need a storeid:price mapping (ProductDetails are just price and stock level)
     * <p>
     * (oneItem, userSelectedStores) goes in, and output is (StoreId, PriceOfThatOneItem)
     * @param shoppingListItem
     * @param stores
     * @return
     */
    private Map<String, ProductDetails> getPricesForAnItemForEachStore(ShoppingListItem shoppingListItem, Set<Store> stores) {
        Map<String, ProductDetails> storeIdToProductPriceAndStockLevel = new HashMap<>();

        for (Store store : stores) {
            Optional<PerStoreProductsSearchDetails> thePrice =
                    productsSearchService.getPriceFromGivenStoreAndProductReference(store, shoppingListItem.getProduct().getBarcode());
            thePrice.ifPresent(details -> storeIdToProductPriceAndStockLevel.put(
                    store.getId(),
                    new ProductDetails(
                            details.getPrice(),
                            details.getInStockStatus()
                    )
            ));
        }

        return storeIdToProductPriceAndStockLevel;
    }
}
