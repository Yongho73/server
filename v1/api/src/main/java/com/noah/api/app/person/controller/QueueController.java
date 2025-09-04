package com.noah.api.app.person.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.noah.api.app.person.entity.queue.QueueJoinRequest;
import com.noah.api.app.person.entity.queue.QueueJoinResponse;
import com.noah.api.app.person.entity.queue.QueueStatusResponse;
import com.noah.api.app.person.service.QueueService;

@RestController
@RequestMapping("/api/queue")
public class QueueController {
	
	private final QueueService queueService;

    public QueueController(QueueService queueService) {
        this.queueService = queueService;
    }

    @PostMapping("/join")
    public QueueJoinResponse join(@RequestBody QueueJoinRequest req) {
        return queueService.joinQueue(req.getEventId());
    }

    @GetMapping("/status/{eventId}/{queueId}")
    public QueueStatusResponse status(
    	@PathVariable("eventId") String eventId, 
    	@PathVariable("queueId") String queueId   	
    ) {
        return queueService.checkStatus(eventId, queueId);
    }

    @PostMapping("/allow/{eventId}")
    public String allow(@PathVariable("eventId") String eventId) {
        return queueService.allowNext(eventId); // 관리자/스케줄러가 호출해서 맨앞 사람 입장 처리
    }
    
    @PostMapping("/heartbeat/{queueId}")
    public Map<String, Object> heartbeat(@PathVariable("queueId") String queueId) {
    	return Map.of("ok", queueService.heartbeat(queueId));
    }
}
