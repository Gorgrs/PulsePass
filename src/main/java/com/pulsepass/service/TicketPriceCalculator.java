package com.pulsepass.service;

import com.pulsepass.domain.TicketType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class TicketPriceCalculator {

    private static final BigDecimal BASE_PRICE = new BigDecimal("100000.00");

    public BigDecimal calculate(TicketType type) {
        BigDecimal multiplier = switch (type) {
            case GENERAL -> new BigDecimal("1.00");
            case STUDENT -> new BigDecimal("0.70");
            case VIP -> new BigDecimal("2.00");
            case BACKSTAGE -> new BigDecimal("3.00");
        };
        return BASE_PRICE.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }
}