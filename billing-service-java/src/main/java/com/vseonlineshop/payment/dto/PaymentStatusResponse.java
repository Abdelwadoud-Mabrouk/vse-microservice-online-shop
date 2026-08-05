package com.vseonlineshop.payment.dto;

import com.vseonlineshop.payment.entity.PaymentStatus;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentStatusResponse {
    private String transactionId;
    private PaymentStatus status;
}
