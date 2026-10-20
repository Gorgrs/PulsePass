package com.pulsepass.service.impl;

import com.pulsepass.domain.TicketType;
import com.pulsepass.service.TicketPriceCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TicketPriceCalculatorTest {

    private final TicketPriceCalculator calculator = new TicketPriceCalculator();

    @Test
    void calculate_general_returnsBasePrice() {
        // ACT
        BigDecimal price = calculator.calculate(TicketType.GENERAL);

        // ASSERT
        assertThat(price).isEqualByComparingTo("100000.00");
    }

    @Test
    void calculate_student_appliesDiscount() {
        BigDecimal price = calculator.calculate(TicketType.STUDENT);

        assertThat(price).isEqualByComparingTo("70000.00");
    }

    @Test
    void calculate_vip_doublesBasePrice() {
        BigDecimal price = calculator.calculate(TicketType.VIP);

        assertThat(price).isEqualByComparingTo("200000.00");
    }

    @Test
    void calculate_backstage_triplesBasePrice() {
        BigDecimal price = calculator.calculate(TicketType.BACKSTAGE);

        assertThat(price).isEqualByComparingTo("300000.00");
    }

    @Test
    void calculate_anyType_neverReturnsNegativePrice() {
        for (TicketType type : TicketType.values()) {
            assertThat(calculator.calculate(type)).isNotNegative();
        }
    }
}