package com.noah.api.app.queue.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class QueueJoinResponse {
	private String queueId;
	private int position;
	private int estimateTime;
}