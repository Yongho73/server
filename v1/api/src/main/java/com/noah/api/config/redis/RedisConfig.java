package com.noah.api.config.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.stereotype.Component;

import com.noah.api.config.socket.QueueWebSocketHandler;

import lombok.RequiredArgsConstructor;

@Configuration
public class RedisConfig {	
	@Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter listenerAdapter
    ) {		
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listenerAdapter, new PatternTopic("queue:allowed"));
        return container;
    }

    @Bean
    public MessageListenerAdapter listenerAdapter(RedisSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "onMessage");
    }

}

@Component
@RequiredArgsConstructor
class RedisSubscriber {

    private final QueueWebSocketHandler wsHandler;

    public void onMessage(String message, String channel) {
        try {
            // message = queueId
            wsHandler.notifyAllowed(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}