package com.noah.api.app.person.entity.queue;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class QueueStatusResponse {
	private int position;
    private int estimateTime;
    private boolean allowed;
}