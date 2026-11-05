package com.aerosaga;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BatteryPolicyTest {
    private final BatteryPolicy policy = new BatteryPolicy(new AeroProperties());

    @Test
    void everyFourthDroneStartsLow() {
        assertEquals(25, policy.initialBattery("drone-4"));
        assertEquals(25, policy.initialBattery("drone-8"));
        assertEquals(100, policy.initialBattery("drone-1"));
        assertEquals(100, policy.initialBattery("drone-7"));
    }

    @Test
    void lowBatteryThresholdIsExclusive() {
        assertTrue(policy.isLow(19.9));
        assertFalse(policy.isLow(20));
        assertFalse(policy.isLow(80));
    }

    @Test
    void drainNeverGoesBelowZero() {
        assertEquals(99.7, policy.drain(100), 0.0001);
        assertEquals(0, policy.drain(0.1), 0.0001);
    }

    @Test
    void unknownDroneNameGetsFullBattery() {
        assertEquals(100, policy.initialBattery("alpha"));
    }
}
