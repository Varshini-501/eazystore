package com.eazybytes.eazystore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreatePostDto {

    @NotBlank(message = "Post content is required")
    @Size(max = 1000, message = "Post content can be at most 1000 characters")
    private String content;

    @Size(max = 500)
    private String imageUrl;

    private Long productId;
}
