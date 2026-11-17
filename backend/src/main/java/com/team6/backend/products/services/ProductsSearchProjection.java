package com.team6.backend.products.services;

public interface ProductsSearchProjection {
    String getBarcode();
    String getProductName();
    String getImageUrl();
    String getFranchiseName();
    String getStoreName();
    String getStoreId();
    Boolean getInStockStatus();
    Integer getPrice();
}