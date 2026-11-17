package com.team6.backend.app.controllers;


import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;

import com.team6.backend.app.services.ShoppingListBuilderService;
import com.team6.backend.app.util.ItemToAddReq;
import com.team6.backend.app.util.ShoppingListResponse;
import com.team6.backend.app.util.StoreToAddReq;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;


/*
TODO
CRUD operations
create
- [x] creating a new list
read
- [x] retriving all lists
- [x] retriving one list based off id
- [x] retriving one list based off name update
- [x] adding to a list
- [x] update an item on a list (quantity if that is all?)
    - [x] add more of same item
    - [x] delete
- [x] rename list
- [x] share / unshare a list with the other profiles on the account
delete
- [x] delete an item from a list
- [ ] clear a list?
- [x] delete a list

SHARED LISTS
Lists returned/accepted here are any list the selected profile can access:
its own lists, plus lists other profiles on the same account have shared.
Rename, delete and share/unshare are owner only (403 otherwise).
 */
@RestController
@RequestMapping("/api/list")
public class ShoppingListController {
    private final ShoppingListBuilderService shoppingListService;

    public ShoppingListController(ShoppingListBuilderService shoppingListService) {
        this.shoppingListService = shoppingListService;
    }


    /**
     * Retrieving a list via list id
     * works for the profile's own lists and for lists shared on the same account
     * @param list_id
     * @return
     */
    @GetMapping("/id/{list_id}")
    public ShoppingListResponse getUserShoppingList(
            @PathVariable Long list_id,
            @Parameter(hidden = true) @SessionAttribute("profileId") Long profileId) {
        return shoppingListService.getUserShoppingList(profileId, list_id);
    }

    /**
     * Retrieving a list via list name
     * @param list_name
     * @return
     */
    @Operation(summary = "get a single user shopping list by list name, not list id")
    @GetMapping("/name/{list_name}")
    public ShoppingListResponse getUserShoppingList(
            @PathVariable String list_name,
            @Parameter(hidden = true) @SessionAttribute("profileId") Long profileId) {
        return shoppingListService.getUserShoppingListByListNameAndProfileId(profileId, list_name);
    }

    /**
     * GET REQ
     * retrives all of the lists the selected profile can see:
     * its own lists + lists shared by other profiles on the same account
     * reqs authentication
     *
     * @param profileId
     * @return
     */
    @Operation(summary = "all lists the selected profile can see (own lists + lists shared on the same account)")
    @GetMapping("/")
    public List<ShoppingListResponse> getAllUserShoppingLists(
            @Parameter(hidden = true) @SessionAttribute("profileId") Long profileId) {
        return shoppingListService.getAllUserShoppingListsByUserId(profileId);
    }


    /**
     * POST REQ
     *
     * creates a list (always private to begin with)
     * list's name is determined by the user (@RequestParam)
     * user must be logged in
     *
     * @param list_name
     * @param profileId
     * @return
     */
    @PostMapping("/{list_name}")
    public ResponseEntity<ShoppingListResponse> createList(
            @PathVariable String list_name,
            @Parameter(hidden = true) @SessionAttribute("profileId") Long profileId) {
        return ResponseEntity.ok(shoppingListService.createList(profileId, list_name));
    }

    @Operation(summary = "delete a list (owner only)")
    @DeleteMapping("/{list_id}")
    public ResponseEntity<Void> deleteList(
            @PathVariable Long list_id,
            @Parameter(hidden = true) @SessionAttribute("profileId") Long profileId) {
        shoppingListService.deleteWholeShoppingList(list_id, profileId);
        return ResponseEntity.noContent().build();
    }


    /**
     * POST REQ
     * adding item to a user list
     * <p>
     * reqs authentication
     * <p>
     * takes in a JSON and maps it to ItemToAddReq
     * e.g json payload:
     * <p>
     * <pre>{@code
     *  {
     *      "displayName": "Banana",
     *      "productReference": "945717643932",
     *      "quantity": 999
     *  }
     * }</pre>
     * <p>
     * the @RequestBody accepts a Json payload that is sent via a POST req
     * (The @RequestBody does the parsing from json to ItemToAddReq for u)
     *
     * @param list_id the list id
     * @param itemToAddReq the json payload
     * @param profileId the selected profile hence the authentication needed for list building
     * @return
     */
    @Operation(summary = "add an item to a shopping list given the list id")
    @PostMapping("/{list_id}/items")
    public ResponseEntity<ShoppingListResponse.ShoppingListItemResponse> addItem(
            @PathVariable Long list_id,
            @RequestBody ItemToAddReq itemToAddReq,
            @Parameter(hidden = true) @SessionAttribute("profileId") Long profileId) {
        return ResponseEntity.ok(shoppingListService.addItem(profileId, list_id, itemToAddReq));
    }

    @Operation(summary = "delete item from shopping list")
    @DeleteMapping("/{list_id}/items/{item_id}")
    public ResponseEntity<Void> deleteItemFromShoppingList(
            @PathVariable Long list_id,
            @PathVariable Long item_id,
            @Parameter(hidden = true) @SessionAttribute("profileId") Long profileId) {
        shoppingListService.deleteItemFromShoppingList(list_id, item_id, profileId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "rename a list (owner only)")
    @PatchMapping("/{list_id}")
    public ResponseEntity<ShoppingListResponse> renameList(
            @PathVariable Long list_id,
            @RequestParam String newName,
            @Parameter(hidden = true) @SessionAttribute("profileId") Long profileId) {
        return ResponseEntity.ok(shoppingListService.renameList(list_id, newName, profileId));
    }

    @Operation(summary = "share (true) or unshare (false) a list with every profile on the account (owner only)")
    @PatchMapping("/{list_id}/shared")
    public ResponseEntity<ShoppingListResponse> setListShared(
            @PathVariable Long list_id,
            @RequestParam boolean shared,
            @Parameter(hidden = true) @SessionAttribute("profileId") Long profileId) {
        return ResponseEntity.ok(shoppingListService.setShared(list_id, shared, profileId));
    }

    @PatchMapping("/{list_id}/items/{item_id}/increment")
    public ResponseEntity<ShoppingListResponse.ShoppingListItemResponse> incrementItemQuantity(
            @PathVariable Long list_id,
            @PathVariable Long item_id,
            @Parameter(hidden = true) @SessionAttribute("profileId") Long profileId) {
        return ResponseEntity.ok(shoppingListService.changeItemQuantity(list_id, item_id, (short) 1, profileId));
    }

    @PatchMapping("/{list_id}/items/{item_id}/decrement")
    public ResponseEntity<ShoppingListResponse.ShoppingListItemResponse> decrementItemQuantity(
            @PathVariable Long list_id,
            @PathVariable Long item_id,
            @Parameter(hidden = true) @SessionAttribute("profileId") Long profileId) {
        return ResponseEntity.ok(shoppingListService.changeItemQuantity(list_id, item_id, (short) -1, profileId));
    }

    @PatchMapping("/{list_id}/items/{item_id}/setItemQuantity")
    public ResponseEntity<ShoppingListResponse.ShoppingListItemResponse> setItemQuantity(
            @PathVariable Long list_id,
            @PathVariable Long item_id,
            @RequestParam Short amount,
            @Parameter(hidden = true) @SessionAttribute("profileId") Long profileId) {
        return ResponseEntity.ok(shoppingListService.setItemQuantity(list_id, item_id, amount, profileId));
    }


}
