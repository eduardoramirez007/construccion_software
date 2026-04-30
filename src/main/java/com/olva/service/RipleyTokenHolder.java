package com.olva.service;

import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class RipleyTokenHolder {

    private String token;
    private Instant expiry;

    public synchronized void update(String token, Instant expiry) {
        this.token = token;
        this.expiry = expiry;
    }

    public synchronized boolean isValid() {
        return token != null
                && expiry != null
                && Instant.now().isBefore(expiry.minusSeconds(60));
    }

    public synchronized String getToken() {
        return token;
    }

    public synchronized void clear() {
        this.token = null;
        this.expiry = null;
    }
}
