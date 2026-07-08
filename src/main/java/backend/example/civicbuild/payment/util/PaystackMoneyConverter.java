package backend.example.civicbuild.payment.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class PaystackMoneyConverter {

    private static final int MONEY_SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private PaystackMoneyConverter() {
    }

    /** Converts GHS display amount to Paystack pesewas (smallest unit). */
    public static long toPesewas(BigDecimal amountGhs) {
        return amountGhs.setScale(MONEY_SCALE, ROUNDING).multiply(HUNDRED).longValueExact();
    }

    /** Converts Paystack pesewas to GHS BigDecimal. */
    public static BigDecimal fromPesewas(long pesewas) {
        return BigDecimal.valueOf(pesewas).divide(HUNDRED, MONEY_SCALE, ROUNDING);
    }
}
