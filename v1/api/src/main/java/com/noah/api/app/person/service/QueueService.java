package com.noah.api.app.person.service;

import com.noah.api.app.person.entity.queue.QueueJoinResponse;
import com.noah.api.app.person.entity.queue.QueueStatusResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class QueueService {

	// 메모리 저장소 (실제는 Redis 사용 권장)
	private final Map<String, List<String>> eventQueue = new ConcurrentHashMap<>();

	// 대기열 등록
	public QueueJoinResponse joinQueue(String eventId) {
		
		log.info("calling joinQueue...eventId="+eventId);
		
		eventQueue.putIfAbsent(eventId, new ArrayList<>());

		String queueId = UUID.randomUUID().toString();
		eventQueue.get(eventId).add(queueId);

		int position = eventQueue.get(eventId).size();
		int estimateTime = position * 1; // 단순히 1분 * 순번 (예시)	

		return new QueueJoinResponse(queueId, position, estimateTime);
	}

	// 대기열 상태 조회
	public QueueStatusResponse checkStatus(String eventId, String queueId) {
		
		log.info("calling checkStatus...queueId="+queueId);
		
		List<String> queue = eventQueue.getOrDefault(eventId, Collections.emptyList());
		int position = queue.indexOf(queueId) + 1;
		boolean allowed = (position == 1); // 맨 앞이면 입장 가능
		int estimateTime = position * 1;

		return new QueueStatusResponse(position, estimateTime, allowed);
	}
}
