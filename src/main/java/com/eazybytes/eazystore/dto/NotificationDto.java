package com.eazybytes.eazystore.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class NotificationDto {

    private Long notificationId;
    private String type;
    private String message;
    private Long postId;
    private String actorName;
    private boolean isRead;
    private Instant createdAt;
}
