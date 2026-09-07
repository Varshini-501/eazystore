package com.eazybytes.eazystore.repository;

import com.eazybytes.eazystore.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Post> findByVendor_VendorIdInOrderByCreatedAtDesc(List<Long> vendorIds, Pageable pageable);

    Page<Post> findByVendor_VendorIdOrderByCreatedAtDesc(Long vendorId, Pageable pageable);
}
