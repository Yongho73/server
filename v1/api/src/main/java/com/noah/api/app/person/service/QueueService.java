package com.noah.api.app.person.service;

import java.time.Duration;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.noah.api.app.person.entity.queue.QueueJoinResponse;
import com.noah.api.app.person.entity.queue.QueueStatusResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class QueueService {

	// 메모리 저장소 (실제는 Redis 사용 권장)
	private final StringRedisTemplate redisTemplate;
	
	public QueueService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

	// 대기열 등록
	public QueueJoinResponse joinQueue(String eventId) {
		
		String queueKey = "queue:" + eventId;
        String queueId = UUID.randomUUID().toString();
        
        // Redis List에 대기열 추가 (FIFO 구조)
        redisTemplate.opsForList().rightPush(queueKey, queueId);
        
        // 현재 대기 순번
        Long position = redisTemplate.opsForList().size(queueKey);
		
        // TTL(heartbeat 기반 관리용 세션)
        redisTemplate.opsForValue().set("session:" + queueId, "waiting", Duration.ofMinutes(60));

        return new QueueJoinResponse(queueId, position != null ? position.intValue() : -1, (position != null ? position.intValue() : -1) * 1);
	}

	// 대기열 상태 조회
    public QueueStatusResponse checkStatus(String eventId, String queueId) {
    	String queueKey = "queue:" + eventId;

        // 현재 순번 조회
        Long position = redisTemplate.opsForList().indexOf(queueKey, queueId);

        if (position == null || position == -1) {
            // 리스트에 없으면 "허용" 상태일 수 있음 → allowed 키 확인
            Boolean allowed = redisTemplate.hasKey("allowed:" + queueId);
            if (Boolean.TRUE.equals(allowed)) {
                return new QueueStatusResponse(0, 0, true); // 정상적으로 허용된 사용자
            } else {
                // 리스트에도 없고 허용 토큰도 없음 → 잘못된 queueId or 만료된 사용자
                return new QueueStatusResponse(-1, -1, false);
            }
        }

        // 순번이 있는 경우 → 대기 상태
        int positionValue = position.intValue() + 1;
        int estimateTime = positionValue * 1; // 단순히 "순번 * 1분"으로 계산

        return new QueueStatusResponse(positionValue, estimateTime, position == 0);
    }

    // 대기열 입장 허용 (스케줄러나 관리자에 의해 실행)
    public String allowNext(String eventId) {
        String queueKey = "queue:" + eventId;
        String queueId = redisTemplate.opsForList().leftPop(queueKey); // 맨 앞 사람 제거

        if (queueId != null) {
            redisTemplate.opsForValue().set("allowed:" + queueId, "true", Duration.ofMinutes(5));
        }
        return queueId;
    }
}
