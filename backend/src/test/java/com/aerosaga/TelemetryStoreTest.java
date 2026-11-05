package com.aerosaga;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelemetryStoreTest {
    private final TelemetryStore store = new TelemetryStore();

    @Test
    void unknownDroneHasNoHistory() {
        assertTrue(store.history("nope", 10).isEmpty());
        assertNull(store.latest("nope"));
    }

    @Test
    void latestReturnsNewestPing() {
        store.record(new TelemetryPoint("drone-1", 28.0, 77.0, 10, 99));
        store.record(new TelemetryPoint("drone-1", 28.1, 77.1, 20, 98));

        assertEquals(28.1, store.latest("drone-1").lat, 0.0001);
    }

    @Test
    void historyKeepsOnlyLast200Pings() {
        for (int i = 0; i < 250; i++) {
            store.record(new TelemetryPoint("drone-1", i, 77.0, 10, 99));
        }

        List<TelemetryPoint> all = store.history("drone-1", 1000);
        assertEquals(200, all.size());
        assertEquals(50, all.get(0).lat, 0.0001);   // oldest kept
        assertEquals(249, all.get(199).lat, 0.0001); // newest
    }

    @Test
    void historyLimitReturnsNewestN() {
        for (int i = 0; i < 10; i++) {
            store.record(new TelemetryPoint("drone-2", i, 77.0, 10, 99));
        }

        List<TelemetryPoint> last3 = store.history("drone-2", 3);
        assertEquals(3, last3.size());
        assertEquals(7, last3.get(0).lat, 0.0001);
    }

    @Test
    void clearRemovesEverything() {
        store.record(new TelemetryPoint("drone-3", 1, 1, 1, 1));
        store.clear();
        assertTrue(store.latestAll().isEmpty());
    }
}
