package com.vseonlineshop.payment.strategy.config;

import com.vseonlineshop.payment.dto.enums.PaymentMethod;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@ConfigurationProperties(prefix = "payment")
@Component
@Getter
@Setter
public class PaymentRoutingProperties {

    private Map<PaymentMethod, String> routing = new EnumMap<>(PaymentMethod.class);

}
