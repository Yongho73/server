package com.noah.api.app.queue.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.noah.api.app.queue.entity.BulkJoinRequest;
import com.noah.api.app.queue.entity.QueueJoinRequest;
import com.noah.api.app.queue.entity.QueueJoinResponse;
import com.noah.api.app.queue.entity.QueueStatusResponse;
import com.noah.api.app.queue.service.QueueService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/queue")
public class QueueController {
	
	private final QueueService queueService;

    @PostMapping("/join")
    public QueueJoinResponse join(@RequestBody QueueJoinRequest req, HttpServletRequest request, HttpServletResponse response) {
        return queueService.joinQueue(req.getEventId(), request, response);
    }

    @GetMapping("/checkStatus/{eventId}/{queueId}")
    public QueueStatusResponse checkStatus(
    	@PathVariable("eventId") String eventId, 
    	@PathVariable("queueId") String queueId   	
    ) {        
    	return queueService.checkStatus(eventId, queueId);
    }

    @PostMapping("/allow/{eventId}")
    public String allow(@PathVariable("eventId") String eventId) {
        return queueService.allowNext(eventId); // 관리자/스케줄러가 호출해서 맨앞 사람 입장 처리
    }
    
    @PostMapping("/heartbeat/{eventId}/{queueId}")
    public Map<String, Object> heartbeat(
    	@PathVariable("eventId") String eventId,
    	@PathVariable("queueId") String queueId
    ) {
    	return Map.of("ok", queueService.heartbeat(eventId, queueId));
    }
    
    @PostMapping("/leave/{eventId}/{queueId}")
    public Map<String, Object> leave(
    	@PathVariable("eventId") String eventId,
    	@PathVariable("queueId") String queueId
    ) {
    	return Map.of("ok", queueService.leaveQueue(eventId, queueId));
    }
    
    @PostMapping("/bulkJoin")
    public Map<String, Object> bulkJoin(@RequestBody BulkJoinRequest req) {
    	return Map.of("ok", queueService.bulkJoinWithPipeline(req));
    }
}
