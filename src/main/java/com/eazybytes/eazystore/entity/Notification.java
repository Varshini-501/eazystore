package com.eazybytes.eazystore.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id", nullable = false)
    private Long notificationId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_customer_id", nullable = false)
    private Customer recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_customer_id")
    private Customer actor;

    @Size(max = 30)
    @NotNull
    @Column(name = "type", nullable = false, length = 30)
    private String type;

    @Size(max = 500)
    @NotNull
    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @ColumnDefault("false")
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

}
