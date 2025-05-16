package com.noah.api.config.activeMQ;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;

@Configuration
public class JmsConfig {

	@Bean
    public JmsTemplate jmsTemplate() {
        // ActiveMQConnectionFactory를 사용하여 연결 팩토리 설정
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory();
        connectionFactory.setBrokerURL("tcp://192.168.21.10:61616");  // ActiveMQ 브로커 URL 설정

        // ConnectionFactory를 감싸는 CachingConnectionFactory로 성능 최적화
        CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(connectionFactory);

        // JmsTemplate 빈 설정
        JmsTemplate jmsTemplate = new JmsTemplate(cachingConnectionFactory);
        return jmsTemplate;
    }
}