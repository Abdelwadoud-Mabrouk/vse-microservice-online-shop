package com.vseonlineshop.shopservice.dto.order.request;

import jakarta.validation.constraints.NotBlank;

public record ShippingAddressRequest(

        @NotBlank(message = "Straße ist erforderlich.")
        String street,

        @NotBlank(message = "Hausnummer ist erforderlich.")
        String houseNumber,

        @NotBlank(message = "PLZ ist erforderlich.")
        String postalCode,

        @NotBlank(message = "Stadt ist erforderlich.")
        String city,

        @NotBlank(message = "Land ist erforderlich.")
        String country

) {}
