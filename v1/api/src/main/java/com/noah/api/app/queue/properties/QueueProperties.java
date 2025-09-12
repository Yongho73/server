package com.noah.api.app.queue.properties;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "queue.system")
public class QueueProperties {
	private List<String> eventIds;
	private Map<String, Integer> initialAllowLimit;
}

