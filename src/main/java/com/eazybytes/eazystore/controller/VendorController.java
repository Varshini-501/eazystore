package com.eazybytes.eazystore.controller;

import com.eazybytes.eazystore.dto.BulkUploadResultDto;
import com.eazybytes.eazystore.dto.VendorProfileDto;
import com.eazybytes.eazystore.dto.VendorProfileUpdateDto;
import com.eazybytes.eazystore.dto.VendorRegisterRequestDto;
import com.eazybytes.eazystore.entity.Customer;
import com.eazybytes.eazystore.repository.CustomerRepository;
import com.eazybytes.eazystore.repository.VendorProfileRepository;
import com.eazybytes.eazystore.service.IVendorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/vendor")
@RequiredArgsConstructor
public class VendorController {

    private final IVendorService iVendorService;
    private final CustomerRepository customerRepository;
    private final VendorProfileRepository vendorProfileRepository;

    @PostMapping("/register")
    public ResponseEntity<?> registerVendor(@Valid @RequestBody VendorRegisterRequestDto vendorRegisterRequestDto) {
        Map<String, String> errors = new HashMap<>();

        Optional<Customer> existingCustomer = customerRepository.findByEmailOrMobileNumber(
                vendorRegisterRequestDto.getEmail(), vendorRegisterRequestDto.getMobileNumber());
        existingCustomer.ifPresent(customer -> {
            if (customer.getEmail().equalsIgnoreCase(vendorRegisterRequestDto.getEmail())) {
                errors.put("email", "Email is already registered");
            }
            if (customer.getMobileNumber().equals(vendorRegisterRequestDto.getMobileNumber())) {
                errors.put("mobileNumber", "Mobile number is already registered");
            }
        });
        if (vendorProfileRepository.existsByStoreNameIgnoreCase(vendorRegisterRequestDto.getStoreName())) {
            errors.put("storeName", "Store name is already taken, please choose another one");
        }
        if (!errors.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
        }

        try {
            iVendorService.registerVendor(vendorRegisterRequestDto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Vendor registration successful"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<VendorProfileDto> getMyProfile() {
        return ResponseEntity.ok(iVendorService.getMyProfile());
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateMyProfile(@Valid @RequestBody VendorProfileUpdateDto vendorProfileUpdateDto) {
        try {
            return ResponseEntity.ok(iVendorService.updateMyProfile(vendorProfileUpdateDto));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping(value = "/products/bulk-upload", consumes = "multipart/form-data")
    public ResponseEntity<?> bulkUploadProducts(@RequestParam("file") MultipartFile file) {
        try {
            BulkUploadResultDto result = iVendorService.bulkUploadProducts(file);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Could not read the uploaded file. Please check the format and try again."));
        }
    }

}
