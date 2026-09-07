package com.eazybytes.eazystore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VendorProfileUpdateDto {

    @NotBlank(message = "Store name is required")
    @Size(min = 3, max = 150, message = "Store name should be between 3 and 150 characters")
    private String storeName;

    @Size(max = 500, message = "Store description can be at most 500 characters")
    private String storeDescription;

    @Size(max = 500)
    private String logoUrl;
}
