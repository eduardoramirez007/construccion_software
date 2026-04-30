package com.olva.academic.kata;

import com.olva.notification.model.NotificationProcessingResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RipleyRetryKataTest {

    private final RipleyRetryKata kata = new RipleyRetryKata();

    @Test
    void shouldRetryWhenRipleyErrorIsTemporaryAndAttemptsRemain() {
        NotificationProcessingResult result = NotificationProcessingResult.retryableError(
                1L, 500, "{}", "{}", "HTTP_500", "Ripley no disponible"
        );

        RetryDecision decision = kata.decide(result, 1, 3);

        assertThat(decision.status()).isEqualTo("RETRY");
        assertThat(decision.attemptNumber()).isEqualTo(2);
        assertThat(decision.reason()).isEqualTo("HTTP_500");
    }

    @Test
    void shouldFailWhenRetryLimitIsReached() {
        NotificationProcessingResult result = NotificationProcessingResult.retryableError(
                1L, 500, "{}", "{}", "HTTP_500", "Ripley no disponible"
        );

        RetryDecision decision = kata.decide(result, 2, 3);

        assertThat(decision.status()).isEqualTo("FAILED");
        assertThat(decision.attemptNumber()).isEqualTo(3);
    }
}
