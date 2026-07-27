package com.vseonlineshop.shopservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class CustomerInformation {

    @Column(name = "customer_first_name", nullable = false)
    private String firstName;

    @Column(name = "customer_last_name", nullable = false)
    private String lastName;

    @Column(name = "customer_email", nullable = false)
    private String email;

    @Column(name = "customer_phone")
    private String phone;
}