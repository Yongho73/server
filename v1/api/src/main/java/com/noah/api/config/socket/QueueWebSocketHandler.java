package com.noah.api.config.socket;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class QueueWebSocketHandler extends TextWebSocketHandler {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final RedisTemplate<String, String> redisTemplate;

    public QueueWebSocketHandler(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String query = session.getUri().getQuery();
        String queueId = Arrays.stream(query.split("&"))
                .filter(p -> p.startsWith("queueId="))
                .map(p -> p.split("=")[1])
                .findFirst().orElse(null);

        if (queueId != null) {
            sessions.put(queueId, session);
            log.info("🔌 연결 established queueId={}", queueId);
        }
    }

    public void notifyAllowed(String queueId) throws IOException {
        WebSocketSession session = sessions.get(queueId);
        if (session != null && session.isOpen()) {
            session.sendMessage(new TextMessage("{\"allowed\":true}"));
            log.info("✅ 입장 허용 알림 전송 queueId={}", queueId);
        }
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        JsonNode json = new ObjectMapper().readTree(payload);

        if ("REJOIN".equals(json.get("type").asText())) {
            String eventId = json.get("eventId").asText();
            String queueId = json.get("queueId").asText();

            log.info("♻️ 재연결 요청: eventId={}, queueId={}", eventId, queueId);

            // 세션 등록
            sessions.put(queueId, session);

            String queueKey = "queue:" + eventId;
            Long position = redisTemplate.opsForList().indexOf(queueKey, queueId);

            if (position == null || position == -1) {
                session.sendMessage(new TextMessage("{\"allowed\":true}"));
                log.info("⚠️ queueId={} 은 이미 빠져서 allowed 처리", queueId);
                return;
            }

            int estimateTime = (int) ((position + 1) * 1);

            Map<String, Object> status = new HashMap<>();
            status.put("allowed", position == 0);
            status.put("position", position.intValue() + 1);
            status.put("estimateTime", estimateTime);

            String jsonResponse = new ObjectMapper().writeValueAsString(status);
            session.sendMessage(new TextMessage(jsonResponse));

            log.info("📩 REJOIN 복구 완료: {}", jsonResponse);
        }
    }
}
