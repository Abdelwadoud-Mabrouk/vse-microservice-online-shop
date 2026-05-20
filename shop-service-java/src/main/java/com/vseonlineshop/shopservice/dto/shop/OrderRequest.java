package com.vseonlineshop.shopservice.dto.shop;

import com.vseonlineshop.shopservice.domain.PaymentProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record OrderRequest(
        UUID userId,

        @NotEmpty(message = "Die Artikelliste darf nicht leer sein.")
        @Valid
        List<ItemRequest> items,

        @NotBlank(message = "Die Währung (currency) ist ein Pflichtfeld.")
        String currency,

        @NotNull(message = "Der Zahlungsanbieter (paymentProvider) ist ein Pflichtfeld.")
        PaymentProvider paymentProvider
) {}