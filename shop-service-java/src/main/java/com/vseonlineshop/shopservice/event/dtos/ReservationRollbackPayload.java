package com.vseonlineshop.shopservice.event.dtos;

import java.util.UUID;

public record ReservationRollbackPayload(
        UUID orderId,
        UUID reservationId
) {}