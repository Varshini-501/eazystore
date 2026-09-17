package com.eazybytes.eazystore.repository;

import com.eazybytes.eazystore.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByVendor_VendorIdOrderByCreatedAtDesc(Long vendorId);
}
