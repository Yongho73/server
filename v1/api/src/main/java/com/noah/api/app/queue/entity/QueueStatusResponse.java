package com.noah.api.app.queue.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class QueueStatusResponse {
	private int position;
    private int estimateTime;
    private boolean allowed;
}