package com.eazybytes.eazystore.service;

import com.eazybytes.eazystore.dto.BulkUploadResultDto;
import com.eazybytes.eazystore.dto.VendorProfileDto;
import com.eazybytes.eazystore.dto.VendorProfileUpdateDto;
import com.eazybytes.eazystore.dto.VendorRegisterRequestDto;
import org.springframework.web.multipart.MultipartFile;
import com.eazybytes.eazystore.dto.ProductDto;
import java.util.List;

import java.io.IOException;

public interface IVendorService {

    void registerVendor(VendorRegisterRequestDto vendorRegisterRequestDto);

    VendorProfileDto getMyProfile();

    VendorProfileDto updateMyProfile(VendorProfileUpdateDto vendorProfileUpdateDto);

    BulkUploadResultDto bulkUploadProducts(MultipartFile file) throws IOException;

        List<ProductDto> getMyProducts();

    String generatePromoText(Long productId);
}
