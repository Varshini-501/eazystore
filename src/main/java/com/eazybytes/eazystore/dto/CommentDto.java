package com.eazybytes.eazystore.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class CommentDto {

    private Long commentId;
    private Long postId;
    private String customerName;
    private String content;
    private Instant createdAt;
}
