package com.vseonlineshop.shopservice.event.dtos.order;


import java.util.UUID;

public record WarehouseDispatchFailedPayload(UUID orderId,
                                             UUID correlationId,
                                             String reason) {

}
