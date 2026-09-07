package com.eazybytes.eazystore.repository;

import com.eazybytes.eazystore.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    Optional<PostLike> findByPost_PostIdAndCustomer_CustomerId(Long postId, Long customerId);

    long countByPost_PostId(Long postId);

    boolean existsByPost_PostIdAndCustomer_CustomerId(Long postId, Long customerId);
}
