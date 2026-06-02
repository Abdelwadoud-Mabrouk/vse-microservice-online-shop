package com.vseonlineshop.shopservice.service.shop;

import java.math.BigDecimal;

class PricingPolicy {

    public static BigDecimal taxRate() {
        return new BigDecimal("0.19");
    }
}