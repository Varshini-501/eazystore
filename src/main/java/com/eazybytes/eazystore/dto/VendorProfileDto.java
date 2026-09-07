package com.eazybytes.eazystore.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VendorProfileDto {

    private Long vendorId;
    private String storeName;
    private String storeDescription;
    private String logoUrl;
    private long followerCount;
    private long postCount;
    private boolean followingCurrentUser;
}
