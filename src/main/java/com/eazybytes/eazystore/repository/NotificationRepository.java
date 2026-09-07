package com.eazybytes.eazystore.repository;

import com.eazybytes.eazystore.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipient_CustomerIdOrderByCreatedAtDesc(Long customerId);

    long countByRecipient_CustomerIdAndIsReadFalse(Long customerId);
}
