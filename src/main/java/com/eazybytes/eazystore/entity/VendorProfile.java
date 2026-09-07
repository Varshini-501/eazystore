package com.eazybytes.eazystore.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "vendor_profiles")
public class VendorProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vendor_id", nullable = false)
    private Long vendorId;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false, unique = true)
    private Customer customer;

    @Size(max = 150)
    @NotNull
    @Column(name = "store_name", nullable = false, length = 150)
    private String storeName;

    @Size(max = 500)
    @Column(name = "store_description", length = 500)
    private String storeDescription;

    @Size(max = 500)
    @Column(name = "logo_url", length = 500)
    private String logoUrl;

}
