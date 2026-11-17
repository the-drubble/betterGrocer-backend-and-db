package com.team6.backend.app.util;

public record StoreToAddReq(
        String storeId,
        String name,
        String franchiseName,
        String location)
{ }
