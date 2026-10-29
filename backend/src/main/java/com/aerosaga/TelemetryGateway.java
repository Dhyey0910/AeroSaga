package com.aerosaga;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class TelemetryGateway extends TextWebSocketHandler {
    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private final TelemetryStore store;

    public TelemetryGateway(TelemetryStore store) {
        this.store = store;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession s) {
        sessions.add(s);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession s, CloseStatus st) {
        sessions.remove(s);
    }

    public int connectedClients() {
        return sessions.size();
    }

    public void broadcast(String id, double lat, double lon, double alt, double battery) {
        TelemetryPoint point = new TelemetryPoint(id, lat, lon, alt, battery);
        store.record(point);
        String json = toJson(point);
        for (WebSocketSession s : sessions) {
            send(s, json);
        }
    }

    private void send(WebSocketSession s, String json) {
        try {
            synchronized (s) {
                if (s.isOpen()) {
                    s.sendMessage(new TextMessage(json));
                }
            }
        } catch (Exception ignored) {
            sessions.remove(s);
        }
    }

    private String toJson(TelemetryPoint p) {
        return String.format(Locale.US,
                "{\"droneId\":\"%s\",\"lat\":%.6f,\"lon\":%.6f,\"alt\":%.1f,\"battery\":%.1f}",
                p.droneId, p.lat, p.lon, p.alt, p.battery);
    }
}