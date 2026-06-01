package com.vseonlineshop.shopservice.event.dtos.order;

public record CustomerPayload(
        String name,
        String email,
        String street,
        String postalCode,
        String city,
        String country
) {}