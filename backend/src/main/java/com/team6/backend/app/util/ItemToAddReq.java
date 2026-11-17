package com.team6.backend.app.util;

public record ItemToAddReq(
        String displayName,
        String productReference,
        Short quantity
){}
