package com.olva.academic.kata;

public record RetryDecision(
        String status,
        int attemptNumber,
        String reason
) {

    public static RetryDecision sent(int attemptNumber) {
        return new RetryDecision("SENT", attemptNumber, null);
    }

    public static RetryDecision retry(int attemptNumber, String reason) {
        return new RetryDecision("RETRY", attemptNumber, reason);
    }

    public static RetryDecision failed(int attemptNumber, String reason) {
        return new RetryDecision("FAILED", attemptNumber, reason);
    }
}
