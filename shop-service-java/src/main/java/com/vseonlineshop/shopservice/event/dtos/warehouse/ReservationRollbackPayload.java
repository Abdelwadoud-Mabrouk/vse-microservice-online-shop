package com.vseonlineshop.shopservice.event.dtos.warehouse;

import java.util.UUID;

public record ReservationRollbackPayload(
        UUID orderId,
        UUID reservationId
) {}