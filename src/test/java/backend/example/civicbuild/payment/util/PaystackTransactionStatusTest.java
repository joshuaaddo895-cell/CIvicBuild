package backend.example.civicbuild.payment.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PaystackTransactionStatusTest {

    @Test
    void recognizesSuccessAndPendingStatuses() {
        assertThat(PaystackTransactionStatus.isSuccess("success")).isTrue();
        assertThat(PaystackTransactionStatus.isPending("pending")).isTrue();
        assertThat(PaystackTransactionStatus.isPending("ongoing")).isTrue();
        assertThat(PaystackTransactionStatus.isTerminalFailure("failed")).isTrue();
        assertThat(PaystackTransactionStatus.isTerminalFailure("success")).isFalse();
    }
}
