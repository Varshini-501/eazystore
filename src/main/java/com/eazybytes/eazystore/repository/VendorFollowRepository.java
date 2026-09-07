package com.eazybytes.eazystore.repository;

import com.eazybytes.eazystore.entity.VendorFollow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VendorFollowRepository extends JpaRepository<VendorFollow, Long> {

    Optional<VendorFollow> findByVendor_VendorIdAndCustomer_CustomerId(Long vendorId, Long customerId);

    boolean existsByVendor_VendorIdAndCustomer_CustomerId(Long vendorId, Long customerId);

    long countByVendor_VendorId(Long vendorId);

    List<VendorFollow> findByVendor_VendorId(Long vendorId);

    @org.springframework.data.jpa.repository.Query(
            "select f.vendor.vendorId from VendorFollow f where f.customer.customerId = :customerId")
    List<Long> findFollowedVendorIds(@org.springframework.data.repository.query.Param("customerId") Long customerId);
}
