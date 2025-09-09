package com.noah.api.app.queue.entity;

import lombok.Data;

@Data
public class QueueStatusResponse {
	private int position;
    private int estimateTime;
    private boolean allowed;
    private String token;
    
    // 기존 생성자(호환성 위해 유지)
    public QueueStatusResponse(int position, int estimateTime, boolean allowed) {
        this(position, estimateTime, allowed, null);
    }
    
    public QueueStatusResponse(int position, int estimateTime, boolean allowed, String token) {
        this.position = position;
        this.estimateTime = estimateTime;
        this.allowed = allowed;
        this.token = token;
    }
}