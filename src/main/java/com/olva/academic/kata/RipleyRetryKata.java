package com.olva.academic.kata;

import com.olva.notification.model.NotificationProcessingResult;

public class RipleyRetryKata {

    public RetryDecision decide(NotificationProcessingResult result, int currentAttempt, int maxAttempts) {
        if (result.successful()) {
            return RetryDecision.sent(currentAttempt);
        }

        int nextAttempt = currentAttempt + 1;
        if (result.retryable() && nextAttempt < maxAttempts) {
            return RetryDecision.retry(nextAttempt, result.errorCode());
        }

        return RetryDecision.failed(nextAttempt, result.errorCode());
    }
}
