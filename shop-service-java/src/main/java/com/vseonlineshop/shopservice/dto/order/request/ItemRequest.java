package com.vseonlineshop.shopservice.dto.order.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ItemRequest(
        @NotNull(message = "Die Produkt-ID darf nicht null sein.")
        UUID productId,

        @NotNull(message = "Die Menge darf nicht null sein.")
        @Min(value = 1, message = "Die Bestellmenge muss mindestens 1 betragen.")
        Integer quantity,

        @NotNull(message = "Die Sku darf nicht null sein.")
        String sku
) {
}