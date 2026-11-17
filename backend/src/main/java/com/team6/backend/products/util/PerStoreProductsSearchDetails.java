package com.team6.backend.products.util;


import lombok.AllArgsConstructor;
import lombok.Getter;

/**
    "uniqueDetails": [
        {
            "storeId": "33d8d6fc-861a-45ff-9937-5ccdb55eaede",
            "price": 699,
            "franchiseName": "paknsave",
            "inStockStatus": true,
        },...
 */
@AllArgsConstructor
@Getter
public class PerStoreProductsSearchDetails {
    private String storeId;
    private Integer price;
    private String franchiseName;
    private String storeName;
    private Boolean inStockStatus;
}

