package com.noah.api.config.socket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

	private final QueueWebSocketHandler queueWebSocketHandler;

    // ✅ 생성자 주입 (Spring이 @Component QueueWebSocketHandler를 주입)
    public WebSocketConfig(QueueWebSocketHandler queueWebSocketHandler) {
        this.queueWebSocketHandler = queueWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(queueWebSocketHandler, "/ws/queue")
                .setAllowedOrigins("*"); // 필요하면 CORS Origin 제한 가능
    }
}
