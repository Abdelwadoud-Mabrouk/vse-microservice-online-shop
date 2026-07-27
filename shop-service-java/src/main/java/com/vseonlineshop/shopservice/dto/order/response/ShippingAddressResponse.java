package com.vseonlineshop.shopservice.dto.order.response;

public record ShippingAddressResponse(

        String street,

        String houseNumber,

        String postalCode,

        String city,

        String country

) {}