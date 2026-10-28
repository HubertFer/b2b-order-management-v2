package com.humberto.b2b.ordermanagement.shared.api;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyFormat {

    private static final int SCALE = 2;

    private MoneyFormat() {
    }

    public static BigDecimal of(BigDecimal amount) {
        return amount.setScale(SCALE, RoundingMode.UNNECESSARY);
    }
}
