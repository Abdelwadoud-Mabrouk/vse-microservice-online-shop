package com.vseonlineshop.shopservice.dto.warehouse;


import java.util.UUID;

public record ReservationCommitedResponse(
        UUID reservationId,
        UUID orderId,
        UUID correlationId,
        String status

) {
}