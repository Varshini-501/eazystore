package com.eazybytes.eazystore.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class PostDto {

    private Long postId;
    private Long vendorId;
    private String storeName;
    private String vendorLogoUrl;
    private Long productId;
    private String productName;
    private String productImageUrl;
    private String content;
    private String imageUrl;
    private Instant createdAt;
    private long likeCount;
    private long commentCount;
    private boolean likedByCurrentUser;
}
