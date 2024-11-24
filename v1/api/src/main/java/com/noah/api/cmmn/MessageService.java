package com.noah.api.cmmn;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

@Service
public class MessageService {

    @Autowired
    private JmsTemplate jmsTemplate;

    // ActiveMQ로 메시지를 전송하는 메소드
    public void sendMessage(String message) {
        jmsTemplate.convertAndSend("chatQueue", message);  // "chatQueue"는 큐 이름
    }
}
