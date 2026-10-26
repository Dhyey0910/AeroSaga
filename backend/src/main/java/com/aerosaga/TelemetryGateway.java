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

    @Override public void afterConnectionEstablished(WebSocketSession s) { sessions.add(s); }
    @Override public void afterConnectionClosed(WebSocketSession s, CloseStatus st) { sessions.remove(s); }

    public void broadcast(String id, double lat, double lon, double alt, double battery) {
        String json = String.format(Locale.US,
                "{\"droneId\":\"%s\",\"lat\":%.6f,\"lon\":%.6f,\"alt\":%.1f,\"battery\":%.1f}", id, lat, lon, alt, battery);
        for (WebSocketSession s : sessions) {
            try { synchronized (s) { if (s.isOpen()) s.sendMessage(new TextMessage(json)); } }
            catch (Exception ignored) { sessions.remove(s); }
        }
    }
}
