package com.vseonlineshop.shopservice.event;

import java.util.UUID;

public record ReservationRollbackPayload(
        UUID orderId,
        UUID reservationId
) {}