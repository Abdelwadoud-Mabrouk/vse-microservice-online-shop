package com.vseonlineshop.shopservice.dto.order.request;

import com.vseonlineshop.shopservice.dto.payment.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OrderRequest(
        @NotNull(message = "Kundendaten sind erforderlich.")
        @Valid
        CustomerInformationRequest customer,

        @NotNull(message = "Lieferadresse ist erforderlich.")
        @Valid
        ShippingAddressRequest shippingAddress,

        @NotEmpty(message = "Die Artikelliste darf nicht leer sein.")
        @Valid
        List<ItemRequest> items,

        @NotBlank(message = "Die Währung ist ein Pflichtfeld.")
        String currency,

        @NotNull(message = "Die Zahlungsmethode ist ein Pflichtfeld.")
        PaymentMethod paymentMethod
) {}