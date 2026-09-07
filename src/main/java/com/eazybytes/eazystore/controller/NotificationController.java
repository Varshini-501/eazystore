package com.eazybytes.eazystore.controller;

import com.eazybytes.eazystore.dto.NotificationDto;
import com.eazybytes.eazystore.service.ISocialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final ISocialService iSocialService;

    @GetMapping
    public ResponseEntity<List<NotificationDto>> getMyNotifications() {
        return ResponseEntity.ok(iSocialService.getMyNotifications());
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long notificationId) {
        iSocialService.markNotificationRead(notificationId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead() {
        iSocialService.markAllNotificationsRead();
        return ResponseEntity.ok().build();
    }

}
