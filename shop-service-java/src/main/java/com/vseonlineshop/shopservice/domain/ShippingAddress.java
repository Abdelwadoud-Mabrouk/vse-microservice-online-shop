package com.vseonlineshop.shopservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class ShippingAddress {

    @Column(name = "shipping_street", nullable = false)
    private String street;

    @Column(name = "shipping_house_number", nullable = false)
    private String houseNumber;

    @Column(name = "shipping_postal_code", nullable = false)
    private String postalCode;

    @Column(name = "shipping_city", nullable = false)
    private String city;

    @Column(name = "shipping_country", nullable = false)
    private String country;
}