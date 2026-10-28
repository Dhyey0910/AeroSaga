package com.aerosaga;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory GPS history. Keeps the last MAX_POINTS pings per drone. */
@Component
public class TelemetryStore {
    private static final int MAX_POINTS = 200;

    private final Map<String, LinkedList<TelemetryPoint>> history = new ConcurrentHashMap<>();

    public void record(TelemetryPoint point) {
        LinkedList<TelemetryPoint> list = history.computeIfAbsent(point.droneId, k -> new LinkedList<>());
        synchronized (list) {
            list.addLast(point);
            while (list.size() > MAX_POINTS) {
                list.removeFirst();
            }
        }
    }

    public List<TelemetryPoint> history(String droneId, int limit) {
        LinkedList<TelemetryPoint> list = history.get(droneId);
        if (list == null) {
            return Collections.emptyList();
        }
        synchronized (list) {
            int from = Math.max(0, list.size() - Math.max(1, limit));
            return new ArrayList<>(list.subList(from, list.size()));
        }
    }

    public TelemetryPoint latest(String droneId) {
        LinkedList<TelemetryPoint> list = history.get(droneId);
        if (list == null) {
            return null;
        }
        synchronized (list) {
            return list.isEmpty() ? null : list.getLast();
        }
    }

    /** Latest ping of every drone, sorted by drone id. */
    public Map<String, TelemetryPoint> latestAll() {
        Map<String, TelemetryPoint> out = new TreeMap<>();
        for (String id : history.keySet()) {
            TelemetryPoint p = latest(id);
            if (p != null) {
                out.put(id, p);
            }
        }
        return out;
    }

    public int count(String droneId) {
        LinkedList<TelemetryPoint> list = history.get(droneId);
        if (list == null) {
            return 0;
        }
        synchronized (list) {
            return list.size();
        }
    }

    public void clear() {
        history.clear();
    }
}