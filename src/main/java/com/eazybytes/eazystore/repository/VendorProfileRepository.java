package com.eazybytes.eazystore.repository;

import com.eazybytes.eazystore.entity.VendorProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VendorProfileRepository extends JpaRepository<VendorProfile, Long> {

    Optional<VendorProfile> findByCustomer_CustomerId(Long customerId);

    Optional<VendorProfile> findByCustomer_Email(String email);

    boolean existsByStoreNameIgnoreCase(String storeName);
}
