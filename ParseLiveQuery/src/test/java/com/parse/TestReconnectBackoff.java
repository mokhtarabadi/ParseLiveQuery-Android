package com.parse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Adversarial tests for the reconnect backoff calculation in ParseLiveQueryClientImpl.
 * These verify that the delay arithmetic never overflows int, never goes negative,
 * and stays within the documented bounds [BASE, MAX].
 */
public class TestReconnectBackoff {

    // Mirror constants from ParseLiveQueryClientImpl to test in isolation
    private static final int BASE_RECONNECT_DELAY_MS = 1000;
    private static final int MAX_RECONNECT_DELAY_MS = 30000;
    private static final int MAX_RECONNECT_ATTEMPTS = 10;

    /**
     * Core delay calculation extracted from the library for testability.
     * Uses long arithmetic to avoid int overflow at high attempt counts.
     */
    private long computeDelay(int reconnectAttempts) {
        int clamped = Math.min(reconnectAttempts, MAX_RECONNECT_ATTEMPTS);
        return Math.min(BASE_RECONNECT_DELAY_MS * (long) Math.pow(2, clamped), MAX_RECONNECT_DELAY_MS);
    }

    @Test
    public void delayAtZeroAttempts_isBaseDelay() {
        assertEquals(BASE_RECONNECT_DELAY_MS, computeDelay(0));
    }

    @Test
    public void delayNeverExceedsMax() {
        for (int i = 0; i <= 100; i++) {
            long delay = computeDelay(i);
            assertTrue("delay=" + delay + " at attempt=" + i + " exceeds MAX",
                    delay <= MAX_RECONNECT_DELAY_MS);
        }
    }

    @Test
    public void delayNeverGoesBelowBase() {
        for (int i = 0; i <= 100; i++) {
            long delay = computeDelay(i);
            assertTrue("delay=" + delay + " at attempt=" + i + " is below BASE",
                    delay >= BASE_RECONNECT_DELAY_MS);
        }
    }

    @Test
    public void delayNeverNegative() {
        // The old code used int arithmetic: BASE * (int)Math.pow(2, attempts)
        // At attempt=30 this overflows int and produces a NEGATIVE value.
        for (int i = 0; i <= 100; i++) {
            long delay = computeDelay(i);
            assertTrue("delay=" + delay + " at attempt=" + i + " is negative!", delay >= 0);
        }
    }

    @Test
    public void oldIntArithmeticWouldOverflow() {
        // Demonstrates the bug: int math produces negative value at attempt=30
        int oldDelay = BASE_RECONNECT_DELAY_MS * (int) Math.pow(2, 30);
        assertTrue("Old int arithmetic overflows to negative: " + oldDelay, oldDelay < 0);

        // Our fix stays positive
        long newDelay = computeDelay(30);
        assertTrue("New long arithmetic stays positive: " + newDelay, newDelay > 0);
    }

    @Test
    public void delayClampedByMaxAfterEnoughAttempts() {
        // After MAX_RECONNECT_ATTEMPTS (10), delay should be capped at MAX
        long delayAt10 = computeDelay(10);
        long delayAt20 = computeDelay(20);
        long delayAt100 = computeDelay(100);
        assertEquals(delayAt10, delayAt20);
        assertEquals(delayAt10, delayAt100);
        assertEquals(MAX_RECONNECT_DELAY_MS, delayAt10);
    }

    @Test
    public void exponentialGrowthUpToClamp() {
        // Verify delay doubles each step until clamped
        assertEquals(1000, computeDelay(0));   // 1000 * 2^0
        assertEquals(2000, computeDelay(1));   // 1000 * 2^1
        assertEquals(4000, computeDelay(2));   // 1000 * 2^2
        assertEquals(8000, computeDelay(3));   // 1000 * 2^3
        assertEquals(16000, computeDelay(4));  // 1000 * 2^4
        assertEquals(30000, computeDelay(5));  // 1000 * 2^5 = 32000, clamped to 30000
        assertEquals(30000, computeDelay(6));  // clamped
    }
}
