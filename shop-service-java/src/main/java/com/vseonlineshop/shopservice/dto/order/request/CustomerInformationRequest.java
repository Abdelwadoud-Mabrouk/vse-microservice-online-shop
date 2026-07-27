package com.vseonlineshop.shopservice.dto.order.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CustomerInformationRequest(

        @NotBlank(message = "Vorname ist erforderlich.")
        String firstName,

        @NotBlank(message = "Nachname ist erforderlich.")
        String lastName,

        @NotBlank(message = "E-Mail ist erforderlich.")
        @Email(message = "Ungültige E-Mail-Adresse.")
        String email,

        String phone

) {}
