package com.team6.backend.products.util;


import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
{
    "productName": "None Medium Banana Cake ea",
    "barcode": "208505000000",
    "imageUrl": "https://a.fsimg.co.nz/product/retail/fan/image/500x500/5100216.png"
    "uniqueDetails": [
        {
            "storeId": "33d8d6fc-861a-45ff-9937-5ccdb55eaede",
            "price": 699,
            "franchiseName": "paknsave",
            "inStockStatus": true,
        },...

    ]
}
 */
@AllArgsConstructor
@Getter
public class ProductsSearchResult {
    private String productName;
    private String barcode;
    private String imageUrl;
    private List<PerStoreProductsSearchDetails> uniqueDetails;
}


