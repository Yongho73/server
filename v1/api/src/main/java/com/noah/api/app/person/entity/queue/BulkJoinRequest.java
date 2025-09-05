package com.noah.api.app.person.entity.queue;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BulkJoinRequest {
	private String eventId;
	private int count;
}