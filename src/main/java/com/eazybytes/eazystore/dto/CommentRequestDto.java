package com.eazybytes.eazystore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentRequestDto {

    @NotBlank(message = "Comment content is required")
    @Size(max = 500, message = "Comment can be at most 500 characters")
    private String content;
}
