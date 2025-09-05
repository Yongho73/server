package com.noah.api.app.queue.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.noah.api.app.queue.service.QueueServiceFlagRedis;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/queue/flag")
@RequiredArgsConstructor
public class QueueFlagController {

    private final QueueServiceFlagRedis flag;

    // -------- 마스터 플래그 --------

    /** 마스터 상태 조회 */
    @GetMapping("/master")
    public Map<String, Object> getMaster() {        
    	return Map.of("masterEnabled", flag.isMasterEnabled());
    }

    /** 마스터 ON/OFF (on|off) */
    @PostMapping("/master/{onoff}")
    public Map<String, Object> setMaster(@PathVariable("onoff") String onoff) {
        boolean enabled = "on".equalsIgnoreCase(onoff);
        flag.setMasterEnabled(enabled);
        return Map.of("masterEnabled", enabled);
    }

    // -------- 이벤트별 플래그 --------

    /** 특정 이벤트 상태 조회 */
    @GetMapping("/{eventId}")
    public Map<String, Object> getEvent(@PathVariable("eventId") String eventId) {
        boolean enabled = flag.isEnabled(eventId);
        return Map.of("eventId", eventId, "enabled", enabled);
    }

    /** 특정 이벤트 ON/OFF (on|off) */
    @PostMapping("/{eventId}/{onoff}")
    public Map<String, Object> setEvent(@PathVariable("eventId") String eventId, @PathVariable("onoff") String onoff) {
        boolean enabled = "on".equalsIgnoreCase(onoff);
        flag.setEnabled(eventId, enabled);
        return Map.of("eventId", eventId, "enabled", enabled);
    }

    /** 특정 이벤트 토글 */
    @PostMapping("/{eventId}/toggle")
    public Map<String, Object> toggleEvent(@PathVariable("eventId") String eventId) {
        boolean curr = flag.isEnabled(eventId);        
        boolean next = !curr;
        flag.setEnabled(eventId, next);
        return Map.of("eventId", eventId, "enabled", next);
    }
}
