package com.vseonlineshop.shopservice.event.dtos.order;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderRollbackCompletedPayload(UUID orderId,
                                            UUID userId,
                                            BigDecimal amount,
                                            String currency,
                                            UUID correlationId,
                                            String transactionId,
                                            String refundId,
                                            String reason) {

}
